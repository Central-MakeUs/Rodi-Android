package com.dororong.rodi.core.data.source.remote.model

import com.dororong.rodi.core.data.di.NetworkModule
import com.dororong.rodi.core.data.source.remote.model.member.CursorPagePracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.MyPageResponse
import com.dororong.rodi.core.data.source.remote.model.place.CursorPagePlaceResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceDetailResponse
import com.dororong.rodi.core.data.source.remote.model.practice.FormResponse
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeRegisterResponse
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeVisitResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReviewSummaryResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 서버가 필수 필드를 빠뜨리면 빈 문자열·0·false·빈 목록으로 채우지 않고 파싱에 실패해야 한다.
 * 설명에 생략·null 조건이 있는 필드는 빠져도 정상 처리한다. 운영과 같은 Json 설정을 쓴다.
 */
class ResponseContractTest {
    private val json = NetworkModule.provideJson()

    @Test
    fun `my page parses a complete response and a null driving goal`() {
        val page = decode<MyPageResponse>(MY_PAGE)

        assertEquals("로디", page.nickname)
        assertNull(page.drivingGoal)
        assertNull(page.levelProgress.nextLevelKm)
    }

    @Test
    fun `my page rejects a missing nickname, level, count, or progress`() {
        listOf("nickname", "level", "savedPlaceCount", "recommendationTags", "levelProgress").forEach { field ->
            assertThrows<SerializationException>(field) { decode<MyPageResponse>(MY_PAGE.without(field)) }
        }
    }

    @Test
    fun `level progress rejects missing distances but allows the top level to omit the next goal`() {
        assertThrows<SerializationException> { decode<MyPageResponse>(MY_PAGE.replace("\"totalDistanceKm\":12.5,", "")) }
    }

    @Test
    fun `review summary rejects missing counts that drive the review header`() {
        listOf("totalReviewCount", "levelReviewCount", "recommendCount", "notRecommendCount", "difficultyCounts", "levelCounts")
            .forEach { field ->
                assertThrows<SerializationException>(field) { decode<ReviewSummaryResponse>(SUMMARY.without(field)) }
            }
    }

    @Test
    fun `review summary tolerates the omitted top difficulty and unsent congestion counts`() {
        val summary = decode<ReviewSummaryResponse>(SUMMARY)

        assertEquals(3L, summary.totalReviewCount)
        assertEquals(emptyMap<String, Long>(), summary.congestionCounts)
    }

    @Test
    fun `cursor pages reject missing items or next flag instead of ending pagination`() {
        listOf("items", "hasNext").forEach { field ->
            assertThrows<SerializationException>(field) {
                decode<CursorPagePracticeItemResponse>(PRACTICE_PAGE.without(field))
            }
        }
    }

    @Test
    fun `practice records parse without the verification flag the server does not send`() {
        val page = decode<CursorPagePracticeItemResponse>(PRACTICE_PAGE)

        assertFalse(page.items.single().isVerified)
    }

    @Test
    fun `place detail rejects missing bookmark state and practice types`() {
        listOf("bookmarkCount", "isBookmarked", "practiceTypes").forEach { field ->
            assertThrows<SerializationException>(field) { decode<PlaceDetailResponse>(PLACE_DETAIL.without(field)) }
        }
    }

    @Test
    fun `practice registration rejects a missing id instead of registering practice zero`() {
        listOf("practiceId", "status", "visitCount", "requiredDistanceMeters").forEach { field ->
            assertThrows<SerializationException>(field) {
                decode<PracticeRegisterResponse>(PRACTICE_REGISTER.without(field))
            }
        }
    }

    @Test
    fun `practice visit rejects missing results but allows no new level`() {
        val visit = decode<PracticeVisitResponse>(PRACTICE_VISIT)
        assertNull(visit.newLevel)

        listOf("isCertifiedNow", "levelUp", "visitCount", "totalDistanceKm").forEach { field ->
            assertThrows<SerializationException>(field) { decode<PracticeVisitResponse>(PRACTICE_VISIT.without(field)) }
        }
    }

    @Test
    fun `skip reason form rejects missing question or option fields`() {
        assertThrows<SerializationException> { decode<FormResponse>(FORM.without("options")) }
        assertThrows<SerializationException> { decode<FormResponse>(FORM.replace("\"code\":\"NO_TIME\",", "")) }
    }

    @Test
    fun `live place list response still parses with unknown fields ignored`() {
        val page = decode<CursorPagePlaceResponse>(LIVE_PLACE_PAGE)

        assertEquals(2, page.items.size)
        assertFalse(page.hasNext)
        assertNull(page.items.first { it.type == "COURSE" }.capacity)
    }

    private inline fun <reified T> decode(data: String): T =
        requireNotNull(json.decodeFromString<ApiEnvelope<T>>(envelope(data)).data)

    private fun envelope(data: String) =
        """{"isSuccess":true,"code":"COMMON_200","message":"요청에 성공했습니다.","data":$data}"""

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
