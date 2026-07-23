package com.dororong.rodi.feature.home

import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery

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
    val isMapSearchDirty: Boolean = false,
    val pendingAction: PendingHomeAction? = null,
    val isLoginInProgress: Boolean = false,
    val hasPendingRestore: Boolean = false,
    val isRestoreInProgress: Boolean = false,
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
    data object OnBookmarkClick : HomeIntent
    data object OnMyClick : HomeIntent
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
    data class OpenNaviInstallPage(val app: NaviApp) : HomeEffect
    data class ShowSnackbar(val message: String) : HomeEffect
    data object NavigateMyPage : HomeEffect
}

sealed interface PendingHomeAction {
    data class OpenDetail(val placeId: Long, val origin: HomeDetailOrigin) : PendingHomeAction
    data object ToggleBookmark : PendingHomeAction
    data object OpenMyPage : PendingHomeAction
}
