package com.dororong.rodi.feature.home.detail

import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewLevelFilter
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import com.dororong.rodi.core.domain.model.review.ReviewSubmissionResult
import com.dororong.rodi.core.domain.model.review.ReviewSummary
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import com.dororong.rodi.core.domain.usecase.member.GetMyPageUseCase
import com.dororong.rodi.core.domain.usecase.review.GetReportedReviewIdsUseCase
import com.dororong.rodi.core.domain.usecase.review.GetPlaceReviewsUseCase
import com.dororong.rodi.core.domain.usecase.review.GetReviewSummaryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class CourseReviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val getReviewSummary = mockk<GetReviewSummaryUseCase>()
    private val getPlaceReviews = mockk<GetPlaceReviewsUseCase>()
    private val getAuthSession = mockk<GetAuthSessionUseCase>()
    private val getMyPage = mockk<GetMyPageUseCase>()
    private val getReportedReviewIds = mockk<GetReportedReviewIdsUseCase>(relaxed = true)
    private val clock = Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC)

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = CourseReviewViewModel(getReviewSummary, getPlaceReviews, getAuthSession, getMyPage, getReportedReviewIds, clock)

    private fun loggedIn() {
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = false)
    }

    @Test
    fun `load reads recommend count from the ALL summary and difficulty from my level`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) } returns
            Result.success(summary(level = null, total = 61, recommend = 15))
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) } returns
            Result.success(
                summary(
                    level = OnboardingLevel.ROOKIE,
                    total = 30,
                    recommend = 9,
                    difficulty = mapOf(ReviewDifficulty.VERY_EASY to 30L),
                ),
            )
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(listOf(review(1L))))

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(15L, state.recommendCount)
        assertEquals(61L, state.totalCount)
        assertEquals(OnboardingLevel.ROOKIE, state.selectedLevel)
        assertEquals(mapOf(ReviewDifficulty.VERY_EASY to 30L), state.difficultyCounts)
        assertEquals(1, state.latestReviews.size)

        coVerify(exactly = 1) { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) }
        coVerify(exactly = 1) { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) }
    }

    @Test
    fun `selectLevel keeps the recommend count and does not refetch the ALL summary`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) } returns
            Result.success(summary(level = null, total = 61, recommend = 15))
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 10, recommend = 4))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(emptyList()))

        val target = ReviewLevelFilter.Of(OnboardingLevel.OWNER)
        coEvery { getReviewSummary(PLACE_ID, target) } returns
            Result.success(
                summary(
                    level = OnboardingLevel.OWNER,
                    total = 7,
                    recommend = 2,
                    difficulty = mapOf(ReviewDifficulty.HARD to 7L),
                ),
            )
        coEvery { getPlaceReviews(PLACE_ID, target, null, 1) } returns Result.success(page(emptyList()))

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()
        vm.selectLevel(OnboardingLevel.OWNER)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(15L, state.recommendCount)
        assertEquals(OnboardingLevel.OWNER, state.selectedLevel)
        assertEquals(mapOf(ReviewDifficulty.HARD to 7L), state.difficultyCounts)

        coVerify(exactly = 1) { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) }
    }

    @Test
    fun `loadNextPage appends items and drops duplicates`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, any()) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 4, recommend = 1))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(emptyList()))

        val level = ReviewLevelFilter.Of(OnboardingLevel.SEED)
        coEvery { getPlaceReviews(PLACE_ID, level, null, 10) } returns
            Result.success(page(listOf(review(1L), review(2L)), hasNext = true, nextCursor = "c1"))
        coEvery { getPlaceReviews(PLACE_ID, level, "c1", 10) } returns
            Result.success(page(listOf(review(2L), review(3L))))

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()
        vm.loadInitialReviews()
        advanceUntilIdle()
        vm.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L, 3L), vm.state.value.reviews.map { it.reviewId })
    }

    @Test
    fun `guest skips every review request`() = runTest(dispatcher) {
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = false, hasRecentKakaoLogin = false)

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()

        assertTrue(vm.state.value.isGuest)
        coVerify(exactly = 0) { getReviewSummary(any(), any()) }
        coVerify(exactly = 0) { getPlaceReviews(any(), any(), any(), any()) }
    }

    @Test
    fun `failure surfaces an error message without clearing the place`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) } returns
            Result.failure(IllegalStateException("후기를 불러오지 못했어요."))
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 0, recommend = 0))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(emptyList()))

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.", state.errorMessage)
        assertEquals(PLACE_ID, state.placeId)
    }

    @Test
    fun `load can retry the same place after a failed request`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) } returns
            Result.failure(IllegalStateException("후기를 불러오지 못했어요."))
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 0, recommend = 0))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(emptyList()))

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()
        vm.load(PLACE_ID)
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        coVerify(exactly = 2) { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) }
    }

    @Test
    fun `load clears loading state when auth session lookup throws`() = runTest(dispatcher) {
        coEvery { getAuthSession() } throws IllegalStateException("세션을 불러오지 못했어요.")

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertEquals("요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.", vm.state.value.errorMessage)
    }

    @Test
    fun `selectLevelAndLoadReviews replaces the previous level page`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) } returns
            Result.success(summary(level = null, total = 2, recommend = 1))
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 1, recommend = 1))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(listOf(review(1L))))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Of(OnboardingLevel.SEED), null, 10) } returns
            Result.success(page(listOf(review(1L)), hasNext = true, nextCursor = "seed"))

        val target = ReviewLevelFilter.Of(OnboardingLevel.OWNER)
        coEvery { getReviewSummary(PLACE_ID, target) } returns
            Result.success(summary(level = OnboardingLevel.OWNER, total = 1, recommend = 0))
        coEvery { getPlaceReviews(PLACE_ID, target, null, 1) } returns Result.success(page(listOf(review(2L))))
        coEvery { getPlaceReviews(PLACE_ID, target, null, 10) } returns Result.success(page(listOf(review(2L))))

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()
        vm.loadInitialReviews()
        advanceUntilIdle()
        vm.selectLevelAndLoadReviews(OnboardingLevel.OWNER)
        advanceUntilIdle()

        assertEquals(OnboardingLevel.OWNER, vm.state.value.selectedLevel)
        assertEquals(listOf(2L), vm.state.value.reviews.map { it.reviewId })
        assertEquals(null, vm.state.value.nextCursor)
        assertFalse(vm.state.value.hasNext)
    }

    @Test
    fun `excludeMemberReviews removes the member from summary and full lists`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, any()) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 2, recommend = 1))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(listOf(review(1L))))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Of(OnboardingLevel.SEED), null, 10) } returns
            Result.success(page(listOf(review(1L), review(2L))))

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()
        vm.loadInitialReviews()
        advanceUntilIdle()
        vm.excludeMemberReviews(1L)

        assertTrue(vm.state.value.latestReviews.none { it.memberId == 1L })
        assertTrue(vm.state.value.reviews.none { it.memberId == 1L })
    }

    @Test
    fun `submitted review stays visible when immediate refresh returns stale data`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) } returns
            Result.success(summary(level = null, total = 5, recommend = 0))
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 5, recommend = 0))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(emptyList()))
        coEvery { getMyPage() } returns Result.success(myPage())

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()
        vm.onReviewSubmitted(submission())
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()

        assertEquals(listOf(REVIEW_ID), vm.state.value.latestReviews.map { it.reviewId })
        assertEquals(6L, vm.state.value.totalCount)
        assertEquals(1L, vm.state.value.recommendCount)
        assertEquals("초보초보", vm.state.value.latestReviews.single().nickname)
        assertEquals("후기 내용", vm.state.value.latestReviews.single().content)
        assertEquals(clock.instant(), vm.state.value.latestReviews.single().createdAt)
    }

    @Test
    fun `repeated submission notification does not duplicate or double count`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, any()) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 0, recommend = 0))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(emptyList()))
        coEvery { getMyPage() } returns Result.success(myPage())

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()
        val result = submission()
        vm.onReviewSubmitted(result)
        vm.onReviewSubmitted(result)
        advanceUntilIdle()

        assertEquals(listOf(REVIEW_ID), vm.state.value.latestReviews.map { it.reviewId })
        assertEquals(1L, vm.state.value.totalCount)
        coVerify(exactly = 1) { getMyPage() }
    }

    @Test
    fun `optimistic counts stay isolated when loading another place`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) } returns
            Result.success(summary(level = null, total = 5, recommend = 0))
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 5, recommend = 0))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(emptyList()))
        coEvery { getReviewSummary(SECOND_PLACE_ID, any()) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 0, recommend = 0))
        coEvery { getPlaceReviews(SECOND_PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(emptyList()))
        coEvery { getMyPage() } returns Result.success(myPage())

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()
        vm.onReviewSubmitted(submission())
        advanceUntilIdle()

        vm.load(SECOND_PLACE_ID)
        advanceUntilIdle()
        assertEquals(SECOND_PLACE_ID, vm.state.value.placeId)
        assertEquals(0L, vm.state.value.totalCount)
        assertEquals(0L, vm.state.value.recommendCount)

        vm.load(PLACE_ID)
        advanceUntilIdle()
        assertEquals(6L, vm.state.value.totalCount)
        assertEquals(1L, vm.state.value.recommendCount)
    }

    @Test
    fun `removeReview decrements visible total and refreshes recommend count from the server`() = runTest(dispatcher) {
        // 목록 응답은 isRecommended를 안 주므로(서버 스키마 변경) 지운 후기가 추천이었는지
        // 로컬에서 알 수 없다 — removeReview는 항상 서버 요약을 다시 받아 recommendCount를 맞춘다.
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) } returnsMany listOf(
            Result.success(summary(level = null, total = 2, recommend = 1)),
            Result.success(summary(level = null, total = 1, recommend = 0)),
        )
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 1, recommend = 1))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(listOf(review(1L))))

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()
        vm.removeReview(1L)
        advanceUntilIdle()

        assertTrue(vm.state.value.latestReviews.isEmpty())
        assertEquals(1L, vm.state.value.totalCount)
        assertEquals(0L, vm.state.value.recommendCount)
        coVerify(exactly = 2) { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) }
    }

    @Test
    fun `removeReview refreshes the summary when the network review has no recommendation value`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) } returnsMany listOf(
            Result.success(summary(level = null, total = 1, recommend = 1)),
            Result.success(summary(level = null, total = 0, recommend = 0)),
        )
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 1, recommend = 1))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(listOf(review(1L).copy(isRecommended = null))))

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()
        vm.removeReview(1L)
        advanceUntilIdle()

        assertEquals(0L, vm.state.value.recommendCount)
        coVerify(exactly = 2) { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) }
    }

    @Test
    fun `optimistic review keeps the selected level when profile level differs`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, any()) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 0, recommend = 0))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(emptyList()))
        coEvery { getMyPage() } returns Result.success(myPage().copy(level = OnboardingLevel.ROOKIE))

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()
        vm.onReviewSubmitted(submission())
        advanceUntilIdle()

        assertEquals(OnboardingLevel.SEED, vm.state.value.latestReviews.single().memberLevel)
    }

    @Test
    fun `network review replaces optimistic review without duplication`() = runTest(dispatcher) {
        loggedIn()
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) } returnsMany listOf(
            Result.success(summary(level = null, total = 0, recommend = 0)),
            Result.success(summary(level = null, total = 1, recommend = 1)),
        )
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) } returnsMany listOf(
            Result.success(summary(level = OnboardingLevel.SEED, total = 0, recommend = 0)),
            Result.success(summary(level = OnboardingLevel.SEED, total = 1, recommend = 1)),
        )
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returnsMany listOf(
            Result.success(page(emptyList())),
            Result.success(page(listOf(review(REVIEW_ID, content = "후기 내용")))),
        )
        coEvery { getMyPage() } returns Result.success(myPage())

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()
        vm.onReviewSubmitted(submission())
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()

        assertEquals(listOf(REVIEW_ID), vm.state.value.latestReviews.map { it.reviewId })
        assertEquals(1L, vm.state.value.totalCount)
        assertEquals("초보초보", vm.state.value.latestReviews.single().nickname)
    }

    @Test
    fun `late reported id lookup still hides an already merged review`() = runTest(dispatcher) {
        loggedIn()
        // 신고 목록 조회가 load()보다 늦게 끝나는 순서를 재현한다 — 먼저 끝나면 애초에 병합 단계에서 걸러진다.
        coEvery { getReportedReviewIds() } coAnswers {
            delay(1_000)
            setOf(1L)
        }
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) } returns
            Result.success(summary(level = null, total = 1, recommend = 0))
        coEvery { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) } returns
            Result.success(summary(level = OnboardingLevel.SEED, total = 1, recommend = 0))
        coEvery { getPlaceReviews(PLACE_ID, ReviewLevelFilter.Mine, null, 1) } returns
            Result.success(page(listOf(review(1L))))

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()

        assertTrue(
            vm.state.value.latestReviews.isEmpty(),
            "늦게 도착한 신고 목록이 이미 병합된 후기에도 적용돼야 한다",
        )
    }

    private fun summary(
        level: OnboardingLevel?,
        total: Long,
        recommend: Long,
        difficulty: Map<ReviewDifficulty, Long> = emptyMap(),
    ) = ReviewSummary(
        level = level,
        totalCount = total,
        recommendCount = recommend,
        notRecommendCount = 0,
        difficultyCounts = difficulty,
        congestionCounts = emptyMap(),
        levelCounts = emptyMap(),
    )

    private fun page(
        items: List<Review>,
        hasNext: Boolean = false,
        nextCursor: String? = null,
    ) = CursorPage(items = items, hasNext = hasNext, nextCursor = nextCursor, totalCount = items.size.toLong())

    private fun review(id: Long) = Review(
        reviewId = id,
        memberId = id,
        nickname = "초보초보",
        memberLevel = OnboardingLevel.SEED,
        isRecommended = true,
        difficulty = ReviewDifficulty.VERY_EASY,
        congestion = ReviewCongestion.QUIET,
        practiceMethod = PracticeMethod.SOLO,
        content = "자전거 타기 좋은 곳",
        caution = null,
        isMine = false,
        isEditable = false,
        isHidden = false,
        createdAt = Instant.parse("2026-05-10T00:00:00Z"),
        isVerifiedVisit = true,
    )

    private fun review(id: Long, content: String) = review(id).copy(
        nickname = "초보초보",
        memberId = -1L,
        isMine = true,
        isEditable = true,
        content = content,
    )

    private fun myPage() = MyPage(
        nickname = "초보초보",
        level = OnboardingLevel.SEED,
        recommendationTags = emptyList(),
        drivingGoal = null,
        savedPlaceCount = 0,
    )

    private fun submission() = ReviewSubmissionResult(
        placeId = PLACE_ID,
        reviewId = REVIEW_ID,
        draft = ReviewDraft(
            isRecommended = true,
            difficulty = ReviewDifficulty.VERY_EASY,
            congestion = ReviewCongestion.QUIET,
            practiceMethod = PracticeMethod.SOLO,
            content = "후기 내용",
            caution = null,
        ),
        isEditing = false,
    )

    private companion object {
        const val PLACE_ID = 42L
        const val SECOND_PLACE_ID = 43L
        const val REVIEW_ID = 31L
    }
}
