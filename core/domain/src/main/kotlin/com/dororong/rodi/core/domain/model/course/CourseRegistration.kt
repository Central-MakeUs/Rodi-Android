package com.dororong.rodi.core.domain.model.course

import java.time.Instant

enum class RegistrationWaypointType {
    START,
    VIA,
    DESTINATION,
}

data class RegistrationWaypoint(
    val type: RegistrationWaypointType,
    val name: String,
    val address: String,
    val jibunAddress: String? = null,
    val lat: Double,
    val lng: Double,
)

data class CourseInputSpec(
    val required: Boolean,
    val minLength: Int? = null,
    val maxLength: Int,
    val placeholder: String,
)

data class CourseRegistrationSections(
    val basicInfo: String,
    val practiceCategory: String,
    val practiceType: String,
    val caution: String,
    val description: String,
)

data class CoursePracticeType(
    val code: String,
    val label: String,
    val order: Int,
)

data class CoursePracticeCategory(
    val code: String,
    val label: String,
    val order: Int,
    val practiceTypes: List<CoursePracticeType>,
)

data class CourseRegistrationForm(
    val maxWaypoints: Int,
    val sections: CourseRegistrationSections,
    val practiceTypeMaxSelect: Int,
    val practiceTypeMaxSelectExceededMessage: String,
    val categories: List<CoursePracticeCategory>,
    val cautionInput: CourseInputSpec,
    val descriptionInput: CourseInputSpec,
)

data class CourseDraft(
    val waypoints: List<RegistrationWaypoint> = emptyList(),
    val selectedPracticeTypeCodes: List<String> = emptyList(),
    val caution: String = "",
    val description: String = "",
) {
    val isMeaningful: Boolean
        get() = waypoints.isNotEmpty() || selectedPracticeTypeCodes.isNotEmpty() ||
            caution.isNotBlank() || description.isNotBlank()
}

data class CourseRegistrationRequest(
    val address: String,
    val distanceMeters: Int,
    val waypoints: List<RegistrationWaypoint>,
    val practiceTypes: List<String>,
    val description: String,
    val caution: String,
)

fun CourseRegistrationRequest.validateForSubmission() {
    require(address.isNotBlank()) { "출발지 주소가 필요합니다." }
    require(distanceMeters > 0) { "실제 주행거리가 필요합니다." }
    require(waypoints.size >= 2) { "출발지와 도착지가 필요합니다." }
    require(waypoints.first().type == RegistrationWaypointType.START) { "출발지가 필요합니다." }
    require(waypoints.last().type == RegistrationWaypointType.DESTINATION) { "도착지가 필요합니다." }
    require(waypoints.drop(1).dropLast(1).all { it.type == RegistrationWaypointType.VIA }) {
        "경유지 순서가 올바르지 않습니다."
    }
    require(waypoints.all { it.name.isNotBlank() && it.address.isNotBlank() }) { "모든 위치 정보가 필요합니다." }
    require(practiceTypes.isNotEmpty() && practiceTypes.distinct().size == practiceTypes.size) {
        "연습 유형은 한 개 이상 중복 없이 선택해야 합니다."
    }
}

data class CourseRegistrationResult(
    val courseId: Long,
    val approvalStatus: CourseApprovalStatus,
)

enum class CourseApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
}

data class RegisteredCourse(
    val courseId: Long,
    val name: String,
    val approvalStatus: CourseApprovalStatus,
    val createdAt: Instant,
)

data class CourseLocationSuggestion(
    val id: String,
    val title: String,
    val address: String,
    val point: GeoPoint?,
    val kind: CourseLocationKind,
    val lastUsedAt: Instant = Instant.EPOCH,
    val source: CourseLocationSuggestionSource = CourseLocationSuggestionSource.KAKAO_KEYWORD,
    val resolution: CourseLocationResolution = CourseLocationResolution.RESOLVED,
    val sourceId: Long? = null,
)

enum class CourseLocationKind {
    REGION,
    PLACE,
}

enum class CourseLocationSuggestionSource {
    SERVER_REGION,
    SERVER_PLACE,
    KAKAO_ADDRESS,
    KAKAO_KEYWORD,
    REVERSE_GEOCODE,
    HISTORY,
}

enum class CourseLocationResolution {
    RESOLVED,
    REQUIRES_REGION_CENTER,
    REQUIRES_PLACE_DETAIL,
}

data class CourseLocationSearchResult(
    val recent: List<CourseLocationSuggestion> = emptyList(),
    val regions: List<CourseLocationSuggestion> = emptyList(),
    val places: List<CourseLocationSuggestion> = emptyList(),
    val isPartial: Boolean = false,
)
