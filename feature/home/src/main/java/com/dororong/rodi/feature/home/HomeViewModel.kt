package com.dororong.rodi.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.userMessage
import com.dororong.rodi.core.domain.usecase.course.GetRouteUseCase
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.LoginResult
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.model.member.PracticeRecordItem
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.practice.ActivePracticeSession
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import com.dororong.rodi.core.domain.usecase.auth.LoginWithKakaoUseCase
import com.dororong.rodi.core.domain.usecase.auth.RestoreWithKakaoUseCase
import com.dororong.rodi.core.domain.usecase.navi.GetNaviAlwaysUseCase
import com.dororong.rodi.core.domain.usecase.navi.SetNaviAlwaysUseCase
import com.dororong.rodi.core.domain.usecase.member.UpdateFilterTagsUseCase
import com.dororong.rodi.core.domain.usecase.place.GetPlaceCoordinatesUseCase
import com.dororong.rodi.core.domain.usecase.place.GetPlaceDetailUseCase
import com.dororong.rodi.core.domain.usecase.place.GetPlacesUseCase
import com.dororong.rodi.core.domain.usecase.place.RefreshPlaceCoordinatesUseCase
import com.dororong.rodi.core.domain.usecase.place.RefreshPlacesUseCase
import com.dororong.rodi.core.domain.usecase.place.SetPlaceBookmarkUseCase
import com.dororong.rodi.core.domain.usecase.driving.ObserveDrivingSessionUseCase
import com.dororong.rodi.core.domain.usecase.entry.MarkNotificationPermissionRequestedUseCase
import com.dororong.rodi.core.domain.usecase.practice.ClearActivePracticeSessionUseCase
import com.dororong.rodi.core.domain.usecase.practice.GetActivePracticeSessionUseCase
import com.dororong.rodi.core.domain.usecase.practice.RecordPracticeVisitUseCase
import com.dororong.rodi.core.domain.usecase.practice.RegisterPracticeUseCase
import com.dororong.rodi.core.domain.usecase.practice.SaveActivePracticeSessionUseCase
import com.dororong.rodi.feature.home.search.RegionOfficeLocation
import com.dororong.rodi.feature.home.filter.FilterCategory
import com.dororong.rodi.feature.home.filter.FilterPracticeOption
import com.dororong.rodi.feature.home.filter.practiceTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val PLACE_PAGE_SIZE = 20

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getPlaceCoordinatesUseCase: GetPlaceCoordinatesUseCase,
    private val refreshPlaceCoordinatesUseCase: RefreshPlaceCoordinatesUseCase,
    private val getPlacesUseCase: GetPlacesUseCase,
    private val refreshPlacesUseCase: RefreshPlacesUseCase,
    private val getPlaceDetailUseCase: GetPlaceDetailUseCase,
    private val getRouteUseCase: GetRouteUseCase,
    private val setPlaceBookmarkUseCase: SetPlaceBookmarkUseCase,
    private val getAuthSessionUseCase: GetAuthSessionUseCase,
    private val loginWithKakaoUseCase: LoginWithKakaoUseCase,
    private val restoreWithKakaoUseCase: RestoreWithKakaoUseCase,
    private val getNaviAlwaysUseCase: GetNaviAlwaysUseCase,
    private val setNaviAlwaysUseCase: SetNaviAlwaysUseCase,
    private val updateFilterTagsUseCase: UpdateFilterTagsUseCase,
    private val registerPracticeUseCase: RegisterPracticeUseCase,
    private val recordPracticeVisitUseCase: RecordPracticeVisitUseCase,
    private val getActivePracticeSessionUseCase: GetActivePracticeSessionUseCase,
    private val saveActivePracticeSessionUseCase: SaveActivePracticeSessionUseCase,
    private val clearActivePracticeSessionUseCase: ClearActivePracticeSessionUseCase,
    private val observeDrivingSessionUseCase: ObserveDrivingSessionUseCase,
    private val markNotificationPermissionRequestedUseCase: MarkNotificationPermissionRequestedUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    private val _permissionEffect = Channel<HomePermissionEffect>(Channel.BUFFERED)
    val permissionEffect: Flow<HomePermissionEffect> = _permissionEffect.receiveAsFlow()

    private var firstPageJob: Job? = null
    private var nextPageJob: Job? = null
    private var detailJob: Job? = null
    private var routeJob: Job? = null
    private var practicePromptJob: Job? = null
    private var practicePromptRequestGeneration = 0L
    private var practiceLaunchJob: Job? = null
    private var pendingPlaceSwitch: PendingPlaceSwitch? = null
    private var requestGeneration = 0L
    private var mapMovementGeneration = 0L
    private var lastFirstPageKey: PlaceRequestKey? = null
    private var pendingRestoreCredential: String? = null
    init {
        loadCoordinates()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.MapGestured -> {
                mapMovementGeneration += 1
                _uiState.update { it.copy(isMapSearchDirty = true) }
            }
            is HomeIntent.ViewportSettled -> loadInitialViewport(intent.query)
            is HomeIntent.ProgrammaticSearchRequested -> loadFirstPage(
                query = intent.query,
                force = true,
                clearMapMovementGeneration = mapMovementGeneration,
            )
            is HomeIntent.ResearchClicked -> {
                _uiState.update {
                    if (it.surfaceState == HomeSurfaceState.Navigation) {
                        it.copy(surfaceState = HomeSurfaceState.PartialList)
                    } else {
                        it
                    }
                }
                loadFirstPage(
                    query = intent.query,
                    force = true,
                    clearMapMovementGeneration = mapMovementGeneration,
                )
            }
            HomeIntent.ListOpenClicked -> _uiState.update { it.copy(surfaceState = HomeSurfaceState.PartialList) }
            HomeIntent.ListCollapseRequested -> collapseList()
            is HomeIntent.ListSheetSettled -> settleListSheet(intent.surface)
            HomeIntent.ListEndReached -> loadNextPage()
            is HomeIntent.PlaceClicked -> openPlace(intent.id, intent.origin)
            HomeIntent.DetailDismissed -> dismissDetail()
            HomeIntent.DetailDragDismissed -> dismissDetail(HomeSurfaceState.Navigation)
            HomeIntent.LevelReviewsOpened -> _uiState.update { it.copy(isLevelReviewsVisible = true) }
            HomeIntent.LevelReviewsClosed -> _uiState.update { it.copy(isLevelReviewsVisible = false) }
            HomeIntent.ReviewUpdated -> viewModelScope.launch { _effect.send(HomeEffect.RefreshReviews) }
            HomeIntent.AppResumed -> loadActivePracticeSession()
            HomeIntent.PracticeContinueClicked -> hidePracticeContinueDialog()
            HomeIntent.PracticeStopClicked -> stopPracticeMeasurement()
            HomeIntent.PracticeVisitedAnswered -> recordPracticeVisit()
            HomeIntent.PracticeNotVisitedAnswered -> openPracticeSkipReason()
            HomeIntent.PracticePromptDismissed -> dismissPracticePrompt()
            HomeIntent.NotificationPermissionAllowClicked -> allowNotificationPermission()
            HomeIntent.NotificationPermissionRouteOnlyClicked -> routeWithoutPracticeMeasurement()
            is HomeIntent.NotificationPermissionResultReceived -> onNotificationPermissionResult(intent.granted)
            HomeIntent.LevelUpDismissed -> _uiState.update { it.copy(levelUp = null) }
            HomeIntent.BookmarkClicked -> toggleBookmark()
            HomeIntent.MyPageClicked -> openMyPage()
            HomeIntent.RegisterClicked -> openCourseRegistration()
            is HomeIntent.SearchClicked -> openSearch(intent.origin)
            is HomeIntent.RegionSearchRequested -> prepareRegionSearch(intent.region, intent.initialPlaces)
            HomeIntent.FilterOpened -> _uiState.update { it.copy(isFilterSheetVisible = true) }
            is HomeIntent.FilterCategorySelected -> selectFilterCategory(intent.category)
            is HomeIntent.FilterPracticeOptionToggled -> toggleFilterPracticeOption(intent.option)
            HomeIntent.FilterResetClicked -> if (!_uiState.value.isFilterSaving) {
                _uiState.update {
                    it.copy(
                        activeFilterCategory = FilterCategory.BASIC_DRIVING,
                        selectedFilterPracticeTypes = emptySet(),
                    )
                }
            }
            HomeIntent.FilterApplyClicked -> applyFilter()
            HomeIntent.FilterDismissed -> dismissFilter()
            HomeIntent.LoginDismissed -> _uiState.update { it.copy(pendingAction = null, isLoginInProgress = false) }
            is HomeIntent.KakaoLoginSucceeded -> loginWithKakao(intent.accessToken)
            is HomeIntent.KakaoLoginFailed -> onKakaoLoginFailed(intent.message)
            HomeIntent.AccountRestoreClicked -> restoreAccount()
            HomeIntent.AccountRestoreDismissed -> {
                pendingRestoreCredential = null
                _uiState.update {
                    it.copy(
                        pendingAction = null,
                        hasPendingRestore = false,
                        isLoginInProgress = false,
                        isRestoreInProgress = false,
                    )
                }
            }
            is HomeIntent.NavigateClicked -> onNavigateClick(intent)
            is HomeIntent.NaviAppSelected -> onNaviAppSelected(intent)
            is HomeIntent.NaviAppInstallSelected -> onInstallNaviAppSelected(intent)
        }
    }

    private fun loadCoordinates() {
        viewModelScope.launch {
            getPlaceCoordinatesUseCase()
                .onSuccess { coordinates -> _uiState.update { it.copy(coordinates = coordinates.distinctBy { item -> item.id }) } }
                .onFailure { _effect.send(HomeEffect.ShowSnackbar(it.userMessage())) }
            refreshPlaceCoordinatesUseCase()
                .onSuccess { coordinates -> _uiState.update { it.copy(coordinates = coordinates.distinctBy { item -> item.id }) } }
        }
    }

    private fun prepareRegionSearch(
        region: RegionOfficeLocation,
        initialPlaces: List<PlaceSummary>,
    ) {
        requestGeneration += 1
        firstPageJob?.cancel()
        nextPageJob?.cancel()
        lastFirstPageKey = null
        _uiState.update {
            it.copy(
                surfaceState = HomeSurfaceState.PartialList,
                searchKeyword = region.displayName,
                regionSearch = region,
                places = initialPlaces.distinctBy(PlaceSummary::id),
                listState = HomeListState.Content,
                hasNextPage = false,
                nextCursor = null,
                totalCount = initialPlaces.size.toLong(),
                searchedQuery = null,
                isNextPageLoading = false,
                selectedPlaceId = null,
                selectedPlace = null,
                detailOrigin = null,
                isDetailLoading = false,
                isMapSearchDirty = false,
            )
        }
        viewModelScope.launch { _effect.send(HomeEffect.MoveToRegion(region)) }
    }

    private fun loadInitialViewport(query: PlaceViewportQuery) {
        if (_uiState.value.searchedQuery == null) loadFirstPage(query, force = false)
    }

    private fun loadFirstPage(
        query: PlaceViewportQuery,
        force: Boolean,
        clearMapMovementGeneration: Long? = null,
    ) {
        val key = PlaceRequestKey(query, cursor = null)
        if (!force && key == lastFirstPageKey) return
        if (key == lastFirstPageKey && firstPageJob?.isActive == true) return
        lastFirstPageKey = key
        requestGeneration += 1
        val generation = requestGeneration
        firstPageJob?.cancel()
        nextPageJob?.cancel()
        firstPageJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    listState = if (it.places.isEmpty()) HomeListState.Loading else it.listState,
                    isNextPageLoading = false,
                )
            }
            getPlacesUseCase(query, cursor = null, size = PLACE_PAGE_SIZE)
                .onSuccess { page ->
                    if (generation != requestGeneration) return@onSuccess
                    applyFirstPage(query, page)
                    refreshFirstPage(query, generation, clearMapMovementGeneration)
                }
                .onFailure { error ->
                    if (generation != requestGeneration) return@onFailure
                    _uiState.update { current ->
                        current.copy(
                            listState = if (current.places.isEmpty()) HomeListState.InitialError else current.listState,
                        )
                    }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private suspend fun refreshFirstPage(
        query: PlaceViewportQuery,
        generation: Long,
        clearMapMovementGeneration: Long?,
    ) {
        refreshPlacesUseCase(query, cursor = null, size = PLACE_PAGE_SIZE)
            .onSuccess { page ->
                if (generation == requestGeneration) {
                    applyFirstPage(query, page)
                    if (
                        clearMapMovementGeneration != null &&
                        clearMapMovementGeneration == mapMovementGeneration
                    ) {
                        _uiState.update { it.copy(isMapSearchDirty = false) }
                    }
                }
            }
            .onFailure { error ->
                if (generation == requestGeneration && _uiState.value.places.isEmpty()) {
                    _uiState.update { it.copy(listState = HomeListState.InitialError) }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
            }
    }

    private fun applyFirstPage(
        query: PlaceViewportQuery,
        page: CursorPage<PlaceSummary>,
    ) {
        val uniqueItems = page.items.distinctBy(PlaceSummary::id)
        _uiState.update {
            it.copy(
                places = uniqueItems,
                listState = if (uniqueItems.isEmpty()) HomeListState.Empty else HomeListState.Content,
                hasNextPage = page.hasNext,
                nextCursor = page.nextCursor,
                totalCount = page.totalCount,
                searchedQuery = query,
                isNextPageLoading = false,
                placeListGeneration = it.placeListGeneration + 1,
            )
        }
    }

    private fun loadNextPage() {
        val current = _uiState.value
        val query = current.searchedQuery ?: return
        val cursor = current.nextCursor ?: return
        if (!current.hasNextPage || current.isNextPageLoading || nextPageJob?.isActive == true) return
        val generation = requestGeneration
        nextPageJob = viewModelScope.launch {
            _uiState.update { it.copy(isNextPageLoading = true) }
            refreshPlacesUseCase(query, cursor = cursor, size = PLACE_PAGE_SIZE)
                .onSuccess { page ->
                    if (generation != requestGeneration) return@onSuccess
                    _uiState.update { latest ->
                        val merged = (latest.places + page.items).distinctBy(PlaceSummary::id)
                        latest.copy(
                            places = merged,
                            listState = if (merged.isEmpty()) HomeListState.Empty else HomeListState.Content,
                            hasNextPage = page.hasNext,
                            nextCursor = page.nextCursor,
                            isNextPageLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    if (generation != requestGeneration) return@onFailure
                    _uiState.update { it.copy(isNextPageLoading = false) }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun openPlace(placeId: Long, origin: HomeDetailOrigin) {
        detailJob?.cancel()
        routeJob?.cancel()
        detailJob = viewModelScope.launch {
            if (!isLoggedIn()) {
                requireLogin(PendingHomeAction.OpenDetail(placeId, origin))
                return@launch
            }
            _uiState.update {
                it.copy(
                    selectedPlaceId = placeId,
                    selectedPlace = null,
                    selectedRoute = null,
                    isRouting = false,
                    isBookmarkUpdating = false,
                    isLevelReviewsVisible = false,
                    detailOrigin = origin,
                    isDetailLoading = true,
                    surfaceState = HomeSurfaceState.Detail,
                    searchKeyword = if (origin == HomeDetailOrigin.Map) null else it.searchKeyword,
                )
            }
            getPlaceDetailUseCase(placeId)
                .onSuccess { detail ->
                    if (_uiState.value.selectedPlaceId == placeId) {
                        _uiState.update {
                            it.copy(
                                selectedPlace = detail,
                                isDetailLoading = false,
                                searchKeyword = detail.name.takeIf { origin == HomeDetailOrigin.List },
                            )
                        }
                        loadRoute(detail)
                    }
                }
                .onFailure { error ->
                    if (_uiState.value.selectedPlaceId == placeId) {
                        _uiState.update { current ->
                            current.copy(
                                selectedPlaceId = null,
                                selectedPlace = null,
                                selectedRoute = null,
                                detailOrigin = null,
                                isDetailLoading = false,
                                searchKeyword = null,
                                surfaceState = if (origin == HomeDetailOrigin.List) {
                                    HomeSurfaceState.PartialList
                                } else {
                                    HomeSurfaceState.Navigation
                                },
                            )
                        }
                    }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun resumePendingAction(action: PendingHomeAction) {
        when (action) {
            is PendingHomeAction.OpenDetail -> openPlace(action.placeId, action.origin)
            PendingHomeAction.ToggleBookmark -> toggleBookmark()
            PendingHomeAction.OpenMyPage -> viewModelScope.launch { _effect.send(HomeEffect.NavigateMyPage) }
            PendingHomeAction.OpenCourseRegistration -> viewModelScope.launch {
                _effect.send(HomeEffect.NavigateCourseRegistration)
            }
            is PendingHomeAction.OpenSearch -> viewModelScope.launch { _effect.send(HomeEffect.NavigateSearch(action.origin)) }
            is PendingHomeAction.SaveFilterTags -> saveFilterTags(action.filterTags)
        }
    }

    private fun dismissDetail() {
        val destination = if (_uiState.value.detailOrigin == HomeDetailOrigin.List) {
            HomeSurfaceState.PartialList
        } else {
            HomeSurfaceState.Navigation
        }
        dismissDetail(destination)
    }

    private fun dismissDetail(destination: HomeSurfaceState) {
        detailJob?.cancel()
        routeJob?.cancel()
        _uiState.update {
            it.copy(
                selectedPlaceId = null,
                selectedPlace = null,
                selectedRoute = null,
                isRouting = false,
                isBookmarkUpdating = false,
                detailOrigin = null,
                isDetailLoading = false,
                isLevelReviewsVisible = false,
                searchKeyword = null,
                surfaceState = destination,
            )
        }
    }

    private fun loadRoute(place: PlaceDetail) {
        if (place.course == null) {
            _uiState.update { uiState ->
                if (uiState.selectedPlaceId == place.id) uiState.copy(isRouting = false) else uiState
            }
            return
        }
        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            _uiState.update { it.copy(isRouting = true) }
            getRouteUseCase(place)
                .onSuccess { route ->
                    if (_uiState.value.selectedPlaceId == place.id) {
                        _uiState.update { it.copy(selectedRoute = route, isRouting = false) }
                    }
                }
                .onFailure {
                    if (_uiState.value.selectedPlaceId == place.id) {
                        _uiState.update { it.copy(isRouting = false) }
                    }
                }
        }
    }

    private fun toggleBookmark() {
        val place = _uiState.value.selectedPlace ?: return
        if (_uiState.value.isBookmarkUpdating) return
        viewModelScope.launch {
            if (!isLoggedIn()) {
                requireLogin(PendingHomeAction.ToggleBookmark)
                return@launch
            }
            val target = !place.isBookmarked
            _uiState.update { it.copy(isBookmarkUpdating = true) }
            setPlaceBookmarkUseCase(place, target)
                .onSuccess {
                    _uiState.update { current ->
                        val selectedPlace = current.selectedPlace
                        if (selectedPlace?.id != place.id) current else current.copy(
                            selectedPlace = selectedPlace.copy(
                                isBookmarked = target,
                                bookmarkCount = (selectedPlace.bookmarkCount + if (target) 1 else -1)
                                    .coerceAtLeast(0),
                            ),
                            isBookmarkUpdating = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        if (current.selectedPlace?.id == place.id) current.copy(isBookmarkUpdating = false) else current
                    }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun openMyPage() {
        viewModelScope.launch {
            if (isLoggedIn()) {
                _effect.send(HomeEffect.NavigateMyPage)
            } else {
                requireLogin(PendingHomeAction.OpenMyPage)
            }
        }
    }

    private fun openCourseRegistration() {
        viewModelScope.launch {
            if (isLoggedIn()) {
                _effect.send(HomeEffect.NavigateCourseRegistration)
            } else {
                requireLogin(PendingHomeAction.OpenCourseRegistration)
            }
        }
    }

    private fun openSearch(origin: GeoPoint?) {
        viewModelScope.launch {
            if (origin == null) {
                _effect.send(HomeEffect.ShowSnackbar("지도 위치를 준비 중이에요. 잠시 후 다시 시도해주세요."))
            } else if (isLoggedIn()) {
                _effect.send(HomeEffect.NavigateSearch(origin))
            } else {
                requireLogin(PendingHomeAction.OpenSearch(origin))
            }
        }
    }

    private fun selectFilterCategory(category: FilterCategory) {
        if (_uiState.value.isFilterSaving) return
        _uiState.update { current ->
            if (current.activeFilterCategory == category) {
                current.copy(
                    activeFilterCategory = null,
                    selectedFilterPracticeTypes = if (category == FilterCategory.PARKING) {
                        current.selectedFilterPracticeTypes - PracticeType.PARKING
                    } else {
                        current.selectedFilterPracticeTypes
                    },
                )
            } else {
                current.copy(
                    activeFilterCategory = category,
                    selectedFilterPracticeTypes = if (category == FilterCategory.PARKING) {
                        current.selectedFilterPracticeTypes + PracticeType.PARKING
                    } else {
                        current.selectedFilterPracticeTypes
                    },
                )
            }
        }
    }

    private fun toggleFilterPracticeOption(option: FilterPracticeOption) {
        if (_uiState.value.isFilterSaving) return
        _uiState.update { current ->
            val activeCategory = current.activeFilterCategory ?: return@update current
            val targetTypes = when (option) {
                FilterPracticeOption.ALL -> activeCategory.practiceTypes()
                else -> setOf(requireNotNull(option.practiceType))
            }
            val selectedTypes = if (targetTypes.all(current.selectedFilterPracticeTypes::contains)) {
                current.selectedFilterPracticeTypes - targetTypes
            } else {
                current.selectedFilterPracticeTypes + targetTypes
            }
            current.copy(selectedFilterPracticeTypes = selectedTypes)
        }
    }

    private fun applyFilter() {
        val filterTags = _uiState.value.selectedFilterPracticeTypes
        if (_uiState.value.isFilterSaving) return
        viewModelScope.launch {
            if (isLoggedIn()) {
                saveFilterTags(filterTags)
            } else {
                requireLogin(PendingHomeAction.SaveFilterTags(filterTags))
            }
        }
    }

    private fun saveFilterTags(filterTags: Set<PracticeType>) {
        if (_uiState.value.isFilterSaving) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedFilterPracticeTypes = filterTags,
                    isFilterSaving = true,
                )
            }
            updateFilterTagsUseCase(filterTags)
                .onSuccess {
                    _uiState.update { it.copy(isFilterSheetVisible = false, isFilterSaving = false) }
                    _uiState.value.searchedQuery?.let { query ->
                        loadFirstPage(query, force = true)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isFilterSaving = false) }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun dismissFilter() {
        if (!_uiState.value.isFilterSaving) {
            _uiState.update { it.copy(isFilterSheetVisible = false) }
        }
    }

    private fun requireLogin(action: PendingHomeAction) {
        _uiState.update { it.copy(pendingAction = action, isLoginInProgress = false) }
    }

    private fun loginWithKakao(accessToken: String) {
        val action = _uiState.value.pendingAction ?: return
        if (_uiState.value.isLoginInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoginInProgress = true) }
            loginWithKakaoUseCase(accessToken)
                .onSuccess { result ->
                    when (result) {
                        is LoginResult.Success -> if (_uiState.value.pendingAction == action) {
                            _uiState.update { it.copy(pendingAction = null, isLoginInProgress = false) }
                            if (result.isNewMember) {
                                _effect.send(HomeEffect.NavigateGuestSignUp)
                            } else {
                                resumePendingAction(action)
                            }
                        }
                        is LoginResult.WithdrawalPending -> {
                            pendingRestoreCredential = accessToken
                            _uiState.update {
                                it.copy(
                                    hasPendingRestore = true,
                                    isLoginInProgress = false,
                                )
                            }
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoginInProgress = false) }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun restoreAccount() {
        val credential = pendingRestoreCredential ?: return
        val action = _uiState.value.pendingAction ?: return
        if (_uiState.value.isRestoreInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoreInProgress = true) }
            restoreWithKakaoUseCase(credential)
                .onSuccess { result ->
                    if (result is AccountRestoreResult.Restored && _uiState.value.pendingAction == action) {
                        pendingRestoreCredential = null
                        _uiState.update {
                            it.copy(
                                pendingAction = null,
                                hasPendingRestore = false,
                                isRestoreInProgress = false,
                            )
                        }
                        resumePendingAction(action)
                    } else {
                        _uiState.update { it.copy(isRestoreInProgress = false) }
                        _effect.send(HomeEffect.ShowSnackbar("계정 복구를 완료하지 못했습니다."))
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isRestoreInProgress = false) }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun onKakaoLoginFailed(message: String) {
        if (message.contains("취소")) {
            _uiState.update { it.copy(pendingAction = null, isLoginInProgress = false) }
        } else {
            _uiState.update { it.copy(isLoginInProgress = false) }
            viewModelScope.launch { _effect.send(HomeEffect.ShowSnackbar(message)) }
        }
    }

    private fun collapseList() {
        _uiState.update {
            val nextSurface = when (it.surfaceState) {
                HomeSurfaceState.FullList -> HomeSurfaceState.PartialList
                HomeSurfaceState.PartialList -> HomeSurfaceState.Navigation
                else -> it.surfaceState
            }
            if (nextSurface == HomeSurfaceState.Navigation && it.regionSearch != null) {
                it.copy(surfaceState = nextSurface, searchKeyword = null, regionSearch = null)
            } else {
                it.copy(surfaceState = nextSurface)
            }
        }
    }

    /**
     * 시트가 정착한 앵커를 그대로 반영한다. [collapseList]처럼 한 단계씩 내려가면 Full에서 Hidden까지
     * 한 번에 끌었을 때 PartialList로만 내려가 시트가 도로 튀어 올라간다.
     *
     * 상세를 여는 중에는 시트가 Hidden으로 정착하며 늦게 도착한 이벤트가 Detail을 덮어쓸 수 있어 무시한다.
     */
    private fun settleListSheet(surface: HomeSurfaceState) {
        _uiState.update {
            if (it.surfaceState == HomeSurfaceState.Detail) it else it.copy(surfaceState = surface)
        }
    }

    private fun onNavigateClick(intent: HomeIntent.NavigateClicked) {
        val place = _uiState.value.selectedPlace ?: return
        viewModelScope.launch {
            val savedApp = getNaviAlwaysUseCase()
            when {
                savedApp == NaviApp.KAKAOMAP && intent.kakaoMapInstalled ->
                    requestPracticeNavigation(place, NaviApp.KAKAOMAP, intent.notificationPermissionGranted)
                savedApp == NaviApp.KAKAONAVI && intent.kakaoNaviInstalled ->
                    requestPracticeNavigation(place, NaviApp.KAKAONAVI, intent.notificationPermissionGranted)
                intent.kakaoMapInstalled && intent.kakaoNaviInstalled ->
                    _effect.send(HomeEffect.ShowNaviPicker(place))
                intent.kakaoMapInstalled ->
                    requestPracticeNavigation(place, NaviApp.KAKAOMAP, intent.notificationPermissionGranted)
                intent.kakaoNaviInstalled ->
                    requestPracticeNavigation(place, NaviApp.KAKAONAVI, intent.notificationPermissionGranted)
                else -> _effect.send(HomeEffect.ShowInstallNaviPicker(place))
            }
        }
    }

    private fun onNaviAppSelected(intent: HomeIntent.NaviAppSelected) {
        val place = _uiState.value.selectedPlace ?: return
        viewModelScope.launch {
            if (intent.always) setNaviAlwaysUseCase(intent.app)
            requestPracticeNavigation(place, intent.app, intent.notificationPermissionGranted)
        }
    }

    private fun onInstallNaviAppSelected(intent: HomeIntent.NaviAppInstallSelected) {
        viewModelScope.launch { _effect.send(HomeEffect.OpenNaviInstallPage(intent.app)) }
    }

    private fun requestPracticeNavigation(place: PlaceDetail, app: NaviApp, notificationPermissionGranted: Boolean) {
        if (practiceLaunchJob?.isActive == true || _uiState.value.isPracticeLaunchInProgress) return
        val activeSession = _uiState.value.activePracticeSession
        if (activeSession != null && activeSession.isMeasured && activeSession.placeId != place.id) {
            pendingPlaceSwitch = PendingPlaceSwitch(place, app, notificationPermissionGranted)
            _uiState.update { it.copy(isPracticeContinueDialogVisible = true) }
            return
        }
        practiceLaunchJob = viewModelScope.launch {
            _uiState.update { it.copy(isPracticeLaunchInProgress = true) }
            if (notificationPermissionGranted) {
                startPracticeNavigation(PendingPracticeNavigation(place, app))
                return@launch
            }
            _uiState.update {
                it.copy(
                    isPracticeLaunchInProgress = false,
                    isNotificationPermissionRationaleVisible = true,
                    pendingPracticeNavigation = PendingPracticeNavigation(place, app),
                )
            }
        }
    }

    private fun allowNotificationPermission() {
        if (_uiState.value.pendingPracticeNavigation == null || _uiState.value.isPracticeLaunchInProgress) return
        viewModelScope.launch {
            markNotificationPermissionRequestedSafely()
            _uiState.update {
                it.copy(
                    isNotificationPermissionRationaleVisible = false,
                    isPracticeLaunchInProgress = true,
                )
            }
            _permissionEffect.send(HomePermissionEffect.RequestNotificationPermission)
        }
    }

    private fun routeWithoutPracticeMeasurement() {
        val pending = _uiState.value.pendingPracticeNavigation ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isNotificationPermissionRationaleVisible = false,
                    pendingPracticeNavigation = null,
                    isPracticeLaunchInProgress = false,
                )
            }
            val session = ActivePracticeSession(
                placeId = pending.place.id,
                placeName = pending.place.name,
                placeType = pending.place.type,
                startedAt = Instant.now(clock),
                isMeasured = false,
            )
            try {
                saveActivePracticeSessionWithRetry(session)
                _uiState.update { it.copy(activePracticeSession = session) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // 저장에 실패해도 경로 안내 자체는 막지 않는다 — 재진입 확인만 못 받을 뿐이다.
            }
            _effect.send(pending.navigationEffect(startDriving = false))
        }
    }

    private fun onNotificationPermissionResult(granted: Boolean) {
        if (!granted) {
            routeWithoutPracticeMeasurement()
            return
        }
        val pending = _uiState.value.pendingPracticeNavigation ?: return
        viewModelScope.launch { startPracticeNavigation(pending) }
    }

    private suspend fun startPracticeNavigation(pending: PendingPracticeNavigation) {
        val session = ActivePracticeSession(
            placeId = pending.place.id,
            placeName = pending.place.name,
            placeType = pending.place.type,
            startedAt = Instant.now(clock),
        )
        try {
            saveActivePracticeSessionWithRetry(session)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            _uiState.update {
                it.copy(
                    activePracticeSession = null,
                    practicePrompt = null,
                    isPracticeContinueDialogVisible = false,
                    isNotificationPermissionRationaleVisible = false,
                    pendingPracticeNavigation = null,
                    isPracticeLaunchInProgress = false,
                )
            }
            _effect.send(HomeEffect.ShowSnackbar("연습 측정을 시작하지 못해 경로만 안내합니다."))
            _effect.send(pending.navigationEffect(startDriving = false))
            return
        }
        _uiState.update {
            it.copy(
                activePracticeSession = session,
                practicePrompt = null,
                isPracticeContinueDialogVisible = false,
                isNotificationPermissionRationaleVisible = false,
                pendingPracticeNavigation = null,
                isPracticeLaunchInProgress = false,
            )
        }
        _effect.send(pending.navigationEffect())
    }

    private fun loadActivePracticeSession() {
        practicePromptJob?.cancel()
        val generation = ++practicePromptRequestGeneration
        practicePromptJob = viewModelScope.launch {
            if (!isLoggedIn()) return@launch
            if (_uiState.value.isPracticeActionInProgress) return@launch
            val session = try {
                getActivePracticeSessionUseCase()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                return@launch
            }
            if (generation != practicePromptRequestGeneration) return@launch
            if (session == null) {
                _uiState.update {
                    it.copy(
                        activePracticeSession = null,
                        practicePrompt = null,
                        isPracticeContinueDialogVisible = false,
                    )
                }
                return@launch
            }
            if (session.isCompleted) {
                _uiState.update {
                    it.copy(
                        activePracticeSession = null,
                        practicePrompt = null,
                        isPracticeContinueDialogVisible = false,
                    )
                }
                return@launch
            }
            val elapsed = Duration.between(session.startedAt, Instant.now(clock))
            if (session.isArrivalConfirmed || elapsed >= PRACTICE_MEASUREMENT_DURATION) {
                _uiState.update {
                    it.copy(
                        activePracticeSession = session,
                        practicePrompt = session.toPracticeRecordItem(),
                        isPracticeContinueDialogVisible = false,
                    )
                }
            } else if (session.isMeasured) {
                _uiState.update {
                    it.copy(
                        activePracticeSession = session,
                        practicePrompt = null,
                        isPracticeContinueDialogVisible = true,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        activePracticeSession = session,
                        practicePrompt = null,
                        isPracticeContinueDialogVisible = false,
                    )
                }
            }
        }
    }

    private fun hidePracticeContinueDialog() {
        pendingPlaceSwitch = null
        _uiState.update { it.copy(isPracticeContinueDialogVisible = false) }
    }

    private fun dismissPracticePrompt() {
        if (_uiState.value.isPracticeActionInProgress) return
        viewModelScope.launch {
            try {
                // 로컬 세션을 지우지 않으면 다음 앱 재진입 때 loadActivePracticeSession()이
                // 같은 세션을 다시 읽어 방문 확인 프롬프트가 그대로 재등장한다.
                clearActivePracticeSessionWithRetry()
                _uiState.update {
                    it.copy(
                        activePracticeSession = null,
                        practicePrompt = null,
                        isPracticeContinueDialogVisible = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
            }
        }
    }

    private fun stopPracticeMeasurement() {
        if (_uiState.value.isPracticeActionInProgress) return
        viewModelScope.launch {
            try {
                clearActivePracticeSessionWithRetry()
                _uiState.update {
                    it.copy(
                        activePracticeSession = null,
                        practicePrompt = null,
                        isPracticeContinueDialogVisible = false,
                    )
                }
                pendingPlaceSwitch?.let { switch ->
                    pendingPlaceSwitch = null
                    requestPracticeNavigation(switch.place, switch.app, switch.notificationPermissionGranted)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
            }
        }
    }

    private fun openPracticeSkipReason() {
        val session = _uiState.value.activePracticeSession ?: return
        if (_uiState.value.isPracticeActionInProgress) return
        _uiState.update { it.copy(isPracticeActionInProgress = true) }
        viewModelScope.launch {
            try {
                val practiceId = session.practiceId ?: run {
                    registerPracticeUseCase(session.placeId).getOrElse { error ->
                        _uiState.update { it.copy(isPracticeActionInProgress = false) }
                        _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                        return@launch
                    }.practiceId
                }
                try {
                    clearActivePracticeSessionWithRetry()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    _uiState.update { it.copy(isPracticeActionInProgress = false) }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        activePracticeSession = null,
                        practicePrompt = null,
                        isPracticeContinueDialogVisible = false,
                        isPracticeActionInProgress = false,
                    )
                }
                _effect.send(HomeEffect.OpenPracticeSkipReason(practiceId))
            } finally {
                _uiState.update { current ->
                    if (current.isPracticeActionInProgress) {
                        current.copy(isPracticeActionInProgress = false)
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun recordPracticeVisit() {
        val session = _uiState.value.activePracticeSession ?: return
        if (_uiState.value.isPracticeActionInProgress) return
        _uiState.update { it.copy(isPracticeActionInProgress = true) }
        viewModelScope.launch {
            try {
                var practiceId = session.practiceId
                if (practiceId == null) {
                    val registration = registerPracticeUseCase(session.placeId)
                    val practice = registration.getOrElse { error ->
                        _uiState.update { it.copy(isPracticeActionInProgress = false) }
                        _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                        return@launch
                    }
                    practiceId = practice.practiceId
                    val pendingSession = session.copy(practiceId = practiceId)
                    _uiState.update { it.copy(activePracticeSession = pendingSession) }
                    try {
                        saveActivePracticeSessionWithRetry(pendingSession)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                    }
                }
                // GPS로 실제 도착을 확인한 세션만 인정거리를 방문 인증에 실어 보낸다.
                // 10분 휴리스틱으로만 판단된(알림 미허용) 세션은 실측 거리가 없으므로 null.
                val certifiedDistanceMeters = if (session.isArrivalConfirmed) {
                    try {
                        observeDrivingSessionUseCase().first()
                            ?.takeIf { it.placeId == session.placeId }
                            ?.traveledDistanceMeters
                            ?.roundToInt()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        null
                    }
                } else {
                    null
                }
                val result = recordPracticeVisitUseCase(practiceId, certifiedDistanceMeters)
                result.onSuccess { visitResult ->
                    val completedSession = session.copy(
                        practiceId = practiceId,
                        isCompleted = true,
                    )
                    try {
                        saveActivePracticeSessionWithRetry(completedSession)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                    }
                    try {
                        clearActivePracticeSessionWithRetry()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                    }
                    _uiState.update {
                        it.copy(
                            activePracticeSession = null,
                            practicePrompt = null,
                            isPracticeContinueDialogVisible = false,
                            isPracticeActionInProgress = false,
                            levelUp = visitResult.newLevel.takeIf { level -> visitResult.levelUp && level != null },
                        )
                    }
                    if (session.placeType == PlaceType.COURSE) {
                        _effect.send(HomeEffect.OpenPracticeReview(session.placeId, session.placeName))
                    } else {
                        _effect.send(HomeEffect.ShowSnackbar("연습 기록에 추가되었습니다"))
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(isPracticeActionInProgress = false) }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
            } finally {
                _uiState.update { current ->
                    if (current.isPracticeActionInProgress) current.copy(isPracticeActionInProgress = false) else current
                }
            }
        }
    }

    private suspend fun markNotificationPermissionRequestedSafely() {
        try {
            markNotificationPermissionRequestedUseCase()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
        }
    }

    private suspend fun clearActivePracticeSessionWithRetry() {
        var lastError: Throwable? = null
        repeat(PRACTICE_SESSION_CLEAR_ATTEMPTS) {
            try {
                clearActivePracticeSessionUseCase()
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("연습 측정 세션을 정리하지 못했습니다.")
    }

    private suspend fun saveActivePracticeSessionWithRetry(session: ActivePracticeSession) {
        var lastError: Throwable? = null
        repeat(PRACTICE_SESSION_SAVE_ATTEMPTS) {
            try {
                saveActivePracticeSessionUseCase(session)
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("연습 측정 세션 상태를 저장하지 못했습니다.")
    }

    private suspend fun isLoggedIn(): Boolean = try {
        getAuthSessionUseCase().isLoggedIn
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        false
    }
}

private data class PendingPlaceSwitch(
    val place: PlaceDetail,
    val app: NaviApp,
    val notificationPermissionGranted: Boolean,
)

private fun PendingPracticeNavigation.navigationEffect(
    startDriving: Boolean = true,
): HomeEffect = when (app) {
    NaviApp.KAKAOMAP -> HomeEffect.LaunchKakaoMap(place, startDriving)
    NaviApp.KAKAONAVI -> HomeEffect.LaunchKakaoNavi(place, startDriving)
}

private fun ActivePracticeSession.toPracticeRecordItem() = PracticeRecordItem(
    practiceId = practiceId ?: LOCAL_PRACTICE_ID,
    placeId = placeId,
    placeName = placeName,
    practiceTypes = if (placeType == PlaceType.PARKING) listOf(PracticeType.PARKING) else emptyList(),
    visitCount = 0,
    visitedAt = null,
    isVerified = false,
    hasReview = false,
)

internal data class PlaceRequestKey(
    val query: PlaceViewportQuery,
    val cursor: String?,
)
private const val LOCAL_PRACTICE_ID = 0L
private const val PRACTICE_SESSION_CLEAR_ATTEMPTS = 3
private const val PRACTICE_SESSION_SAVE_ATTEMPTS = 3
private val PRACTICE_MEASUREMENT_DURATION: Duration = Duration.ofMinutes(10)
