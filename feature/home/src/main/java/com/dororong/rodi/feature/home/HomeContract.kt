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
import com.dororong.rodi.core.domain.model.practice.ActivePracticeSession
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
    val isLevelReviewsVisible: Boolean = false,
    val practicePrompt: PracticeRecordItem? = null,
    val activePracticeSession: ActivePracticeSession? = null,
    val isPracticeContinueDialogVisible: Boolean = false,
    val isPracticeActionInProgress: Boolean = false,
    val isPracticeLaunchInProgress: Boolean = false,
    val isNotificationPermissionRationaleVisible: Boolean = false,
    val pendingPracticeNavigation: PendingPracticeNavigation? = null,
    val levelUp: OnboardingLevel? = null,
) {
    val showInitialError: Boolean get() = listState == HomeListState.InitialError
    val showEmpty: Boolean get() = listState == HomeListState.Empty
}

sealed interface HomeIntent {
    data object MapGestured : HomeIntent
    data class ViewportSettled(val query: PlaceViewportQuery) : HomeIntent
    data class ProgrammaticSearchRequested(val query: PlaceViewportQuery) : HomeIntent
    data class ResearchClicked(val query: PlaceViewportQuery) : HomeIntent
    data object ListOpenClicked : HomeIntent
    data object ListCollapseRequested : HomeIntent
    data class ListSheetSettled(val surface: HomeSurfaceState) : HomeIntent
    data object ListEndReached : HomeIntent
    data class PlaceClicked(val id: Long, val origin: HomeDetailOrigin) : HomeIntent
    data object DetailDismissed : HomeIntent
    data object DetailDragDismissed : HomeIntent
    data object LevelReviewsOpened : HomeIntent
    data object LevelReviewsClosed : HomeIntent
    data object ReviewUpdated : HomeIntent
    data object AppResumed : HomeIntent
    data object PracticeContinueClicked : HomeIntent
    data object PracticeStopClicked : HomeIntent
    data object PracticeVisitedAnswered : HomeIntent
    data object PracticeNotVisitedAnswered : HomeIntent
    data object PracticePromptDismissed : HomeIntent
    data object NotificationPermissionAllowClicked : HomeIntent
    data object NotificationPermissionRouteOnlyClicked : HomeIntent
    data class NotificationPermissionResultReceived(val granted: Boolean) : HomeIntent
    data object LevelUpDismissed : HomeIntent
    data object BookmarkClicked : HomeIntent
    data object MyPageClicked : HomeIntent
    data object RegisterClicked : HomeIntent
    data class SearchClicked(val origin: GeoPoint?) : HomeIntent
    data class RegionSearchRequested(
        val region: RegionOfficeLocation,
        val initialPlaces: List<PlaceSummary>,
    ) : HomeIntent
    data object FilterOpened : HomeIntent
    data class FilterCategorySelected(val category: FilterCategory) : HomeIntent
    data class FilterPracticeOptionToggled(val option: FilterPracticeOption) : HomeIntent
    data object FilterResetClicked : HomeIntent
    data object FilterApplyClicked : HomeIntent
    data object FilterDismissed : HomeIntent
    data object LoginDismissed : HomeIntent
    data class KakaoLoginSucceeded(val accessToken: String) : HomeIntent
    data class KakaoLoginFailed(val message: String) : HomeIntent
    data object AccountRestoreClicked : HomeIntent
    data object AccountRestoreDismissed : HomeIntent

    data class NavigateClicked(
        val kakaoMapInstalled: Boolean,
        val kakaoNaviInstalled: Boolean,
        val notificationPermissionGranted: Boolean,
    ) : HomeIntent

    data class NaviAppSelected(
        val app: NaviApp,
        val always: Boolean,
        val notificationPermissionGranted: Boolean,
    ) : HomeIntent
    data class NaviAppInstallSelected(val app: NaviApp) : HomeIntent
}

sealed interface HomeEffect {
    sealed interface LaunchNavi : HomeEffect {
        val place: PlaceDetail
        val startDriving: Boolean
        val app: NaviApp
    }
    data class LaunchKakaoMap(
        override val place: PlaceDetail,
        override val startDriving: Boolean = true,
    ) : LaunchNavi {
        override val app: NaviApp get() = NaviApp.KAKAOMAP
    }
    data class LaunchKakaoNavi(
        override val place: PlaceDetail,
        override val startDriving: Boolean = true,
    ) : LaunchNavi {
        override val app: NaviApp get() = NaviApp.KAKAONAVI
    }
    data class ShowNaviPicker(val place: PlaceDetail) : HomeEffect
    data class MoveToRegion(val region: RegionOfficeLocation) : HomeEffect
    data object RefreshReviews : HomeEffect
    data class ShowInstallNaviPicker(val place: PlaceDetail) : HomeEffect
    data class OpenPracticeReview(val placeId: Long, val placeName: String) : HomeEffect
    data class OpenPracticeSkipReason(val practiceId: Long) : HomeEffect
    data class OpenNaviInstallPage(val app: NaviApp) : HomeEffect
    data class ShowSnackbar(val message: String) : HomeEffect
    data class NavigateSearch(val origin: GeoPoint) : HomeEffect
    data object NavigateMyPage : HomeEffect
    data object NavigateCourseRegistration : HomeEffect
    data object NavigateGuestSignUp : HomeEffect
    data object StopDrivingTracking : HomeEffect
}

sealed interface HomePermissionEffect {
    data object RequestNotificationPermission : HomePermissionEffect
}

data class PendingPracticeNavigation(
    val place: PlaceDetail,
    val app: NaviApp,
)

sealed interface PendingHomeAction {
    data class OpenDetail(val placeId: Long, val origin: HomeDetailOrigin) : PendingHomeAction
    data object ToggleBookmark : PendingHomeAction
    data object OpenMyPage : PendingHomeAction
    data object OpenCourseRegistration : PendingHomeAction
    data class OpenSearch(val origin: GeoPoint) : PendingHomeAction
    data class SaveFilterTags(val filterTags: Set<PracticeType>) : PendingHomeAction
}
