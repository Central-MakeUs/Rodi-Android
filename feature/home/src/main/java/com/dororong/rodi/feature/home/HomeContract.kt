package com.dororong.rodi.feature.home

import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.member.PracticeRecordItem
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.feature.home.filter.FilterCategory
import com.dororong.rodi.feature.home.filter.FilterPracticeOption
import com.dororong.rodi.feature.home.search.RegionOfficeLocation

enum class HomeSurfaceState {
    Navigation,
    PartialList,
    FullList,
    Detail,
}

enum class HomeDetailOrigin {
    Map,
    List,
}

enum class HomeListState {
    Idle,
    Loading,
    Content,
    Empty,
    InitialError,
}

data class HomeUiState(
    val coordinates: List<PlaceCoordinate> = emptyList(),
    val places: List<PlaceSummary> = emptyList(),
    val listState: HomeListState = HomeListState.Idle,
    val surfaceState: HomeSurfaceState = HomeSurfaceState.Navigation,
    val selectedPlaceId: Long? = null,
    val selectedPlace: PlaceDetail? = null,
    val selectedRoute: RouteResult? = null,
    val isRouting: Boolean = false,
    val detailOrigin: HomeDetailOrigin? = null,
    val isDetailLoading: Boolean = false,
    val isBookmarkUpdating: Boolean = false,
    val isNextPageLoading: Boolean = false,
    val hasNextPage: Boolean = false,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
    val searchedQuery: PlaceViewportQuery? = null,
    val placeListGeneration: Long = 0,
    val isMapSearchDirty: Boolean = false,
    val pendingAction: PendingHomeAction? = null,
    val isLoginInProgress: Boolean = false,
    val hasPendingRestore: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val isFilterSheetVisible: Boolean = false,
    val activeFilterCategory: FilterCategory? = FilterCategory.BASIC_DRIVING,
    val selectedFilterPracticeTypes: Set<PracticeType> = emptySet(),
    val isFilterSaving: Boolean = false,
    val searchKeyword: String? = null,
    val regionSearch: RegionOfficeLocation? = null,
    val regionSearchGeneration: Long = 0L,
    val isLevelReviewsVisible: Boolean = false,
    val practicePrompt: PracticeRecordItem? = null,
    val isPracticeSkipReasonVisible: Boolean = false,
    val notVisitedPracticeId: Long? = null,
    val isPracticeActionInProgress: Boolean = false,
    val levelUp: OnboardingLevel? = null,
) {
    val showInitialError: Boolean get() = listState == HomeListState.InitialError
    val showEmpty: Boolean get() = listState == HomeListState.Empty
}

sealed interface HomeIntent {
    data object OnMapGesture : HomeIntent
    data class OnViewportSettled(val query: PlaceViewportQuery) : HomeIntent
    data class OnProgrammaticSearch(val query: PlaceViewportQuery) : HomeIntent
    data class OnResearch(val query: PlaceViewportQuery) : HomeIntent
    data object OnListOpen : HomeIntent
    data object OnListExpand : HomeIntent
    data object OnListCollapse : HomeIntent
    data object OnLoadNextPage : HomeIntent
    data class OnPlaceClick(val id: Long, val origin: HomeDetailOrigin) : HomeIntent
    data object OnDismissDetail : HomeIntent
    data object OnDragDismissDetail : HomeIntent
    data object OnLevelReviewsOpen : HomeIntent
    data object OnLevelReviewsClose : HomeIntent
    data object OnAppResumed : HomeIntent
    data object OnPracticePromptVisited : HomeIntent
    data object OnPracticePromptNotVisited : HomeIntent
    data object OnPracticePromptDismiss : HomeIntent
    data object OnPracticeSkipReasonClosed : HomeIntent
    data object OnLevelUpDismiss : HomeIntent
    data object OnBookmarkClick : HomeIntent
    data object OnMyClick : HomeIntent
    data class OnSearchClick(val origin: GeoPoint?) : HomeIntent
    data class OnRegionSearch(
        val region: RegionOfficeLocation,
        val initialPlaces: List<PlaceSummary>,
    ) : HomeIntent
    data object OnFilterOpen : HomeIntent
    data class OnFilterCategorySelect(val category: FilterCategory) : HomeIntent
    data class OnFilterPracticeOptionToggle(val option: FilterPracticeOption) : HomeIntent
    data object OnFilterReset : HomeIntent
    data object OnFilterApply : HomeIntent
    data object OnFilterDismiss : HomeIntent
    data object OnDismissLogin : HomeIntent
    data class OnKakaoLoginCredential(val accessToken: String) : HomeIntent
    data class OnKakaoLoginFailed(val message: String) : HomeIntent
    data object OnRestoreAccount : HomeIntent
    data object OnDismissRestore : HomeIntent

    data class OnNavigateClick(
        val kakaoMapInstalled: Boolean,
        val kakaoNaviInstalled: Boolean,
    ) : HomeIntent

    data class OnNaviAppSelected(val app: NaviApp, val always: Boolean) : HomeIntent
    data class OnInstallNaviAppSelected(val app: NaviApp) : HomeIntent
}

sealed interface HomeEffect {
    data class LaunchKakaoMap(val place: PlaceDetail) : HomeEffect
    data class LaunchKakaoNavi(val place: PlaceDetail) : HomeEffect
    data class ShowNaviPicker(val place: PlaceDetail) : HomeEffect
    data class ShowInstallNaviPicker(val place: PlaceDetail) : HomeEffect
    data class OpenPracticeReview(val placeId: Long, val placeName: String) : HomeEffect
    data class OpenNaviInstallPage(val app: NaviApp) : HomeEffect
    data class ShowSnackbar(val message: String) : HomeEffect
    data class NavigateSearch(val origin: GeoPoint) : HomeEffect
    data object NavigateMyPage : HomeEffect
    data object NavigateGuestSignUp : HomeEffect
}

sealed interface PendingHomeAction {
    data class OpenDetail(val placeId: Long, val origin: HomeDetailOrigin) : PendingHomeAction
    data object ToggleBookmark : PendingHomeAction
    data object OpenMyPage : PendingHomeAction
    data class OpenSearch(val origin: GeoPoint) : PendingHomeAction
    data class SaveFilterTags(val filterTags: Set<PracticeType>) : PendingHomeAction
}
