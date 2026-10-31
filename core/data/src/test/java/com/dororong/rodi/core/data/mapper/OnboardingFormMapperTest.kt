package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.api.OnboardingApi
import com.dororong.rodi.core.data.source.remote.model.onboarding.OnboardingFormResponse
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.http.GET

class OnboardingFormMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `서버 폼의 문구와 정렬 순서와 선택 규칙을 보존한다`() {
        val form = json.decodeFromString<OnboardingFormResponse>(FORM_JSON).toDomain()
        assertEquals(listOf(1, 2), form.steps.map { it.order })
        val first = form.steps.first()
        assertEquals("서버 경력 제목", first.title)
        assertEquals("서버 설명", first.description)
        assertFalse(first.skippable)
        assertEquals(listOf("drivingPeriod", "roadExperiences", "soloDrivingRange"), first.questions.map { it.code })
        val question = first.questions.first()
        assertTrue(question.required)
        assertEquals(listOf("MONTHS_1_2", "YEARS_3_9"), question.options.map { it.code })
        assertEquals("새 기간 라벨", question.options.first().label)
        assertTrue(question.options.last().skipsFollowUps)
        assertTrue(first.questions[1].options.first().exclusive)
        assertEquals(listOf("SOLO"), first.questions.last().showWhen?.anyOf)
        assertEquals("roadExperiences", first.questions.last().showWhen?.question)
        val second = form.steps.last()
        assertTrue(second.skippable)
        assertEquals(2, second.questions.first().maxSelect)
        assertEquals("순서를 정해주세요", second.questions.first().description)
        assertEquals(12, second.questions.last().maxLength)
        assertEquals("서버 입력 안내", second.questions.last().placeholder)
    }

    @Test
    fun `필수 필드가 누락되거나 알 수 없는 문항이면 폼을 거부한다`() {
        assertThrows<IllegalArgumentException> {
            json.decodeFromString<OnboardingFormResponse>(FORM_JSON.replace("drivingPeriod", "unknownQuestion")).toDomain()
        }
        assertThrows<kotlinx.serialization.SerializationException> {
            json.decodeFromString<OnboardingFormResponse>(FORM_JSON.replace("\"skippable\":false,", ""))
        }
    }

    @Test
    fun `알 수 없는 선택지나 질문 유형을 정상값으로 바꾸지 않는다`() {
        assertThrows<IllegalArgumentException> {
            json.decodeFromString<OnboardingFormResponse>(FORM_JSON.replace("MONTHS_1_2", "UNKNOWN_PERIOD")).toDomain()
        }
        assertThrows<IllegalArgumentException> {
            json.decodeFromString<OnboardingFormResponse>(FORM_JSON.replace("SINGLE_SELECT", "UNKNOWN_TYPE")).toDomain()
        }
    }

    @Test
    fun `온보딩 폼 조회는 전용 인증 API 경로를 사용한다`() {
        val method = OnboardingApi::class.java.methods.single { it.name == "getForm" }
        assertEquals("members/me/onboarding-form", method.getAnnotation(GET::class.java).value)
    }

    private companion object {
        val FORM_JSON = """
            {"steps":[
              {"order":2,"skippable":true,"title":"서버 선호 제목","description":"서버 선택 설명","questions":[
                {"code":"drivingGoal","order":8,"type":"TEXT","required":false,"title":"목표","maxLength":12,"placeholder":"서버 입력 안내"},
                {"code":"practiceTypes","order":6,"type":"RANKED_SELECT","required":false,"title":"서버 순위","description":"순서를 정해주세요","maxSelect":2,"options":[
                  {"code":"PARKING","label":"주차하기","order":1},{"code":"LEFT_RIGHT_TURN","label":"회전하기","order":2}]}]},
              {"order":1,"skippable":false,"title":"서버 경력 제목","description":"서버 설명","questions":[
                {"code":"soloDrivingRange","order":4,"type":"SINGLE_SELECT","required":false,"title":"혼자 운전","showWhen":{"question":"roadExperiences","anyOf":["SOLO"]},"options":[{"code":"NEAR_HOME","label":"집 주변","order":1}]},
                {"code":"roadExperiences","order":3,"type":"MULTI_SELECT","required":false,"title":"도로 경험","options":[{"code":"NONE","label":"경험 없음","order":1,"exclusive":true},{"code":"SOLO","label":"혼자 운전","order":2}]},
                {"code":"drivingPeriod","order":1,"type":"SINGLE_SELECT","required":true,"title":"운전 기간","options":[{"code":"YEARS_3_9","label":"숙련","order":2,"skipsFollowUps":true},{"code":"MONTHS_1_2","label":"새 기간 라벨","order":1}]}]}
            ]}
        """.trimIndent()
    }
}
