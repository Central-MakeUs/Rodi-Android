package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.course.CourseInputSpecResponse
import com.dororong.rodi.core.data.source.remote.model.course.CoursePageResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegisterRequest
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegisterResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationFormResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationInputsResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseRegistrationSectionsResponse
import com.dororong.rodi.core.data.source.remote.model.course.CourseWaypointRequest
import com.dororong.rodi.core.data.source.remote.model.course.MyCourseItemResponse
import com.dororong.rodi.core.data.source.remote.model.course.PracticeTypeCategoryResponse
import com.dororong.rodi.core.data.source.remote.model.course.PracticeTypeFormResponse
import com.dororong.rodi.core.data.source.remote.model.course.PracticeTypeItemResponse
import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.domain.model.course.CourseInputSpec
import com.dororong.rodi.core.domain.model.course.CoursePracticeCategory
import com.dororong.rodi.core.domain.model.course.CoursePracticeType
import com.dororong.rodi.core.domain.model.course.CourseRegistrationForm
import com.dororong.rodi.core.domain.model.course.CourseRegistrationRequest
import com.dororong.rodi.core.domain.model.course.CourseRegistrationResult
import com.dororong.rodi.core.domain.model.course.CourseRegistrationSections
import com.dororong.rodi.core.domain.model.course.RegisteredCourse
import com.dororong.rodi.core.domain.model.course.RegistrationWaypoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypointType
import com.dororong.rodi.core.domain.model.place.CursorPage

fun CourseRegistrationFormResponse.toDomain() = CourseRegistrationForm(
    maxWaypoints = maxWaypoints,
    sections = sections.toDomain(),
    practiceTypeMaxSelect = practiceType.maxSelect,
    practiceTypeMaxSelectExceededMessage = practiceType.maxSelectExceededMessage,
    categories = practiceType.categories.sortedBy(PracticeTypeCategoryResponse::order).map { it.toDomain() },
    cautionInput = inputs.caution.toDomain(),
    descriptionInput = inputs.description.toDomain(),
)

private fun CourseRegistrationSectionsResponse.toDomain() = CourseRegistrationSections(
    basicInfo = basicInfo,
    practiceCategory = practiceCategory,
    practiceType = practiceType,
    caution = caution,
    description = description,
)

private fun PracticeTypeCategoryResponse.toDomain() = CoursePracticeCategory(
    code = code,
    label = label,
    order = order,
    practiceTypes = practiceTypes.sortedBy(PracticeTypeItemResponse::order).map(PracticeTypeItemResponse::toDomain),
)

private fun PracticeTypeItemResponse.toDomain() = CoursePracticeType(
    code = code,
    label = label,
    order = order,
)

private fun CourseInputSpecResponse.toDomain() = CourseInputSpec(
    required = required,
    minLength = minLength,
    maxLength = maxLength,
    placeholder = placeholder,
)

fun CourseRegistrationRequest.toData() = CourseRegisterRequest(
    address = address,
    distanceMeters = distanceMeters,
    waypoints = waypoints.map(RegistrationWaypoint::toData),
    practiceTypes = practiceTypes,
    description = description,
    caution = caution,
)

private fun RegistrationWaypoint.toData() = CourseWaypointRequest(
    type = when (type) {
        RegistrationWaypointType.START -> "START"
        RegistrationWaypointType.VIA -> "VIA"
        RegistrationWaypointType.DESTINATION -> "DESTINATION"
    },
    lat = lat,
    lng = lng,
    name = name,
)

fun CourseRegisterResponse.toDomain() = CourseRegistrationResult(
    courseId = courseId,
    approvalStatus = approvalStatus.toApprovalStatus(),
)

fun CoursePageResponse.toDomain() = CursorPage(
    items = items.map(MyCourseItemResponse::toDomain),
    hasNext = hasNext,
    nextCursor = nextCursor,
    totalCount = totalCount,
)

private fun MyCourseItemResponse.toDomain() = RegisteredCourse(
    courseId = courseId,
    name = name,
    approvalStatus = approvalStatus.toApprovalStatus(),
    createdAt = parseServerTimestamp(createdAt),
)

fun String.toApprovalStatus(): CourseApprovalStatus = when (this) {
    "PENDING" -> CourseApprovalStatus.PENDING
    "APPROVED" -> CourseApprovalStatus.APPROVED
    "REJECTED" -> CourseApprovalStatus.REJECTED
    else -> error("지원하지 않는 코스 승인 상태: $this")
}
