package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.mapper.toDomain
import com.dororong.rodi.core.data.mapper.toQueryValue
import com.dororong.rodi.core.data.mapper.toRequest
import com.dororong.rodi.core.data.source.local.datastore.ReportedReviewPreferences
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.remote.api.ReviewApi
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.review.ReportForm
import com.dororong.rodi.core.domain.model.review.ReportSubmission
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewDetail
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import com.dororong.rodi.core.domain.model.review.ReviewException
import com.dororong.rodi.core.domain.model.review.ReviewLevelFilter
import com.dororong.rodi.core.domain.model.review.ReviewSummary
import com.dororong.rodi.core.domain.repository.ReviewRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

class ReviewRepositoryImpl @Inject constructor(
    private val api: ReviewApi,
    private val tokenStore: AuthTokenStore,
    private val reportedReviewPreferences: ReportedReviewPreferences,
) : ReviewRepository {
    override suspend fun getReviews(
        placeId: Long,
        level: ReviewLevelFilter,
        cursor: String?,
        size: Int,
    ): CursorPage<Review> = authenticatedRequest {
        api.getReviews(
            placeId = placeId,
            level = level.toQueryValue(),
            size = size,
            cursor = cursor,
        ).requireData().toDomain()
    }

    override suspend fun getSummary(placeId: Long, level: ReviewLevelFilter): ReviewSummary =
        authenticatedRequest {
            api.getSummary(placeId, level.toQueryValue()).requireData().toDomain()
        }

    override suspend fun getReview(reviewId: Long): ReviewDetail = authenticatedRequest {
        api.getReview(reviewId).requireData().toDomain()
    }

    override suspend fun createReview(placeId: Long, draft: ReviewDraft): Long =
        authenticatedRequest(operation = ReviewOperation.CREATE) {
            api.createReview(placeId, draft.toRequest())
                .requireData(ReviewOperation.CREATE)
                .reviewId
        }

    override suspend fun updateReview(reviewId: Long, draft: ReviewDraft) {
        authenticatedRequest(operation = ReviewOperation.UPDATE) {
            api.updateReview(reviewId, draft.toRequest())
                .requireSuccess(ReviewOperation.UPDATE)
        }
    }

    override suspend fun deleteReview(reviewId: Long) {
        authenticatedRequest {
            api.deleteReview(reviewId).requireSuccess()
        }
    }

    override suspend fun reportReview(reviewId: Long, submission: ReportSubmission) {
        authenticatedRequest {
            api.reportReview(reviewId, submission.toRequest()).requireSuccess()
        }
        // 신고가 접수돼도 서버는 5명이 모일 때까지 후기를 내려준다. 신고자 화면에서만 감추려고
        // 기기에 기록해 둔다.
        reportedReviewPreferences.add(reviewId)
    }

    override suspend fun getReportedReviewIds(): Set<Long> =
        reportedReviewPreferences.reportedReviewIds.first()

    override suspend fun getReportForm(): ReportForm = authenticatedRequest {
        api.getReportForm().requireData().toDomain()
    }

    /**
     * 토큰 주입과 401 재발급은 OkHttp의 AuthHeaderInterceptor·TokenAuthenticator가 한다.
     * 여기서는 로그인 여부만 확인하고, 남은 실패를 작업별 도메인 예외로 바꾼다.
     */
    private suspend fun <T> authenticatedRequest(
        operation: ReviewOperation = ReviewOperation.DEFAULT,
        block: suspend () -> T,
    ): T {
        tokenStore.getTokens()?.accessToken
            ?: throw ReviewException.AuthenticationRequired("로그인이 필요합니다.")
        return try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw error.toReviewException(operation)
        }
    }

}

private enum class ReviewOperation {
    DEFAULT,
    CREATE,
    UPDATE,
}

private fun <T> ApiEnvelope<T>.requireData(
    operation: ReviewOperation = ReviewOperation.DEFAULT,
): T {
    if (!isSuccess) throw toReviewException(operation)
    return data ?: throw ReviewException.Unexpected("$code: ${message.ifBlank { "응답 데이터가 없습니다." }}")
}

private fun ApiEnvelope<*>.requireSuccess(operation: ReviewOperation = ReviewOperation.DEFAULT) {
    if (!isSuccess) throw toReviewException(operation)
}

private fun ApiEnvelope<*>.toReviewException(operation: ReviewOperation): ReviewException = when {
    code.contains("400") -> ReviewException.InvalidRequest(message)
    code.contains("401") -> ReviewException.AuthenticationRequired(message)
    code.contains("403") -> ReviewException.Forbidden(message)
    code.contains("404") -> ReviewException.NotFound(message)
    code.contains("409") && operation == ReviewOperation.CREATE -> ReviewException.LevelRequired(message)
    code.contains("409") && operation == ReviewOperation.UPDATE -> ReviewException.LevelChanged(message)
    else -> ReviewException.Unexpected("$code: ${message.ifBlank { "후기 요청에 실패했습니다." }}")
}

private fun Throwable.toReviewException(operation: ReviewOperation): ReviewException = when (this) {
    is ReviewException -> this
    is HttpException -> when (code()) {
        400 -> ReviewException.InvalidRequest(message(), this)
        401 -> ReviewException.AuthenticationRequired(message(), this)
        403 -> ReviewException.Forbidden(message(), this)
        404 -> ReviewException.NotFound(message(), this)
        409 -> when (operation) {
            ReviewOperation.CREATE -> ReviewException.LevelRequired(message(), this)
            ReviewOperation.UPDATE -> ReviewException.LevelChanged(message(), this)
            ReviewOperation.DEFAULT -> ReviewException.Unexpected(message(), this)
        }
        else -> ReviewException.Unexpected(message(), this)
    }
    is IOException -> ReviewException.Network("네트워크 연결을 확인해주세요.", this)
    else -> ReviewException.Unexpected("후기 요청에 실패했습니다.", this)
}
