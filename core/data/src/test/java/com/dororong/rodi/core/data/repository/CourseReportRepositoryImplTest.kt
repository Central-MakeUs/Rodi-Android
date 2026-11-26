package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.CourseApi
import com.dororong.rodi.core.data.source.remote.model.course.CourseReportRequest
import com.dororong.rodi.core.data.source.remote.model.review.ReportFormOptionResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReportFormResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.course.CourseReportException
import com.dororong.rodi.core.domain.model.course.CourseReportSubmission
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

class CourseReportRepositoryImplTest {
    @Test
    fun `신고 폼을 선택지 순서대로 후기 문제 항목과 같은 모델로 바꾼다`() = runTest {
        val api = mockk<CourseApi>()
        coEvery { api.getReportForm() } returns ApiEnvelope(true, "COMMON_200", "성공", reportForm())

        val form = repository(api).getReportForm()

        assertEquals("해당 코스에 어떤 문제가 있나요?", form.title)
        assertEquals(listOf("VEHICLE_RESTRICTED", "OTHER"), form.options.map { it.code })
        assertEquals(30, form.options.last().textInputMaxLength)
    }

    @Test
    fun `선택한 문제 항목과 기타 입력을 detail 키로 보낸다`() = runTest {
        val api = mockk<CourseApi>()
        coEvery { api.reportCourse(any(), any()) } returns ApiEnvelope(true, "COMMON_200", "성공", null)

        repository(api).reportCourse(9, CourseReportSubmission(listOf("ROAD_INFO_MISMATCH", "OTHER"), "입구가 잠겨 있어요"))

        coVerify(exactly = 1) {
            api.reportCourse(9, CourseReportRequest(listOf("ROAD_INFO_MISMATCH", "OTHER"), "입구가 잠겨 있어요"))
        }
    }

    @Test
    fun `내 코스 신고 거절은 서버 문구로 알린다`() = runTest {
        val api = mockk<CourseApi>()
        coEvery { api.reportCourse(any(), any()) } throws httpException(400, "COURSE_400_1", "본인이 등록한 코스는 신고할 수 없습니다.")

        val error = assertThrowsSuspend<CourseReportException.OwnCourse> {
            repository(api).reportCourse(9, CourseReportSubmission(listOf("OTHER"), "기타"))
        }
        assertEquals("본인이 등록한 코스는 신고할 수 없습니다.", error.userMessage)
    }

    @Test
    fun `없는 코스와 삭제된 코스는 신고할 수 없는 코스로 알린다`() = runTest {
        val api = mockk<CourseApi>()
        coEvery { api.reportCourse(1, any()) } throws httpException(404, "COURSE_404_1", "코스를 찾을 수 없습니다.")
        coEvery { api.reportCourse(2, any()) } throws httpException(404, "COURSE_404_2", "삭제된 코스입니다.")

        val missing = assertThrowsSuspend<CourseReportException.Unavailable> {
            repository(api).reportCourse(1, CourseReportSubmission(listOf("CONSTRUCTION"), null))
        }
        val deleted = assertThrowsSuspend<CourseReportException.Unavailable> {
            repository(api).reportCourse(2, CourseReportSubmission(listOf("CONSTRUCTION"), null))
        }
        assertEquals("코스를 찾을 수 없습니다.", missing.userMessage)
        assertEquals("삭제된 코스입니다.", deleted.userMessage)
    }

    @Test
    fun `네트워크 실패는 연결 확인 안내로 바꾼다`() = runTest {
        val api = mockk<CourseApi>()
        coEvery { api.getReportForm() } throws IOException("offline")

        assertThrowsSuspend<CourseReportException.Network> { repository(api).getReportForm() }
    }

    @Test
    fun `로그인하지 않았으면 서버를 호출하지 않는다`() = runTest {
        val api = mockk<CourseApi>()
        val tokenStore = mockk<AuthTokenStore>().also { coEvery { it.getTokens() } returns null }

        assertThrowsSuspend<CourseReportException.AuthenticationRequired> {
            repository(api, tokenStore).reportCourse(9, CourseReportSubmission(listOf("OTHER"), "기타"))
        }
        coVerify(exactly = 0) { api.reportCourse(any(), any()) }
    }

    private fun repository(api: CourseApi, tokenStore: AuthTokenStore = tokenStore()) =
        CourseReportRepositoryImpl(api, tokenStore, Json { ignoreUnknownKeys = true })

    private fun tokenStore() = mockk<AuthTokenStore>().also {
        coEvery { it.getTokens() } returns AuthTokens("access", "refresh", "kakao")
    }

    private fun httpException(status: Int, code: String, message: String) = HttpException(
        Response.error<Any>(
            status,
            """{"isSuccess":false,"code":"$code","message":"$message","data":null}"""
                .toResponseBody("application/json".toMediaType()),
        ),
    )

    private fun reportForm() = ReportFormResponse(
        questionId = "COURSE_REPORT_ISSUES",
        title = "해당 코스에 어떤 문제가 있나요?",
        required = true,
        options = listOf(
            ReportFormOptionResponse("OTHER", "기타", 5, true, "이유를 작성해주세요", 30),
            ReportFormOptionResponse("VEHICLE_RESTRICTED", "차량 진입이 제한된 장소예요", 1, false),
        ),
    )
}
