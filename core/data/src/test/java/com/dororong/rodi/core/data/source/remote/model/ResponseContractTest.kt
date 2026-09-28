package com.dororong.rodi.core.data.source.remote.model

import com.dororong.rodi.core.data.di.NetworkModule
import com.dororong.rodi.core.data.mapper.toDomain
import com.dororong.rodi.core.data.source.remote.model.member.CursorPagePracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.MyPageResponse
import com.dororong.rodi.core.data.source.remote.model.member.PracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.place.CursorPagePlaceResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceDetailResponse
import com.dororong.rodi.core.data.source.remote.model.practice.FormResponse
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeRegisterResponse
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeVisitResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReviewSummaryResponse
import com.dororong.rodi.core.data.source.remote.model.search.RecentSearchResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 서버가 필수 필드를 빠뜨리면 빈 문자열·0·false·빈 목록으로 채우지 않고 파싱에 실패해야 한다.
 * 설명에 생략·null 조건이 있는 필드는 빠져도 정상 처리한다. 운영과 같은 Json 설정을 쓴다.
 */
class ResponseContractTest {
    private val json = NetworkModule.provideJson()

    @Test
    fun `마이페이지 응답은 모든 필드와 비어 있는 운전 목표를 파싱한다`() {
        val page = decode<MyPageResponse>(MY_PAGE)

        assertEquals("로디", page.nickname)
        assertNull(page.drivingGoal)
        assertNull(page.levelProgress.nextLevelKm)
    }

    @Test
    fun `마이페이지 응답에 닉네임이나 레벨이나 개수나 진행도가 없으면 파싱에 실패한다`() {
        listOf("nickname", "level", "savedPlaceCount", "recommendationTags", "levelProgress").forEach { field ->
            assertThrows<SerializationException>(field) { decode<MyPageResponse>(MY_PAGE.without(field)) }
        }
    }

    @Test
    fun `레벨 진행도에 누적 거리가 없으면 파싱에 실패한다`() {
        assertThrows<SerializationException> { decode<MyPageResponse>(MY_PAGE.replace("\"totalDistanceKm\":12.5,", "")) }
    }

    @Test
    fun `후기 요약에 헤더를 그리는 개수가 없으면 파싱에 실패한다`() {
        listOf("totalReviewCount", "levelReviewCount", "recommendCount", "notRecommendCount", "difficultyCounts", "levelCounts")
            .forEach { field ->
                assertThrows<SerializationException>(field) { decode<ReviewSummaryResponse>(SUMMARY.without(field)) }
            }
    }

    @Test
    fun `후기 요약은 최다 난이도가 생략돼도 파싱한다`() {
        val summary = decode<ReviewSummaryResponse>(SUMMARY)

        assertEquals(3L, summary.totalReviewCount)
    }

    @Test
    fun `커서 페이지에 목록이나 다음 여부가 없으면 목록 끝으로 보지 않고 파싱에 실패한다`() {
        listOf("items", "hasNext").forEach { field ->
            assertThrows<SerializationException>(field) {
                decode<CursorPagePracticeItemResponse>(PRACTICE_PAGE.without(field))
            }
        }
    }

    @Test
    fun `응답 모델은 서버가 보내는 필드만 선언한다`() {
        assertAll(
            { assertDeclaresOnlyServerFields<PracticeItemResponse>(PRACTICE_PAGE.firstItem()) },
            { assertDeclaresOnlyServerFields<PracticeVisitResponse>(PRACTICE_VISIT_LEVEL_UP) },
            { assertDeclaresOnlyServerFields<ReviewSummaryResponse>(SUMMARY) },
            { assertDeclaresOnlyServerFields<RecentSearchResponse>(RECENT_SEARCH) },
        )
    }

    @Test
    fun `장소 상세에 북마크 상태나 연습 유형이 없으면 파싱에 실패한다`() {
        listOf("bookmarkCount", "isBookmarked", "practiceTypes").forEach { field ->
            assertThrows<SerializationException>(field) { decode<PlaceDetailResponse>(PLACE_DETAIL.without(field)) }
        }
    }

    @Test
    fun `연습 등록 응답에 id가 없으면 0번 연습으로 등록하지 않고 파싱에 실패한다`() {
        listOf("practiceId", "status", "visitCount", "requiredDistanceMeters").forEach { field ->
            assertThrows<SerializationException>(field) {
                decode<PracticeRegisterResponse>(PRACTICE_REGISTER.without(field))
            }
        }
    }

    @Test
    fun `연습 방문 응답은 결과 값이 없으면 실패하지만 새 레벨은 생략할 수 있다`() {
        val visit = decode<PracticeVisitResponse>(PRACTICE_VISIT)
        assertNull(visit.newLevel)

        listOf("isCertifiedNow", "levelUp", "visitCount", "totalDistanceKm").forEach { field ->
            assertThrows<SerializationException>(field) { decode<PracticeVisitResponse>(PRACTICE_VISIT.without(field)) }
        }
    }

    @Test
    fun `미방문 사유 폼에 문항이나 선택지 필드가 없으면 파싱에 실패한다`() {
        assertThrows<SerializationException> { decode<FormResponse>(FORM.without("options")) }
        assertThrows<SerializationException> { decode<FormResponse>(FORM.replace("\"code\":\"NO_TIME\",", "")) }
    }

    @Test
    fun `실제 장소 목록 응답은 모르는 필드를 무시하고 파싱한다`() {
        val page = decode<CursorPagePlaceResponse>(LIVE_PLACE_PAGE)

        assertEquals(2, page.items.size)
        assertFalse(page.hasNext)
        assertNull(page.items.first { it.type == "COURSE" }.capacity)
    }

    @Test
    fun `저장 목록의 삭제 여부는 true 값을 유지하고 누락되면 파싱에 실패한다`() {
        val saved = LIVE_PLACE_PAGE.replaceFirst("\"isDeleted\":false", "\"isDeleted\":true")

        assertTrue(decode<CursorPagePlaceResponse>(saved).toDomain().items.first().isDeleted)
        assertThrows<SerializationException> {
            decode<CursorPagePlaceResponse>(LIVE_PLACE_PAGE.replaceFirst(",\"isDeleted\":false", ""))
        }
    }

    private inline fun <reified T> decode(data: String): T =
        requireNotNull(json.decodeFromString<ApiEnvelope<T>>(envelope(data)).data)

    private fun envelope(data: String) =
        """{"isSuccess":true,"code":"COMMON_200","message":"요청에 성공했습니다.","data":$data}"""

    // 서버가 보내지 않는 필드를 선언하면 기본값이 늘 정상값처럼 채워진다.
    private inline fun <reified T> assertDeclaresOnlyServerFields(serverResponse: String) {
        val serverFields = json.parseToJsonElement(serverResponse).jsonObject.keys
        val descriptor = serializer<T>().descriptor
        val declared = (0 until descriptor.elementsCount).map(descriptor::getElementName).toSet()
        assertEquals(emptySet<String>(), declared - serverFields, T::class.simpleName)
    }

    private fun String.firstItem(): String =
        json.parseToJsonElement(this).jsonObject.getValue("items").jsonArray.first().toString()

    private fun String.without(field: String): String {
        val fields = json.parseToJsonElement(this).jsonObject
        check(field in fields) { "$field not found" }
        return JsonObject(fields - field).toString()
    }

    private companion object {
        const val MY_PAGE = """{"nickname":"로디","level":"SEED","recommendationTags":["PARKING"],""" +
            """"drivingGoal":null,"savedPlaceCount":2,""" +
            """"levelProgress":{"totalDistanceKm":12.5,"currentLevelStartKm":0.0,"progressPercent":40}}"""
        const val SUMMARY = """{"level":"ALL","levelReviewCount":3,"totalReviewCount":3,"recommendCount":2,""" +
            """"notRecommendCount":1,"difficultyCounts":{"EASY":1},"levelCounts":{"SEED":3}}"""
        const val PRACTICE_PAGE = """{"items":[{"practiceId":1,"placeId":2,"placeName":"코스","practiceTypes":["PARKING"],""" +
            """"status":"VISITED","visitCount":1,"lastActivityAt":null,"hasReview":false,"isDeleted":false}],""" +
            """"hasNext":false,"nextCursor":null,"totalCount":1}"""
        const val PLACE_DETAIL = """{"id":1,"type":"PARKING","name":"주차장","address":"서울","lat":37.5,"lng":127.0,""" +
            """"practiceTypes":["PARKING"],"bookmarkCount":0,"isBookmarked":false,"course":null,"parking":null}"""
        const val PRACTICE_REGISTER = """{"practiceId":7,"status":"PLANNED","visitCount":0,"requiredDistanceMeters":0}"""
        const val PRACTICE_VISIT = """{"visitCount":1,"addedCertifiedDistanceMeters":0,"requiredDistanceMeters":0,""" +
            """"isCertifiedNow":true,"totalDistanceKm":3.0,"levelUp":false}"""
        const val PRACTICE_VISIT_LEVEL_UP = """{"visitCount":1,"addedCertifiedDistanceMeters":1200,""" +
            """"requiredDistanceMeters":1000,"isCertifiedNow":true,"totalDistanceKm":13.0,"levelUp":true,"newLevel":"ROOKIE"}"""
        const val RECENT_SEARCH = """{"id":1,"type":"REGION","keyword":"서울 중구","placeId":null}"""
        const val FORM = """{"questionId":"SKIP_REASON","type":"SINGLE_SELECT","title":"왜 못 갔나요?","required":true,""" +
            """"options":[{"code":"NO_TIME","label":"시간이 없었어요","order":1,"requiresTextInput":false}]}"""

        // 2026-09-26 인증 없이 호출한 GET /places 응답의 키 구성과 null 패턴을 따른다(코스는 capacity·openTime,
        // 주차장은 description·distanceMeters가 null로 온다). 값은 줄였다.
        const val LIVE_PLACE_PAGE = """{"items":[""" +
            """{"id":10,"type":"COURSE","name":"연습 코스","address":"서울","lat":37.5,"lng":127.0,"distanceFromMe":120,""" +
            """"practiceTypes":["STRAIGHT"],"description":"설명","distanceMeters":1200,"capacity":null,"openTime":null,"isDeleted":false},""" +
            """{"id":11,"type":"PARKING","name":"공영 주차장","address":"서울","lat":37.5,"lng":127.0,"distanceFromMe":300,""" +
            """"practiceTypes":["PARKING"],"description":null,"distanceMeters":null,"capacity":40,"openTime":"09:00","isDeleted":false}""" +
            """],"hasNext":false,"nextCursor":null,"totalCount":2}"""
    }
}
