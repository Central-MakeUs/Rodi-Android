package com.dororong.rodi.core.data.source.remote.model.course

import kotlinx.serialization.Serializable

@Serializable
data class CourseRegistrationFormResponse(
    val maxWaypoints: Int,
    val sections: CourseRegistrationSectionsResponse,
    val practiceType: PracticeTypeFormResponse,
    val inputs: CourseRegistrationInputsResponse,
)

@Serializable
data class CourseRegistrationSectionsResponse(
    val basicInfo: String,
    val practiceCategory: String,
    val practiceType: String,
    val caution: String,
    val description: String,
)

@Serializable
data class PracticeTypeFormResponse(
    val maxSelect: Int,
    val maxSelectExceededMessage: String,
    val categories: List<PracticeTypeCategoryResponse>,
)

@Serializable
data class PracticeTypeCategoryResponse(
    val code: String,
    val label: String,
    val order: Int,
    val practiceTypes: List<PracticeTypeItemResponse>,
)

@Serializable
data class PracticeTypeItemResponse(
    val code: String,
    val label: String,
    val order: Int,
)

@Serializable
data class CourseRegistrationInputsResponse(
    val caution: CourseInputSpecResponse,
    val description: CourseInputSpecResponse,
)

@Serializable
data class CourseInputSpecResponse(
    val required: Boolean,
    val minLength: Int? = null,
    val maxLength: Int,
    val placeholder: String,
)

@Serializable
data class CourseRegisterRequest(
    val address: String,
    val distanceMeters: Int,
    val waypoints: List<CourseWaypointRequest>,
    val practiceTypes: List<String>,
    val description: String,
    val caution: String,
)

@Serializable
data class CourseWaypointRequest(
    val type: String,
    val lat: Double,
    val lng: Double,
    val name: String,
)

@Serializable
data class CourseRegisterResponse(
    val courseId: Long,
    val approvalStatus: String,
)

@Serializable
data class CoursePageResponse(
    val items: List<MyCourseItemResponse>,
    val hasNext: Boolean,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)

@Serializable
data class MyCourseItemResponse(
    val courseId: Long,
    val name: String,
    val approvalStatus: String,
    val createdAt: String,
)
