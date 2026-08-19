package com.dororong.rodi.feature.course.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.takeGraphemes
import com.dororong.rodi.core.common.takeGraphemesWithinCodeUnits
import com.dororong.rodi.core.domain.model.course.CourseDraft
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.model.course.CoursePoint
import com.dororong.rodi.core.domain.model.course.CourseRegistrationRequest
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypointType
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.domain.repository.CourseDraftRepository
import com.dororong.rodi.core.domain.repository.CourseLocationRepository
import com.dororong.rodi.core.domain.repository.CourseRegistrationRepository
import com.dororong.rodi.core.domain.repository.CourseRegistrationRouteRepository
import com.dororong.rodi.core.domain.repository.MemberRepository
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MILLIS = 300L
private const val SEARCH_MAX_LENGTH = 50
private const val SEARCH_PAGE_SIZE = 20

@HiltViewModel
class CourseRegistrationViewModel @Inject constructor(
    private val getAuthSession: GetAuthSessionUseCase,
    private val memberRepository: MemberRepository,
    private val draftRepository: CourseDraftRepository,
    private val locationRepository: CourseLocationRepository,
    private val registrationRepository: CourseRegistrationRepository,
    private val routeRepository: CourseRegistrationRouteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CourseRegistrationUiState())
    val state: StateFlow<CourseRegistrationUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<CourseRegistrationEffect>(extraBufferCapacity = 8)
    val effect: Flow<CourseRegistrationEffect> = _effect.asSharedFlow()

    private var searchJob: Job? = null
    private var searchSelectionJob: Job? = null
    private var routeJob: Job? = null
    private var pendingAddressJob: Job? = null
    private var pinEditJob: Job? = null
    private var formJob: Job? = null
    private var draftJob: Job? = null
    private var searchGeneration = 0L
    private var searchSelectionGeneration = 0L
    private var routeGeneration = 0L
    private var pendingAddressGeneration = 0L

    init {
        restoreSessionAndDraft()
        observeRecentSearches()
    }

    fun onIntent(intent: CourseRegistrationIntent) {
        when (intent) {
            CourseRegistrationIntent.Retry -> retry()
            is CourseRegistrationIntent.TutorialPageChanged -> setTutorialPage(intent.page)
            CourseRegistrationIntent.CompleteTutorial -> completeTutorial()
            CourseRegistrationIntent.ContinueDraft -> continueDraft()
            CourseRegistrationIntent.DiscardDraft -> discardDraft()
            CourseRegistrationIntent.Back -> back()
            CourseRegistrationIntent.RequestExit -> requestExit()
            CourseRegistrationIntent.ConfirmExit -> exitAndClearDraft()
            CourseRegistrationIntent.DismissDialog -> _state.update { it.copy(dialog = null) }
            is CourseRegistrationIntent.SelectWaypointRole -> selectWaypointRole(intent.role)
            is CourseRegistrationIntent.SelectWaypoint -> selectWaypoint(intent)
            is CourseRegistrationIntent.RemoveWaypoint -> removeWaypoint(intent.index)
            is CourseRegistrationIntent.BeginPinEdit -> beginPinEdit(intent.index)
            is CourseRegistrationIntent.MoveTemporaryPin -> moveTemporaryPin(intent.point)
            is CourseRegistrationIntent.MapCenterChanged -> mapCenterChanged(intent.point)
            is CourseRegistrationIntent.MapPointSelected -> mapPointSelected(intent.point)
            is CourseRegistrationIntent.CurrentLocationSelected -> currentLocationSelected(intent.point)
            CourseRegistrationIntent.LocationUnavailable -> locationUnavailable()
            CourseRegistrationIntent.CommitPinEdit -> commitPinEdit()
            CourseRegistrationIntent.DiscardPinEdit -> discardPinEdit()
            CourseRegistrationIntent.ResetPinEdit -> resetPinEdit()
            is CourseRegistrationIntent.SearchVisibilityChanged -> setSearchVisibility(intent.visible)
            is CourseRegistrationIntent.SearchKeywordChanged -> searchKeywordChanged(intent.keyword)
            CourseRegistrationIntent.SearchSubmitted -> searchImmediately()
            is CourseRegistrationIntent.SearchSuggestionSelected -> selectSearchSuggestion(intent.id)
            is CourseRegistrationIntent.DeleteRecentSearch -> deleteRecentSearch(intent.id)
            CourseRegistrationIntent.DeleteAllRecentSearches -> deleteAllRecentSearches()
            is CourseRegistrationIntent.MapReady -> mapReady(intent.ready)
            is CourseRegistrationIntent.ToggleCategory -> toggleCategory(intent.code)
            is CourseRegistrationIntent.TogglePracticeType -> togglePracticeType(intent.code)
            is CourseRegistrationIntent.CautionChanged -> updateCaution(intent.value)
            is CourseRegistrationIntent.DescriptionChanged -> updateDescription(intent.value)
            CourseRegistrationIntent.ContinueToForm -> continueToForm()
            CourseRegistrationIntent.Submit -> submit()
            CourseRegistrationIntent.SuccessConfirmed -> {
                _state.update { it.copy(dialog = null) }
                _effect.tryEmit(CourseRegistrationEffect.Completed)
            }
        }
    }

    private fun restoreSessionAndDraft() {
        viewModelScope.launch {
            try {
                val session = getAuthSession()
                if (!session.isLoggedIn) {
                    _state.update { it.copy(isAuthResolved = true, isLoggedIn = false) }
                    _effect.emit(CourseRegistrationEffect.LoginRequired)
                    return@launch
                }
                val draft = draftRepository.observe().first()
                val initialPage = if (session.isCourseTutorialCompleted) {
                    CourseRegistrationPage.Map
                } else {
                    CourseRegistrationPage.Tutorial
                }
                _state.update {
                    it.copy(
                        isAuthResolved = true,
                        isLoggedIn = true,
                        tutorialCompleted = session.isCourseTutorialCompleted,
                        page = initialPage,
                        tutorialLoadState = CourseTutorialLoadState.Ready,
                        draft = draft,
                        isDraftRestored = true,
                        waypoints = normalizeWaypoints(draft?.waypoints.orEmpty()),
                        selectedPracticeTypeCodes = draft?.selectedPracticeTypeCodes.orEmpty(),
                        caution = draft?.caution.orEmpty(),
                        description = draft?.description.orEmpty(),
                        dialog = draft?.takeIf(CourseDraft::isMeaningful)?.let {
                            CourseRegistrationDialog.ResumeDraft
                        },
                    )
                }
                loadRegistrationFormIfNeeded()
                if (initialPage == CourseRegistrationPage.Map) {
                    calculateStrictRouteIfPossible()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.update { it.copy(isAuthResolved = true, tutorialLoadState = CourseTutorialLoadState.Error) }
                _effect.tryEmit(
                    CourseRegistrationEffect.ShowSnackbar(error.message ?: "등록 화면을 불러오지 못했어요."),
                )
            }
        }
    }

    private fun observeRecentSearches() {
        viewModelScope.launch {
            locationRepository.observeRecent()
                .catch { emit(emptyList()) }
                .collect { recent ->
                    _state.update { current ->
                        current.copy(
                            isRecentSearchLoading = false,
                            searchResult = current.searchResult.copy(recent = recent.take(15)),
                        )
                    }
                }
        }
    }

    private fun retry() {
        when {
            _state.value.mapLoadState == CourseMapLoadState.Error -> {
                _state.update { it.copy(mapLoadState = CourseMapLoadState.Loading, mapRetryToken = it.mapRetryToken + 1) }
            }
            _state.value.formLoadState == CourseRegistrationFormLoadState.Error -> loadRegistrationForm()
            _state.value.tutorialLoadState == CourseTutorialLoadState.Error -> {
                _state.update {
                    it.copy(
                        isAuthResolved = false,
                        tutorialLoadState = CourseTutorialLoadState.Loading,
                    )
                }
                restoreSessionAndDraft()
            }
        }
    }

    private fun setTutorialPage(page: Int) {
        _state.update { it.copy(tutorialPage = page.coerceIn(0, 2)) }
    }

    private fun completeTutorial() {
        if (_state.value.tutorialLoadState == CourseTutorialLoadState.Completing) return
        _state.update { it.copy(tutorialLoadState = CourseTutorialLoadState.Completing) }
        viewModelScope.launch {
            try {
                memberRepository.completeCourseTutorial()
                _state.update {
                    it.copy(
                        tutorialCompleted = true,
                        tutorialLoadState = CourseTutorialLoadState.Ready,
                        page = CourseRegistrationPage.Map,
                    )
                }
                loadRegistrationFormIfNeeded()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.update { it.copy(tutorialLoadState = CourseTutorialLoadState.Ready) }
                _effect.emit(CourseRegistrationEffect.ShowSnackbar(error.message ?: "잠시 후 다시 시도해 주세요."))
            }
        }
    }

    private fun back() {
        val current = _state.value
        when (current.page) {
            CourseRegistrationPage.Tutorial -> {
                if (current.tutorialPage > 0) {
                    setTutorialPage(current.tutorialPage - 1)
                } else {
                    _effect.tryEmit(CourseRegistrationEffect.Exit)
                }
            }
            CourseRegistrationPage.Map -> {
                when {
                    current.isSearchVisible -> setSearchVisibility(false)
                    current.editingWaypointIndex != null -> discardPinEdit()
                    else -> requestExit()
                }
            }
            CourseRegistrationPage.Form -> {
                _state.update { it.copy(page = CourseRegistrationPage.Map, dialog = null) }
            }
        }
    }

    private fun requestExit() {
        if (_state.value.isDraftMeaningful) {
            _state.update { it.copy(dialog = CourseRegistrationDialog.Exit) }
        } else {
            _effect.tryEmit(CourseRegistrationEffect.Exit)
        }
    }

    private fun discardDraft() {
        clearPersistedDraft()
        _state.update {
            it.copy(
                draft = null,
                dialog = null,
                waypoints = emptyList(),
                route = null,
                selectedPracticeTypeCodes = emptyList(),
                caution = "",
                description = "",
                page = CourseRegistrationPage.Map,
            )
        }
    }

    private fun continueDraft() {
        _state.update { it.copy(dialog = null, page = CourseRegistrationPage.Map) }
        loadRegistrationFormIfNeeded()
        calculateStrictRouteIfPossible()
    }

    private fun exitAndClearDraft() {
        draftJob?.cancel()
        pinEditJob?.cancel()
        _state.update { it.copy(dialog = null) }
        viewModelScope.launch {
            try {
                draftRepository.clear()
            } finally {
                _effect.emit(CourseRegistrationEffect.Exit)
            }
        }
    }

    private fun selectWaypointRole(role: CourseWaypointRole) {
        if (role == CourseWaypointRole.Via && maxViasReached()) {
            _effect.tryEmit(CourseRegistrationEffect.ShowSnackbar("경유지는 더 추가할 수 없어요."))
            return
        }
        _state.update { it.copy(selectedWaypointRole = role) }
    }

    private fun selectWaypoint(intent: CourseRegistrationIntent.SelectWaypoint) {
        val waypointType = intent.role()
        val current = _state.value.waypoints.toMutableList()
        when (waypointType) {
            RegistrationWaypointType.START -> {
                current.removeAll { it.type == RegistrationWaypointType.START }
                current.add(
                    0,
                    RegistrationWaypoint(
                        type = waypointType,
                        name = intent.name,
                        address = intent.address,
                        jibunAddress = intent.jibunAddress,
                        lat = intent.point.lat,
                        lng = intent.point.lng,
                    ),
                )
            }
            RegistrationWaypointType.DESTINATION -> {
                current.removeAll { it.type == RegistrationWaypointType.DESTINATION }
                current.add(
                    RegistrationWaypoint(
                        type = waypointType,
                        name = intent.name,
                        address = intent.address,
                        jibunAddress = intent.jibunAddress,
                        lat = intent.point.lat,
                        lng = intent.point.lng,
                    ),
                )
            }
            RegistrationWaypointType.VIA -> {
                if (maxViasReached(current)) {
                    _effect.tryEmit(CourseRegistrationEffect.ShowSnackbar("경유지는 더 추가할 수 없어요."))
                    return
                }
                val destinationIndex = current.indexOfFirst { it.type == RegistrationWaypointType.DESTINATION }
                val via = RegistrationWaypoint(
                    type = waypointType,
                    name = intent.name,
                    address = intent.address,
                    jibunAddress = intent.jibunAddress,
                    lat = intent.point.lat,
                    lng = intent.point.lng,
                )
                if (destinationIndex >= 0) current.add(destinationIndex, via) else current.add(via)
            }
        }
        val normalized = normalizeWaypoints(current)
        _state.update {
            it.copy(
                waypoints = normalized,
                route = null,
                mapCenter = intent.point,
                mapCenterGeneration = it.mapCenterGeneration + 1,
                selectedWaypointRole = when (waypointType) {
                    RegistrationWaypointType.START -> CourseWaypointRole.Destination
                    RegistrationWaypointType.DESTINATION -> CourseWaypointRole.Destination
                    RegistrationWaypointType.VIA -> CourseWaypointRole.Destination
                },
            )
        }
        persistDraft()
        calculateStrictRouteIfPossible()
    }

    private fun CourseRegistrationIntent.SelectWaypoint.role(): RegistrationWaypointType = when {
        _state.value.selectedWaypointRole == CourseWaypointRole.Start -> RegistrationWaypointType.START
        _state.value.selectedWaypointRole == CourseWaypointRole.Destination -> RegistrationWaypointType.DESTINATION
        else -> RegistrationWaypointType.VIA
    }

    private fun removeWaypoint(index: Int) {
        val remaining = _state.value.waypoints.toMutableList().also {
            if (index in it.indices && it[index].type == RegistrationWaypointType.VIA) it.removeAt(index)
        }
        _state.update { it.copy(waypoints = normalizeWaypoints(remaining), route = null) }
        persistDraft()
        calculateStrictRouteIfPossible()
    }

    private fun beginPinEdit(index: Int) {
        val waypoint = _state.value.waypoints.getOrNull(index) ?: return
        val point = GeoPoint(waypoint.lat, waypoint.lng)
        _state.update {
            it.copy(
                editingWaypointIndex = index,
                temporaryPin = null,
                mapCenter = point,
                mapCenterGeneration = it.mapCenterGeneration + 1,
            )
        }
        // 프로그램적 카메라 이동(moveCamera)은 지도 SDK의 onCameraMoveEnd 콜백을 쏘지 않아
        // mapCenterChanged가 자동으로 안 불린다. 진입 시점 주소를 직접 미리 로딩해둔다.
        loadPendingAddress(point)
    }

    private fun moveTemporaryPin(point: GeoPoint) {
        val current = _state.value
        val index = current.editingWaypointIndex ?: return
        if (current.isPendingAddressLoading || current.pendingSuggestion == null) return
        _state.update { it.copy(temporaryPin = point, mapCenter = point) }
        previewPinEditRoute(index, point)
    }

    /** 핀 수정 중 후보 위치를 고르면 완료를 누르기 전에도 실제 도로 경로를 미리 그려준다. */
    private fun previewPinEditRoute(index: Int, point: GeoPoint) {
        val current = _state.value
        val candidateWaypoints = current.waypoints.toMutableList()
        val original = candidateWaypoints.getOrNull(index) ?: return
        candidateWaypoints[index] = original.copy(lat = point.lat, lng = point.lng)
        val start = candidateWaypoints.firstOrNull { it.type == RegistrationWaypointType.START } ?: return
        val destination = candidateWaypoints.firstOrNull { it.type == RegistrationWaypointType.DESTINATION } ?: return
        routeJob?.cancel()
        routeGeneration += 1
        val generation = routeGeneration
        _state.update { it.copy(isRouteLoading = true) }
        routeJob = viewModelScope.launch {
            try {
                val result = routeRepository.getStrictRoute(
                    origin = CoursePoint(start.name, start.lat, start.lng),
                    waypoints = candidateWaypoints.filter { it.type == RegistrationWaypointType.VIA }
                        .map { CoursePoint(it.name, it.lat, it.lng) },
                    destination = CoursePoint(destination.name, destination.lat, destination.lng),
                )
                if (generation != routeGeneration) return@launch
                _state.update {
                    it.copy(
                        isRouteLoading = false,
                        route = if (result.isRealRoute && result.totalDistanceMeters > 0) result else it.route,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (generation == routeGeneration) {
                    _state.update { it.copy(isRouteLoading = false) }
                }
            }
        }
    }

    private fun mapCenterChanged(point: GeoPoint) {
        _state.update { it.copy(mapCenter = point) }
        if (!_state.value.isSearchVisible) {
            loadPendingAddress(point)
        }
    }

    private fun currentLocationSelected(point: GeoPoint) {
        _state.update {
            it.copy(
                mapCenter = point,
                mapCenterGeneration = it.mapCenterGeneration + 1,
            )
        }
        if (_state.value.editingWaypointIndex == null) {
            loadPendingAddress(point)
        }
    }

    private fun locationUnavailable() {
        _effect.tryEmit(CourseRegistrationEffect.ShowSnackbar("현재 위치를 확인하지 못했어요."))
    }

    /** 다른 지도 앱처럼 드래그하는 동안 미리 역지오코딩해서 확정 시 대기 없이 바로 다음 마커로 넘어가도록 한다. */
    private fun loadPendingAddress(point: GeoPoint) {
        pendingAddressJob?.cancel()
        pendingAddressGeneration += 1
        val generation = pendingAddressGeneration
        _state.update { it.copy(isPendingAddressLoading = true, pendingSuggestion = null) }
        pendingAddressJob = viewModelScope.launch {
            try {
                val suggestion = locationRepository.reverseGeocode(point)
                if (generation != pendingAddressGeneration) return@launch
                val resolved = suggestion?.takeIf { it.address.isNotBlank() }
                _state.update { it.copy(pendingSuggestion = resolved, isPendingAddressLoading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (generation == pendingAddressGeneration) {
                    _state.update { it.copy(pendingSuggestion = null, isPendingAddressLoading = false) }
                }
            }
        }
    }

    private fun mapPointSelected(point: GeoPoint) {
        if (_state.value.editingWaypointIndex != null) {
            moveTemporaryPin(point)
            return
        }
        val current = _state.value
        if (current.isPendingAddressLoading) return
        val suggestion = current.pendingSuggestion
        val resolvedPoint = suggestion?.point
        if (suggestion == null || resolvedPoint == null || suggestion.address.isBlank()) {
            _effect.tryEmit(CourseRegistrationEffect.ShowSnackbar("주소를 확인할 수 없습니다"))
            return
        }
        setSearchVisibility(false)
        selectWaypoint(
            CourseRegistrationIntent.SelectWaypoint(
                point = resolvedPoint,
                name = suggestion.title,
                address = suggestion.address,
                jibunAddress = null,
            ),
        )
    }

    private fun commitPinEdit() {
        val current = _state.value
        val index = current.editingWaypointIndex ?: return
        val point = current.temporaryPin
        if (point == null) {
            discardPinEdit()
            return
        }
        if (current.isMapPointLoading) return
        // 드래그 중 미리 로딩해둔 주소가 선택 시점 좌표와 일치하면 재조회 없이 바로 반영한다.
        val cached = current.pendingSuggestion?.takeIf { it.point == point }
        if (cached != null) {
            applyPinEdit(index, point, point, cached)
            return
        }
        pinEditJob?.cancel()
        _state.update { it.copy(isMapPointLoading = true) }
        pinEditJob = viewModelScope.launch {
            try {
                val suggestion = locationRepository.reverseGeocode(point)
                val resolvedPoint = suggestion?.point
                if (suggestion == null || resolvedPoint == null || suggestion.address.isBlank()) {
                    _effect.emit(CourseRegistrationEffect.ShowSnackbar("주소를 확인할 수 없습니다"))
                    return@launch
                }
                applyPinEdit(index, point, resolvedPoint, suggestion)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _effect.emit(CourseRegistrationEffect.ShowSnackbar(error.message ?: "위치 정보를 확인하지 못했어요."))
            } finally {
                _state.update { it.copy(isMapPointLoading = false) }
            }
        }
    }

    private fun applyPinEdit(index: Int, point: GeoPoint, resolvedPoint: GeoPoint, suggestion: CourseLocationSuggestion) {
        val latest = _state.value
        if (latest.editingWaypointIndex != index || latest.temporaryPin != point) return
        val refreshed = latest.waypoints.toMutableList()
        val updatedWaypoint = refreshed.getOrNull(index) ?: return
        refreshed[index] = updatedWaypoint.copy(
            name = suggestion.title,
            address = suggestion.address,
            lat = resolvedPoint.lat,
            lng = resolvedPoint.lng,
        )
        _state.update {
            it.copy(
                waypoints = normalizeWaypoints(refreshed),
                editingWaypointIndex = null,
                temporaryPin = null,
                mapCenter = resolvedPoint,
                mapCenterGeneration = it.mapCenterGeneration + 1,
                route = null,
                pendingSuggestion = suggestion,
                isPendingAddressLoading = false,
            )
        }
        persistDraft()
        calculateStrictRouteIfPossible()
    }

    private fun discardPinEdit() {
        val current = _state.value
        val index = current.editingWaypointIndex ?: return
        val original = current.waypoints.getOrNull(index) ?: return
        val point = GeoPoint(original.lat, original.lng)
        _state.update {
            it.copy(
                editingWaypointIndex = null,
                temporaryPin = null,
                mapCenter = point,
                mapCenterGeneration = it.mapCenterGeneration + 1,
            )
        }
        loadPendingAddress(point)
    }

    private fun resetPinEdit() {
        val current = _state.value
        val index = current.editingWaypointIndex ?: return
        val original = current.waypoints.getOrNull(index) ?: return
        val point = GeoPoint(original.lat, original.lng)
        _state.update {
            it.copy(
                temporaryPin = null,
                mapCenter = point,
                mapCenterGeneration = it.mapCenterGeneration + 1,
            )
        }
        loadPendingAddress(point)
    }

    private fun mapReady(ready: Boolean) {
        _state.update {
            it.copy(mapLoadState = if (ready) CourseMapLoadState.Ready else CourseMapLoadState.Error)
        }
    }

    private fun searchKeywordChanged(keyword: String) {
        searchJob?.cancel()
        searchSelectionJob?.cancel()
        searchSelectionGeneration += 1
        searchGeneration += 1
        val limited = keyword.take(SEARCH_MAX_LENGTH)
        val normalized = limited.trim()
        _state.update {
            it.copy(
                searchKeyword = limited,
                isSearchVisible = true,
                isSearchLoading = normalized.isNotBlank(),
                isMapPointLoading = false,
                searchError = null,
                searchResult = if (normalized.isBlank()) {
                    it.searchResult.copy(regions = emptyList(), places = emptyList())
                } else {
                    it.searchResult.copy(regions = emptyList(), places = emptyList())
                },
            )
        }
        if (normalized.isBlank()) return
        val generation = searchGeneration
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            search(normalized, generation)
        }
    }

    private fun setSearchVisibility(visible: Boolean) {
        if (!visible) {
            searchJob?.cancel()
            searchSelectionJob?.cancel()
            searchSelectionGeneration += 1
            searchGeneration += 1
            _state.update {
                it.copy(
                    isSearchVisible = false,
                    isSearchLoading = false,
                    isMapPointLoading = false,
                    searchError = null,
                )
            }
        } else {
            _state.update { it.copy(isSearchVisible = true) }
        }
    }

    private fun searchImmediately() {
        val keyword = _state.value.searchKeyword.trim()
        if (keyword.isBlank()) return
        searchJob?.cancel()
        searchGeneration += 1
        val generation = searchGeneration
        _state.update { it.copy(isSearchLoading = true, searchError = null) }
        searchJob = viewModelScope.launch { search(keyword, generation) }
    }

    private suspend fun search(keyword: String, generation: Long) {
        try {
            val result = locationRepository.search(keyword)
            if (generation != searchGeneration) return
            _state.update {
                it.copy(
                    isSearchLoading = false,
                        searchResult = result.copy(
                            regions = result.regions.take(4),
                            places = result.places.take(SEARCH_PAGE_SIZE),
                        ),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            if (generation != searchGeneration) return
            _state.update {
                it.copy(isSearchLoading = false, searchError = error.message ?: "검색에 실패했어요.")
            }
            _effect.emit(CourseRegistrationEffect.ShowSnackbar("검색에 실패했어요. 다시 시도해 주세요."))
        }
    }

    private fun selectSearchSuggestion(id: String) {
        val result = _state.value.searchResult
        val suggestion = (result.recent + result.regions + result.places).firstOrNull { it.id == id } ?: return
        searchJob?.cancel()
        searchSelectionJob?.cancel()
        searchGeneration += 1
        searchSelectionGeneration += 1
        val generation = searchSelectionGeneration
        _state.update { it.copy(isMapPointLoading = true, searchError = null) }
        searchSelectionJob = viewModelScope.launch {
            try {
                val resolved = locationRepository.resolveSelection(suggestion)
                if (generation != searchSelectionGeneration) return@launch
                val point = resolved?.point
                if (resolved == null || point == null || resolved.address.isBlank()) {
                    _effect.emit(CourseRegistrationEffect.ShowSnackbar("선택한 위치를 확인하지 못했어요. 다시 시도해 주세요."))
                    return@launch
                }
                locationRepository.saveRecent(resolved)
                if (generation != searchSelectionGeneration) return@launch
                _state.update {
                    it.copy(
                        isSearchVisible = false,
                        searchKeyword = resolved.title,
                        searchError = null,
                        mapCenter = point,
                        mapCenterGeneration = it.mapCenterGeneration + 1,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (generation == searchSelectionGeneration) {
                    _effect.emit(CourseRegistrationEffect.ShowSnackbar("선택한 위치를 확인하지 못했어요. 다시 시도해 주세요."))
                }
            } finally {
                if (generation == searchSelectionGeneration) {
                    _state.update { it.copy(isMapPointLoading = false) }
                }
            }
        }
    }

    private fun deleteRecentSearch(id: String) {
        viewModelScope.launch { locationRepository.deleteRecent(id) }
    }

    private fun deleteAllRecentSearches() {
        viewModelScope.launch { locationRepository.clearRecent() }
    }

    /** 카테고리는 복수 선택이고, 해제하면 그 카테고리에서 고른 연습유형도 함께 풀린다. */
    private fun toggleCategory(code: String) {
        val form = _state.value.registrationForm ?: return
        val category = form.categories.firstOrNull { it.code == code } ?: return
        val selected = _state.value.selectedCategoryCodes
        if (code in selected) {
            val ownedTypeCodes = category.practiceTypes.map { it.code }.toSet()
            _state.update {
                it.copy(
                    selectedCategoryCodes = selected - code,
                    selectedPracticeTypeCodes = it.selectedPracticeTypeCodes - ownedTypeCodes,
                )
            }
        } else {
            _state.update { it.copy(selectedCategoryCodes = selected + code) }
        }
        persistDraft()
    }

    private fun togglePracticeType(code: String) {
        val form = _state.value.registrationForm ?: return
        val availableCodes = form.categories.flatMap { it.practiceTypes }.map { it.code }.toSet()
        if (code !in availableCodes) return
        val selected = _state.value.selectedPracticeTypeCodes
        if (code in selected) {
            _state.update { it.copy(selectedPracticeTypeCodes = selected - code) }
        } else if (selected.size >= form.practiceTypeMaxSelect) {
            _effect.tryEmit(CourseRegistrationEffect.ShowSnackbar(form.practiceTypeMaxSelectExceededMessage))
            return
        } else {
            _state.update { it.copy(selectedPracticeTypeCodes = selected + code) }
        }
        persistDraft()
    }

    private fun updateCaution(value: String) {
        val max = _state.value.registrationForm?.cautionInput?.maxLength ?: Int.MAX_VALUE
        _state.update { it.copy(caution = value.limitForServer(max)) }
        persistDraft()
    }

    private fun updateDescription(value: String) {
        val max = _state.value.registrationForm?.descriptionInput?.maxLength ?: Int.MAX_VALUE
        _state.update { it.copy(description = value.limitForServer(max)) }
        persistDraft()
    }

    private fun continueToForm() {
        if (!_state.value.canFinishMap) {
            _effect.tryEmit(CourseRegistrationEffect.ShowSnackbar("실제 주행 경로를 확인한 뒤 계속할 수 있어요."))
            return
        }
        _state.update { it.copy(page = CourseRegistrationPage.Form) }
        loadRegistrationFormIfNeeded()
    }

    private fun loadRegistrationFormIfNeeded() {
        if (_state.value.registrationForm != null || _state.value.formLoadState == CourseRegistrationFormLoadState.Loading) {
            if (_state.value.registrationForm == null) loadRegistrationForm()
            return
        }
        loadRegistrationForm()
    }

    private fun loadRegistrationForm() {
        if (formJob?.isActive == true) return
        _state.update { it.copy(formLoadState = CourseRegistrationFormLoadState.Loading, submissionError = null) }
        formJob = viewModelScope.launch {
            try {
                val form = registrationRepository.getRegistrationForm()
                val availableCodes = form.categories.flatMap { it.practiceTypes }.map { it.code }.toSet()
                val currentCodes = _state.value.selectedPracticeTypeCodes
                val reconciledCodes = currentCodes
                    .filter(availableCodes::contains)
                    .distinct()
                    .take(form.practiceTypeMaxSelect.coerceAtLeast(0))
                val currentWaypoints = _state.value.waypoints
                val reconciledWaypoints = trimWaypointsToFormLimit(currentWaypoints, form.maxWaypoints)
                // 임시저장을 복원하면 연습유형만 남아 있으므로, 그 유형을 가진 카테고리를 다시 펼쳐준다.
                // 그러지 않으면 선택된 연습유형이 화면에 보이지 않는 채로 완료 조건만 충족돼 버린다.
                val restoredCategoryCodes = form.categories
                    .filter { category -> category.practiceTypes.any { it.code in reconciledCodes } }
                    .map { it.code }
                _state.update {
                    it.copy(
                        formLoadState = CourseRegistrationFormLoadState.Ready,
                        registrationForm = form,
                        selectedCategoryCodes = restoredCategoryCodes,
                        selectedPracticeTypeCodes = reconciledCodes,
                        waypoints = reconciledWaypoints,
                        route = if (reconciledWaypoints == currentWaypoints) it.route else null,
                    )
                }
                if (reconciledCodes != currentCodes || reconciledWaypoints != currentWaypoints) persistDraft()
                if (reconciledWaypoints != currentWaypoints) calculateStrictRouteIfPossible()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        formLoadState = CourseRegistrationFormLoadState.Error,
                        submissionError = error.message ?: "등록 양식을 불러오지 못했어요.",
                    )
                }
            }
        }
    }

    private fun submit() {
        val current = _state.value
        if (!current.canSubmit) {
            _state.update { it.copy(hasAttemptedSubmit = true) }
            _effect.tryEmit(CourseRegistrationEffect.ShowSnackbar("필수정보를 입력해주세요."))
            return
        }
        // 10자 조건은 버튼을 막는 대신 눌린 뒤 안내한다 — 등록 요청 없이 화면·입력값을 그대로 유지한다.
        if (!current.descriptionMeetsRegisterLength) {
            _state.update { it.copy(hasAttemptedSubmit = true) }
            _effect.tryEmit(CourseRegistrationEffect.ShowSnackbar("한줄 설명은 10자 이상이어야 해요."))
            return
        }
        val start = current.waypoints.firstOrNull { it.type == RegistrationWaypointType.START } ?: return
        val route = current.route ?: return
        _state.update { it.copy(isSubmitting = true, submissionError = null) }
        viewModelScope.launch {
            try {
                val result = registrationRepository.registerCourse(
                    CourseRegistrationRequest(
                        address = start.address,
                        distanceMeters = route.totalDistanceMeters,
                        waypoints = current.waypoints,
                        practiceTypes = current.selectedPracticeTypeCodes,
                        description = current.description,
                        // 공백만 입력한 주의사항은 미입력으로 처리한다.
                        caution = current.caution.trim(),
                    ),
                )
                draftJob?.cancel()
                draftRepository.clear()
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        registrationResult = result,
                        draft = null,
                        dialog = CourseRegistrationDialog.Success,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.update {
                    it.copy(isSubmitting = false, submissionError = error.message ?: "코스 등록에 실패했어요.")
                }
                _effect.emit(CourseRegistrationEffect.ShowSnackbar("등록에 실패했어요. 입력 내용을 확인하고 다시 시도해 주세요."))
            }
        }
    }

    private fun calculateStrictRouteIfPossible() {
        val current = _state.value
        routeJob?.cancel()
        routeGeneration += 1
        val start = current.waypoints.firstOrNull { it.type == RegistrationWaypointType.START }
        val destination = current.waypoints.firstOrNull { it.type == RegistrationWaypointType.DESTINATION }
        if (start == null || destination == null) {
            _state.update { it.copy(isRouteLoading = false, route = null) }
            return
        }
        val generation = routeGeneration
        // 실패해도 기존 입력값과 지도 위치를 유지해야 하므로(CR-01 예외처리), 계산이 끝나기 전까지는
        // 이전에 성공한 route를 지우지 않는다. 실패 시엔 3초 토스트만 띄운다.
        _state.update { it.copy(isRouteLoading = true) }
        routeJob = viewModelScope.launch {
            try {
                val result = routeRepository.getStrictRoute(
                    origin = CoursePoint(start.name, start.lat, start.lng),
                    waypoints = current.waypoints.filter { it.type == RegistrationWaypointType.VIA }
                        .map { CoursePoint(it.name, it.lat, it.lng) },
                    destination = CoursePoint(destination.name, destination.lat, destination.lng),
                )
                if (generation != routeGeneration) return@launch
                if (!result.isRealRoute || result.totalDistanceMeters <= 0 ||
                    result.snappedPoints.size != current.waypoints.size
                ) {
                    _state.update { it.copy(isRouteLoading = false) }
                    _effect.emit(CourseRegistrationEffect.ShowSnackbar("일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요."))
                } else {
                    val adjustedWaypoints = resolveSnappedWaypoints(current.waypoints, result)
                    if (generation != routeGeneration) return@launch
                    if (adjustedWaypoints == null) {
                        _state.update { it.copy(isRouteLoading = false) }
                        _effect.emit(CourseRegistrationEffect.ShowSnackbar("선택한 위치의 주소를 다시 확인하지 못했어요."))
                        return@launch
                    }
                    _state.update {
                        it.copy(
                            isRouteLoading = false,
                            waypoints = adjustedWaypoints,
                            route = result,
                        )
                    }
                    if (adjustedWaypoints != current.waypoints) persistDraft()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (generation != routeGeneration) return@launch
                _state.update { it.copy(isRouteLoading = false) }
                _effect.emit(CourseRegistrationEffect.ShowSnackbar(error.message ?: "일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요."))
            }
        }
    }

    private suspend fun resolveSnappedWaypoints(
        waypoints: List<RegistrationWaypoint>,
        route: RouteResult,
    ): List<RegistrationWaypoint>? {
        if (route.snappedPoints.size != waypoints.size) return null
        return waypoints.mapIndexed { index, waypoint ->
            val snapped = route.snappedPoints[index]
            val suggestion = locationRepository.reverseGeocode(snapped) ?: return null
            if (suggestion.address.isBlank()) return null
            waypoint.copy(
                name = suggestion.title.ifBlank { waypoint.name },
                address = suggestion.address,
                lat = snapped.lat,
                lng = snapped.lng,
            )
        }
    }

    private fun persistDraft() {
        val current = _state.value
        val draft = CourseDraft(
            waypoints = current.waypoints,
            selectedPracticeTypeCodes = current.selectedPracticeTypeCodes,
            caution = current.caution,
            description = current.description,
        )
        _state.update { it.copy(draft = draft.takeIf(CourseDraft::isMeaningful)) }
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            if (draft.isMeaningful) draftRepository.save(draft) else draftRepository.clear()
        }
    }

    private fun clearPersistedDraft() {
        draftJob?.cancel()
        draftJob = viewModelScope.launch { draftRepository.clear() }
    }

    private fun normalizeWaypoints(waypoints: List<RegistrationWaypoint>): List<RegistrationWaypoint> {
        val start = waypoints.firstOrNull { it.type == RegistrationWaypointType.START }
        val vias = waypoints.filter { it.type == RegistrationWaypointType.VIA }
        val destination = waypoints.firstOrNull { it.type == RegistrationWaypointType.DESTINATION }
        return buildList {
            start?.let(::add)
            addAll(vias)
            destination?.let(::add)
        }
    }

    private fun trimWaypointsToFormLimit(
        waypoints: List<RegistrationWaypoint>,
        maxVias: Int,
    ): List<RegistrationWaypoint> {
        val normalized = normalizeWaypoints(waypoints)
        val start = normalized.firstOrNull { it.type == RegistrationWaypointType.START }
        val vias = normalized.filter { it.type == RegistrationWaypointType.VIA }
            .take(maxVias.coerceAtLeast(0))
        val destination = normalized.firstOrNull { it.type == RegistrationWaypointType.DESTINATION }
        return buildList {
            start?.let(::add)
            addAll(vias)
            destination?.let(::add)
        }
    }

    private fun maxViasReached(waypoints: List<RegistrationWaypoint> = _state.value.waypoints): Boolean {
        val maxWaypoints = _state.value.registrationForm?.maxWaypoints ?: Int.MAX_VALUE
        return waypoints.count { it.type == RegistrationWaypointType.VIA } >= maxWaypoints.coerceAtLeast(0)
    }
}

/**
 * 화면에는 사용자가 세는 단위(grapheme)로 [max]자까지 받되, 서버가 UTF-16 code unit으로
 * 길이를 검증하므로 code unit 합도 [max]를 넘지 않게 자른다. take()로 자르면 이모지의
 * 서로게이트 쌍이 중간에서 쪼개져 깨진 글자가 남는다.
 */
private fun String.limitForServer(max: Int): String =
    takeGraphemes(max).takeGraphemesWithinCodeUnits(max)
