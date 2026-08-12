package com.dororong.rodi.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.userMessage
import com.dororong.rodi.core.domain.usecase.course.GetRouteUseCase
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.LoginResult
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.place.PracticeType
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
import com.dororong.rodi.core.domain.usecase.practice.ClearPracticeSessionUseCase
import com.dororong.rodi.core.domain.usecase.practice.GetCompletedPracticeSessionUseCase
import com.dororong.rodi.core.domain.usecase.practice.StartPracticeSessionUseCase
import com.dororong.rodi.feature.home.search.RegionOfficeLocation
import com.dororong.rodi.feature.home.filter.FilterCategory
import com.dororong.rodi.feature.home.filter.FilterPracticeOption
import com.dororong.rodi.feature.home.filter.practiceTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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
    private val startPracticeSessionUseCase: StartPracticeSessionUseCase,
    private val getCompletedPracticeSessionUseCase: GetCompletedPracticeSessionUseCase,
    private val clearPracticeSessionUseCase: ClearPracticeSessionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    private var firstPageJob: Job? = null
    private var nextPageJob: Job? = null
    private var detailJob: Job? = null
    private var routeJob: Job? = null
    private var requestGeneration = 0L
    private var mapMovementGeneration = 0L
    private var lastFirstPageKey: PlaceRequestKey? = null
    private var pendingRestoreCredential: String? = null

    init {
        loadCoordinates()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.OnMapGesture -> {
                mapMovementGeneration += 1
                _state.update { it.copy(isMapSearchDirty = true) }
            }
            is HomeIntent.OnViewportSettled -> loadInitialViewport(intent.query)
            is HomeIntent.OnProgrammaticSearch -> loadFirstPage(
                query = intent.query,
                force = true,
                clearMapMovementGeneration = mapMovementGeneration,
            )
            is HomeIntent.OnResearch -> loadFirstPage(
                query = intent.query,
                force = true,
                clearMapMovementGeneration = mapMovementGeneration,
            )
            HomeIntent.OnListOpen -> _state.update { it.copy(surfaceState = HomeSurfaceState.PartialList) }
            HomeIntent.OnListCollapse -> collapseList()
            is HomeIntent.OnListSheetSettled -> settleListSheet(intent.surface)
            HomeIntent.OnLoadNextPage -> loadNextPage()
            is HomeIntent.OnPlaceClick -> openPlace(intent.id, intent.origin)
            HomeIntent.OnDismissDetail -> dismissDetail()
            HomeIntent.OnDragDismissDetail -> dismissDetail(HomeSurfaceState.Navigation)
            HomeIntent.OnLevelReviewsOpen -> _state.update { it.copy(isLevelReviewsVisible = true) }
            HomeIntent.OnLevelReviewsClose -> _state.update { it.copy(isLevelReviewsVisible = false) }
            HomeIntent.OnAppResumed -> loadCompletedPracticeSession()
            HomeIntent.OnPracticePromptVisited,
            HomeIntent.OnPracticePromptNotVisited,
            HomeIntent.OnPracticePromptDismiss,
            -> clearPracticePrompt()
            HomeIntent.OnBookmarkClick -> toggleBookmark()
            HomeIntent.OnMyClick -> openMyPage()
            is HomeIntent.OnSearchClick -> openSearch(intent.origin)
            is HomeIntent.OnRegionSearch -> prepareRegionSearch(intent.region, intent.initialPlaces)
            HomeIntent.OnFilterOpen -> _state.update { it.copy(isFilterSheetVisible = true) }
            is HomeIntent.OnFilterCategorySelect -> selectFilterCategory(intent.category)
            is HomeIntent.OnFilterPracticeOptionToggle -> toggleFilterPracticeOption(intent.option)
            HomeIntent.OnFilterReset -> if (!_state.value.isFilterSaving) {
                _state.update {
                    it.copy(
                        activeFilterCategory = FilterCategory.BASIC_DRIVING,
                        selectedFilterPracticeTypes = emptySet(),
                    )
                }
            }
            HomeIntent.OnFilterApply -> applyFilter()
            HomeIntent.OnFilterDismiss -> dismissFilter()
            HomeIntent.OnDismissLogin -> _state.update { it.copy(pendingAction = null, isLoginInProgress = false) }
            is HomeIntent.OnKakaoLoginCredential -> loginWithKakao(intent.accessToken)
            is HomeIntent.OnKakaoLoginFailed -> onKakaoLoginFailed(intent.message)
            HomeIntent.OnRestoreAccount -> restoreAccount()
            HomeIntent.OnDismissRestore -> {
                pendingRestoreCredential = null
                _state.update {
                    it.copy(
                        pendingAction = null,
                        hasPendingRestore = false,
                        isLoginInProgress = false,
                        isRestoreInProgress = false,
                    )
                }
            }
            is HomeIntent.OnNavigateClick -> onNavigateClick(intent)
            is HomeIntent.OnNaviAppSelected -> onNaviAppSelected(intent)
            is HomeIntent.OnInstallNaviAppSelected -> onInstallNaviAppSelected(intent)
        }
    }

    private fun loadCoordinates() {
        viewModelScope.launch {
            getPlaceCoordinatesUseCase()
                .onSuccess { coordinates -> _state.update { it.copy(coordinates = coordinates.distinctBy { item -> item.id }) } }
                .onFailure { _effect.send(HomeEffect.ShowSnackbar(it.userMessage())) }
            refreshPlaceCoordinatesUseCase()
                .onSuccess { coordinates -> _state.update { it.copy(coordinates = coordinates.distinctBy { item -> item.id }) } }
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
        _state.update {
            it.copy(
                surfaceState = HomeSurfaceState.PartialList,
                searchKeyword = region.displayName,
                regionSearch = region,
                regionSearchGeneration = it.regionSearchGeneration + 1,
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
    }

    private fun loadInitialViewport(query: PlaceViewportQuery) {
        if (_state.value.searchedQuery == null) loadFirstPage(query, force = false)
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
            _state.update {
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
                    _state.update { current ->
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
                        _state.update { it.copy(isMapSearchDirty = false) }
                    }
                }
            }
            .onFailure { error ->
                if (generation == requestGeneration && _state.value.places.isEmpty()) {
                    _state.update { it.copy(listState = HomeListState.InitialError) }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
            }
    }

    private fun applyFirstPage(
        query: PlaceViewportQuery,
        page: CursorPage<PlaceSummary>,
    ) {
        val uniqueItems = page.items.distinctBy(PlaceSummary::id)
        _state.update {
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
        val current = _state.value
        val query = current.searchedQuery ?: return
        val cursor = current.nextCursor ?: return
        if (!current.hasNextPage || current.isNextPageLoading || nextPageJob?.isActive == true) return
        val generation = requestGeneration
        nextPageJob = viewModelScope.launch {
            _state.update { it.copy(isNextPageLoading = true) }
            refreshPlacesUseCase(query, cursor = cursor, size = PLACE_PAGE_SIZE)
                .onSuccess { page ->
                    if (generation != requestGeneration) return@onSuccess
                    _state.update { latest ->
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
                    _state.update { it.copy(isNextPageLoading = false) }
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
            _state.update {
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
                )
            }
            getPlaceDetailUseCase(placeId)
                .onSuccess { detail ->
                    if (_state.value.selectedPlaceId == placeId) {
                        _state.update {
                            it.copy(selectedPlace = detail, isDetailLoading = false, searchKeyword = detail.name)
                        }
                        loadRoute(detail)
                    }
                }
                .onFailure { error ->
                    if (_state.value.selectedPlaceId == placeId) {
                        _state.update { current ->
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
            is PendingHomeAction.OpenSearch -> viewModelScope.launch { _effect.send(HomeEffect.NavigateSearch(action.origin)) }
            is PendingHomeAction.SaveFilterTags -> saveFilterTags(action.filterTags)
        }
    }

    private fun dismissDetail() {
        val destination = if (_state.value.detailOrigin == HomeDetailOrigin.List) {
            HomeSurfaceState.PartialList
        } else {
            HomeSurfaceState.Navigation
        }
        dismissDetail(destination)
    }

    private fun dismissDetail(destination: HomeSurfaceState) {
        detailJob?.cancel()
        routeJob?.cancel()
        _state.update {
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
            _state.update { state ->
                if (state.selectedPlaceId == place.id) state.copy(isRouting = false) else state
            }
            return
        }
        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            _state.update { it.copy(isRouting = true) }
            getRouteUseCase(place)
                .onSuccess { route ->
                    if (_state.value.selectedPlaceId == place.id) {
                        _state.update { it.copy(selectedRoute = route, isRouting = false) }
                    }
                }
                .onFailure {
                    if (_state.value.selectedPlaceId == place.id) {
                        _state.update { it.copy(isRouting = false) }
                    }
                }
        }
    }

    private fun toggleBookmark() {
        val place = _state.value.selectedPlace ?: return
        if (_state.value.isBookmarkUpdating) return
        viewModelScope.launch {
            if (!isLoggedIn()) {
                requireLogin(PendingHomeAction.ToggleBookmark)
                return@launch
            }
            val target = !place.isBookmarked
            _state.update { it.copy(isBookmarkUpdating = true) }
            setPlaceBookmarkUseCase(place, target)
                .onSuccess {
                    _state.update { current ->
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
                    _state.update { current ->
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
        if (_state.value.isFilterSaving) return
        _state.update { current ->
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
        if (_state.value.isFilterSaving) return
        _state.update { current ->
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
        val filterTags = _state.value.selectedFilterPracticeTypes
        if (_state.value.isFilterSaving) return
        viewModelScope.launch {
            if (isLoggedIn()) {
                saveFilterTags(filterTags)
            } else {
                requireLogin(PendingHomeAction.SaveFilterTags(filterTags))
            }
        }
    }

    private fun saveFilterTags(filterTags: Set<PracticeType>) {
        if (_state.value.isFilterSaving) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedFilterPracticeTypes = filterTags,
                    isFilterSaving = true,
                )
            }
            updateFilterTagsUseCase(filterTags)
                .onSuccess {
                    _state.update { it.copy(isFilterSheetVisible = false, isFilterSaving = false) }
                    _state.value.searchedQuery?.let { query ->
                        loadFirstPage(query, force = true)
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isFilterSaving = false) }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun dismissFilter() {
        if (!_state.value.isFilterSaving) {
            _state.update { it.copy(isFilterSheetVisible = false) }
        }
    }

    private fun requireLogin(action: PendingHomeAction) {
        _state.update { it.copy(pendingAction = action, isLoginInProgress = false) }
    }

    private fun loginWithKakao(accessToken: String) {
        val action = _state.value.pendingAction ?: return
        if (_state.value.isLoginInProgress) return
        viewModelScope.launch {
            _state.update { it.copy(isLoginInProgress = true) }
            loginWithKakaoUseCase(accessToken)
                .onSuccess { result ->
                    when (result) {
                        is LoginResult.Success -> if (_state.value.pendingAction == action) {
                            _state.update { it.copy(pendingAction = null, isLoginInProgress = false) }
                            if (result.isNewMember) {
                                _effect.send(HomeEffect.NavigateGuestSignUp)
                            } else {
                                resumePendingAction(action)
                            }
                        }
                        is LoginResult.WithdrawalPending -> {
                            pendingRestoreCredential = accessToken
                            _state.update {
                                it.copy(
                                    hasPendingRestore = true,
                                    isLoginInProgress = false,
                                )
                            }
                        }
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoginInProgress = false) }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun restoreAccount() {
        val credential = pendingRestoreCredential ?: return
        val action = _state.value.pendingAction ?: return
        if (_state.value.isRestoreInProgress) return
        viewModelScope.launch {
            _state.update { it.copy(isRestoreInProgress = true) }
            restoreWithKakaoUseCase(credential)
                .onSuccess { result ->
                    if (result is AccountRestoreResult.Restored && _state.value.pendingAction == action) {
                        pendingRestoreCredential = null
                        _state.update {
                            it.copy(
                                pendingAction = null,
                                hasPendingRestore = false,
                                isRestoreInProgress = false,
                            )
                        }
                        resumePendingAction(action)
                    } else {
                        _state.update { it.copy(isRestoreInProgress = false) }
                        _effect.send(HomeEffect.ShowSnackbar("계정 복구를 완료하지 못했습니다."))
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isRestoreInProgress = false) }
                    _effect.send(HomeEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun onKakaoLoginFailed(message: String) {
        if (message.contains("취소")) {
            _state.update { it.copy(pendingAction = null, isLoginInProgress = false) }
        } else {
            _state.update { it.copy(isLoginInProgress = false) }
            viewModelScope.launch { _effect.send(HomeEffect.ShowSnackbar(message)) }
        }
    }

    private fun collapseList() {
        _state.update {
            it.copy(
                surfaceState = when (it.surfaceState) {
                    HomeSurfaceState.FullList -> HomeSurfaceState.PartialList
                    HomeSurfaceState.PartialList -> HomeSurfaceState.Navigation
                    else -> it.surfaceState
                },
            )
        }
    }

    /**
     * 시트가 정착한 앵커를 그대로 반영한다. [collapseList]처럼 한 단계씩 내려가면 Full에서 Hidden까지
     * 한 번에 끌었을 때 PartialList로만 내려가 시트가 도로 튀어 올라간다.
     *
     * 상세를 여는 중에는 시트가 Hidden으로 정착하며 늦게 도착한 이벤트가 Detail을 덮어쓸 수 있어 무시한다.
     */
    private fun settleListSheet(surface: HomeSurfaceState) {
        _state.update {
            if (it.surfaceState == HomeSurfaceState.Detail) it else it.copy(surfaceState = surface)
        }
    }

    private fun onNavigateClick(intent: HomeIntent.OnNavigateClick) {
        val place = _state.value.selectedPlace ?: return
        viewModelScope.launch {
            val savedApp = getNaviAlwaysUseCase()
            when {
                savedApp == NaviApp.KAKAOMAP && intent.kakaoMapInstalled -> launchPractice(place, HomeEffect.LaunchKakaoMap(place))
                savedApp == NaviApp.KAKAONAVI && intent.kakaoNaviInstalled -> launchPractice(place, HomeEffect.LaunchKakaoNavi(place))
                intent.kakaoMapInstalled && intent.kakaoNaviInstalled ->
                    _effect.send(HomeEffect.ShowNaviPicker(place))
                intent.kakaoMapInstalled -> launchPractice(place, HomeEffect.LaunchKakaoMap(place))
                intent.kakaoNaviInstalled -> launchPractice(place, HomeEffect.LaunchKakaoNavi(place))
                else -> _effect.send(HomeEffect.ShowInstallNaviPicker(place))
            }
        }
    }

    private fun onNaviAppSelected(intent: HomeIntent.OnNaviAppSelected) {
        val place = _state.value.selectedPlace ?: return
        viewModelScope.launch {
            if (intent.always) setNaviAlwaysUseCase(intent.app)
            when (intent.app) {
                NaviApp.KAKAOMAP -> launchPractice(place, HomeEffect.LaunchKakaoMap(place))
                NaviApp.KAKAONAVI -> launchPractice(place, HomeEffect.LaunchKakaoNavi(place))
            }
        }
    }

    private fun onInstallNaviAppSelected(intent: HomeIntent.OnInstallNaviAppSelected) {
        viewModelScope.launch { _effect.send(HomeEffect.OpenNaviInstallPage(intent.app)) }
    }

    private suspend fun launchPractice(place: PlaceDetail, effect: HomeEffect) {
        if (place.type == PlaceType.COURSE) {
            startPracticeSessionUseCase(place.id, place.name)
                .onFailure { _effect.send(HomeEffect.ShowSnackbar(it.userMessage())) }
                .onSuccess { _effect.send(effect) }
            return
        }
        _effect.send(effect)
    }

    private fun loadCompletedPracticeSession() {
        viewModelScope.launch {
            if (!isLoggedIn()) return@launch
            getCompletedPracticeSessionUseCase().onSuccess { session ->
                _state.update { it.copy(practicePrompt = session) }
            }
        }
    }

    private fun clearPracticePrompt() {
        _state.update { it.copy(practicePrompt = null) }
        viewModelScope.launch {
            clearPracticeSessionUseCase().onFailure { _effect.send(HomeEffect.ShowSnackbar(it.userMessage())) }
        }
    }

    private suspend fun isLoggedIn(): Boolean = try {
        getAuthSessionUseCase().isLoggedIn
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        false
    }
}

internal data class PlaceRequestKey(
    val query: PlaceViewportQuery,
    val cursor: String?,
)
