package com.dororong.rodi.feature.course.registration

import com.dororong.rodi.core.domain.model.course.CourseDraft
import com.dororong.rodi.core.domain.model.course.CourseLocationSearchResult
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.model.course.CourseRegistrationForm
import com.dororong.rodi.core.domain.model.course.CourseRegistrationResult
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypointType
import com.dororong.rodi.core.domain.model.course.RouteResult

enum class CourseRegistrationPage {
    Tutorial,
    Map,
    Form,
}

enum class CourseTutorialLoadState {
    Loading,
    Ready,
    Completing,
    Error,
}

enum class CourseMapLoadState {
    Loading,
    Ready,
    Error,
}

enum class CourseRegistrationFormLoadState {
    Loading,
    Ready,
    Error,
}

enum class CourseWaypointRole {
    Start,
    Via,
    Destination,
}

enum class CourseRegistrationDialog {
    ResumeDraft,
    Exit,
    Success,
}

data class CourseRegistrationUiState(
    val isAuthResolved: Boolean = false,
    val isLoggedIn: Boolean = false,
    val tutorialCompleted: Boolean = false,
    val page: CourseRegistrationPage = CourseRegistrationPage.Tutorial,
    val tutorialLoadState: CourseTutorialLoadState = CourseTutorialLoadState.Loading,
    val tutorialPage: Int = 0,
    val mapLoadState: CourseMapLoadState = CourseMapLoadState.Loading,
    val mapRetryToken: Int = 0,
    val selectedWaypointRole: CourseWaypointRole = CourseWaypointRole.Start,
    val waypoints: List<RegistrationWaypoint> = emptyList(),
    val editingWaypointIndex: Int? = null,
    val temporaryPin: GeoPoint? = null,
    val route: RouteResult? = null,
    val isRouteLoading: Boolean = false,
    val mapCenter: GeoPoint? = null,
    val mapCenterGeneration: Long = 0L,
    val searchKeyword: String = "",
    val isSearchVisible: Boolean = false,
    val isSearchLoading: Boolean = false,
    val isRecentSearchLoading: Boolean = true,
    val searchResult: CourseLocationSearchResult = CourseLocationSearchResult(),
    val searchError: String? = null,
    val formLoadState: CourseRegistrationFormLoadState = CourseRegistrationFormLoadState.Loading,
    val registrationForm: CourseRegistrationForm? = null,
    val selectedPracticeTypeCodes: List<String> = emptyList(),
    val caution: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val hasAttemptedSubmit: Boolean = false,
    val registrationResult: CourseRegistrationResult? = null,
    val submissionError: String? = null,
    val draft: CourseDraft? = null,
    val isDraftRestored: Boolean = false,
    val dialog: CourseRegistrationDialog? = null,
    val snackbarMessage: String? = null,
    val isMapPointLoading: Boolean = false,
    val pendingSuggestion: CourseLocationSuggestion? = null,
    val isPendingAddressLoading: Boolean = false,
) {
    val start: RegistrationWaypoint?
        get() = waypoints.firstOrNull { it.type == RegistrationWaypointType.START }

    val destination: RegistrationWaypoint?
        get() = waypoints.firstOrNull { it.type == RegistrationWaypointType.DESTINATION }

    val vias: List<RegistrationWaypoint>
        get() = waypoints.filter { it.type == RegistrationWaypointType.VIA }

    val isRouteReady: Boolean
        get() = route?.isRealRoute == true && route.totalDistanceMeters > 0 &&
            waypoints.size >= 2 && start != null && destination != null &&
            route.snappedPoints.size == waypoints.size &&
            waypoints.all { it.name.isNotBlank() && it.address.isNotBlank() }

    val canFinishMap: Boolean
        get() = mapLoadState == CourseMapLoadState.Ready &&
            !isRouteLoading && !isMapPointLoading && !isSearchVisible && editingWaypointIndex == null && isRouteReady

    /**
     * 완료 버튼 활성화 조건. 한줄 소개는 **1자 이상**이면 충족이고, 등록에 필요한 10자 조건은
     * 여기서 막지 않는다 — 10자 미만으로 완료를 누르면 버튼이 눌린 뒤 안내 문구를 띄우는 게 명세다.
     * 주의사항은 선택 입력이라 활성화 조건에 넣지 않는다.
     */
    val canSubmit: Boolean
        get() {
            val form = registrationForm ?: return false
            val availablePracticeTypes = form.categories.flatMap { it.practiceTypes }.map { it.code }.toSet()
            return !isSubmitting && isRouteReady && selectedPracticeTypeCodes.isNotEmpty() &&
                selectedPracticeTypeCodes.distinct().size == selectedPracticeTypeCodes.size &&
                selectedPracticeTypeCodes.all(availablePracticeTypes::contains) &&
                selectedPracticeTypeCodes.size <= form.practiceTypeMaxSelect &&
                caution.length <= form.cautionInput.maxLength &&
                description.isNotBlank()
        }

    /** 등록 요청을 보낼 수 있는지 — 한줄 소개는 공백을 제외하고 10자 이상이어야 한다. */
    val descriptionMeetsRegisterLength: Boolean
        get() {
            val minLength = registrationForm?.descriptionInput?.minLength ?: return true
            return description.count { !it.isWhitespace() } >= minLength
        }

    val isDraftMeaningful: Boolean
        get() = draft?.isMeaningful == true || waypoints.isNotEmpty() ||
            selectedPracticeTypeCodes.isNotEmpty() || caution.isNotBlank() || description.isNotBlank()
}

sealed interface CourseRegistrationIntent {
    data object Retry : CourseRegistrationIntent
    data class TutorialPageChanged(val page: Int) : CourseRegistrationIntent
    data object CompleteTutorial : CourseRegistrationIntent
    data object ContinueDraft : CourseRegistrationIntent
    data object DiscardDraft : CourseRegistrationIntent
    data object Back : CourseRegistrationIntent
    data object RequestExit : CourseRegistrationIntent
    data object ConfirmExit : CourseRegistrationIntent
    data object DismissDialog : CourseRegistrationIntent
    data class SelectWaypointRole(val role: CourseWaypointRole) : CourseRegistrationIntent
    data class SelectWaypoint(val point: GeoPoint, val name: String, val address: String, val jibunAddress: String?) :
        CourseRegistrationIntent
    data class RemoveWaypoint(val index: Int) : CourseRegistrationIntent
    data class BeginPinEdit(val index: Int) : CourseRegistrationIntent
    data class MoveTemporaryPin(val point: GeoPoint) : CourseRegistrationIntent
    data class MapCenterChanged(val point: GeoPoint) : CourseRegistrationIntent
    data class MapPointSelected(val point: GeoPoint) : CourseRegistrationIntent
    data class CurrentLocationSelected(val point: GeoPoint) : CourseRegistrationIntent
    data object LocationUnavailable : CourseRegistrationIntent
    data object CommitPinEdit : CourseRegistrationIntent
    data object DiscardPinEdit : CourseRegistrationIntent
    data object ResetPinEdit : CourseRegistrationIntent
    data class SearchVisibilityChanged(val visible: Boolean) : CourseRegistrationIntent
    data class SearchKeywordChanged(val keyword: String) : CourseRegistrationIntent
    data object SearchSubmitted : CourseRegistrationIntent
    data class SearchSuggestionSelected(val id: String) : CourseRegistrationIntent
    data class DeleteRecentSearch(val id: String) : CourseRegistrationIntent
    data object DeleteAllRecentSearches : CourseRegistrationIntent
    data class MapReady(val ready: Boolean) : CourseRegistrationIntent
    data class TogglePracticeType(val code: String) : CourseRegistrationIntent
    data class CautionChanged(val value: String) : CourseRegistrationIntent
    data class DescriptionChanged(val value: String) : CourseRegistrationIntent
    data object ContinueToForm : CourseRegistrationIntent
    data object Submit : CourseRegistrationIntent
    data object SuccessConfirmed : CourseRegistrationIntent
}

sealed interface CourseRegistrationEffect {
    data object LoginRequired : CourseRegistrationEffect
    data object Exit : CourseRegistrationEffect
    data object Completed : CourseRegistrationEffect
    data class ShowSnackbar(val message: String) : CourseRegistrationEffect
}

sealed interface CourseRegistrationRoute {
    data object Tutorial : CourseRegistrationRoute
    data object Map : CourseRegistrationRoute
    data object Form : CourseRegistrationRoute
}
