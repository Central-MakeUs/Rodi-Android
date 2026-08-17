package com.dororong.rodi.core.domain.model.course

sealed class CourseRegistrationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class RouteUnavailable(cause: Throwable? = null) :
        CourseRegistrationException("실제 도로 경로를 확인하지 못했어요. 잠시 후 다시 시도해주세요.", cause)

    class AddressUnavailable(cause: Throwable? = null) :
        CourseRegistrationException("선택한 위치의 주소를 찾지 못했어요.", cause)

    class SearchUnavailable(cause: Throwable? = null) :
        CourseRegistrationException("검색 결과를 불러오지 못했어요.", cause)

    class SelectionUnavailable(cause: Throwable? = null) :
        CourseRegistrationException("선택한 검색 결과의 위치를 확인하지 못했어요.", cause)
}
