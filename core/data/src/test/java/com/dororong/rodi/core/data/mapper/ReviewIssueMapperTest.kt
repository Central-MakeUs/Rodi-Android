package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.review.ReviewFormResponse
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReviewIssueMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `후기 폼의 코스 문제 선택지를 서버 순서와 입력 정책으로 매핑한다`() {
        val response = json.decodeFromString<ReviewFormResponse>(
            """{"steps":[{"questions":[{"code":"content"},{"code":"courseIssues","title":"문제 선택",
            "options":[{"code":"OTHER","label":"기타","order":2,"requiresTextInput":true,
            "textInputPlaceholder":"이유 입력","textInputMaxLength":30},
            {"code":"CONSTRUCTION","label":"공사 중","order":1}]}]}]}""",
        )
        val form = checkNotNull(response.toIssueForm())
        assertEquals("문제 선택", form.title)
        assertEquals(listOf("CONSTRUCTION", "OTHER"), form.options.map { it.code })
        assertTrue(form.options.last().requiresTextInput)
        assertEquals(30, form.options.last().textInputMaxLength)
    }

    @Test
    fun `코스 문제가 없는 후기 폼은 기존 후기 흐름으로 유지한다`() {
        val response = json.decodeFromString<ReviewFormResponse>(
            """{"steps":[{"questions":[{"code":"content"}]}]}""",
        )
        assertNull(response.toIssueForm())
    }

    @Test
    fun `코스 문제 질문이 중복된 폼은 잘못된 응답으로 처리한다`() {
        val response = json.decodeFromString<ReviewFormResponse>(
            """{"steps":[{"questions":[{"code":"courseIssues"},{"code":"courseIssues"}]}]}""",
        )
        assertThrows(IllegalArgumentException::class.java) { response.toIssueForm() }
    }

    @Test
    fun `비추천 후기 요청에는 문제 항목을 담고 난이도와 혼잡도는 보내지 않는다`() {
        val request = ReviewDraft(
            isRecommended = false,
            difficulty = null,
            congestion = null,
            practiceMethod = PracticeMethod.SOLO,
            content = null,
            caution = null,
            courseIssues = listOf("OTHER", "CONSTRUCTION"),
            courseIssueDetail = "공사 중",
        ).toRequest()
        val encoded = json.encodeToString(request)
        assertTrue(encoded.contains("\"courseIssues\":[\"OTHER\",\"CONSTRUCTION\"]"))
        assertTrue(encoded.contains("\"courseIssueDetail\":\"공사 중\""))
        assertFalse(encoded.contains("\"difficulty\""))
        assertFalse(encoded.contains("\"congestion\""))
    }
}
