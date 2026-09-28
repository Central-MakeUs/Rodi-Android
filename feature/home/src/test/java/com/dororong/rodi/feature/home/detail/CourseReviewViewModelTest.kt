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
    fun `후기 불러오기는 추천 수는 전체 요약에서 난이도는 내 레벨에서 가져온다`() = runTest(dispatcher) {
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

        val state = vm.uiState.value
        assertEquals(15L, state.recommendCount)
        assertEquals(61L, state.totalCount)
        assertEquals(OnboardingLevel.ROOKIE, state.selectedLevel)
        assertEquals(mapOf(ReviewDifficulty.VERY_EASY to 30L), state.difficultyCounts)
        assertEquals(1, state.latestReviews.size)

        coVerify(exactly = 1) { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) }
        coVerify(exactly = 1) { getReviewSummary(PLACE_ID, ReviewLevelFilter.Mine) }
    }

    @Test
    fun `레벨을 바꿔도 추천 수를 유지하고 전체 요약을 다시 부르지 않는다`() = runTest(dispatcher) {
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

        val state = vm.uiState.value
        assertEquals(15L, state.recommendCount)
        assertEquals(OnboardingLevel.OWNER, state.selectedLevel)
        assertEquals(mapOf(ReviewDifficulty.HARD to 7L), state.difficultyCounts)

        coVerify(exactly = 1) { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) }
    }

    @Test
    fun `다음 페이지는 후기를 이어 붙이고 중복을 뺀다`() = runTest(dispatcher) {
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

        assertEquals(listOf(1L, 2L, 3L), vm.uiState.value.reviews.map { it.reviewId })
    }

    @Test
    fun `둘러보기 사용자는 후기 요청을 모두 건너뛴다`() = runTest(dispatcher) {
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = false, hasRecentKakaoLogin = false)

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isGuest)
        coVerify(exactly = 0) { getReviewSummary(any(), any()) }
        coVerify(exactly = 0) { getPlaceReviews(any(), any(), any(), any()) }
    }

    @Test
    fun `실패하면 장소를 지우지 않고 오류 문구를 보여준다`() = runTest(dispatcher) {
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

        val state = vm.uiState.value
        assertEquals("요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.", state.errorMessage)
        assertEquals(PLACE_ID, state.placeId)
    }

    @Test
    fun `요청이 실패한 뒤 같은 장소를 다시 불러올 수 있다`() = runTest(dispatcher) {
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

        assertFalse(vm.uiState.value.isLoading)
        coVerify(exactly = 2) { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) }
    }

    @Test
    fun `로그인 세션 조회가 실패해도 로딩 상태를 끝낸다`() = runTest(dispatcher) {
        coEvery { getAuthSession() } throws IllegalStateException("세션을 불러오지 못했어요.")

        val vm = viewModel()
        vm.load(PLACE_ID)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.", vm.uiState.value.errorMessage)
    }

    @Test
    fun `레벨을 바꿔 후기를 불러오면 이전 레벨 페이지를 교체한다`() = runTest(dispatcher) {
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

        assertEquals(OnboardingLevel.OWNER, vm.uiState.value.selectedLevel)
        assertEquals(listOf(2L), vm.uiState.value.reviews.map { it.reviewId })
        assertEquals(null, vm.uiState.value.nextCursor)
        assertFalse(vm.uiState.value.hasNext)
    }

    @Test
    fun `차단한 회원의 후기를 요약과 전체 목록에서 뺀다`() = runTest(dispatcher) {
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

        assertTrue(vm.uiState.value.latestReviews.none { it.memberId == 1L })
        assertTrue(vm.uiState.value.reviews.none { it.memberId == 1L })
    }

    @Test
    fun `바로 새로고침한 응답이 이전 데이터여도 방금 작성한 후기는 보인다`() = runTest(dispatcher) {
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

        assertEquals(listOf(REVIEW_ID), vm.uiState.value.latestReviews.map { it.reviewId })
        assertEquals(6L, vm.uiState.value.totalCount)
        assertEquals(1L, vm.uiState.value.recommendCount)
        assertEquals("초보초보", vm.uiState.value.latestReviews.single().nickname)
        assertEquals("후기 내용", vm.uiState.value.latestReviews.single().content)
        assertEquals(clock.instant(), vm.uiState.value.latestReviews.single().createdAt)
    }

    @Test
    fun `작성 완료 알림이 반복돼도 후기를 중복하거나 두 번 세지 않는다`() = runTest(dispatcher) {
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

        assertEquals(listOf(REVIEW_ID), vm.uiState.value.latestReviews.map { it.reviewId })
        assertEquals(1L, vm.uiState.value.totalCount)
        coVerify(exactly = 1) { getMyPage() }
    }

    @Test
    fun `낙관적 개수는 다른 장소를 불러올 때 섞이지 않는다`() = runTest(dispatcher) {
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
        assertEquals(SECOND_PLACE_ID, vm.uiState.value.placeId)
        assertEquals(0L, vm.uiState.value.totalCount)
        assertEquals(0L, vm.uiState.value.recommendCount)

        vm.load(PLACE_ID)
        advanceUntilIdle()
        assertEquals(6L, vm.uiState.value.totalCount)
        assertEquals(1L, vm.uiState.value.recommendCount)
    }

    @Test
    fun `후기를 지우면 보이는 전체 수를 줄이고 추천 수는 서버에서 다시 받는다`() = runTest(dispatcher) {
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

        assertTrue(vm.uiState.value.latestReviews.isEmpty())
        assertEquals(1L, vm.uiState.value.totalCount)
        assertEquals(0L, vm.uiState.value.recommendCount)
        coVerify(exactly = 2) { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) }
    }

    @Test
    fun `지운 후기의 추천 여부를 모르면 요약을 다시 받는다`() = runTest(dispatcher) {
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

        assertEquals(0L, vm.uiState.value.recommendCount)
        coVerify(exactly = 2) { getReviewSummary(PLACE_ID, ReviewLevelFilter.All) }
    }

    @Test
    fun `낙관적 후기는 프로필 레벨이 달라도 작성 때 고른 레벨을 유지한다`() = runTest(dispatcher) {
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

        assertEquals(OnboardingLevel.SEED, vm.uiState.value.latestReviews.single().memberLevel)
    }

    @Test
    fun `서버 후기가 낙관적 후기를 중복 없이 대체한다`() = runTest(dispatcher) {
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

        assertEquals(listOf(REVIEW_ID), vm.uiState.value.latestReviews.map { it.reviewId })
        assertEquals(1L, vm.uiState.value.totalCount)
        assertEquals("초보초보", vm.uiState.value.latestReviews.single().nickname)
    }

    @Test
    fun `늦게 도착한 신고 목록도 이미 합쳐진 후기를 숨긴다`() = runTest(dispatcher) {
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
            vm.uiState.value.latestReviews.isEmpty(),
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
