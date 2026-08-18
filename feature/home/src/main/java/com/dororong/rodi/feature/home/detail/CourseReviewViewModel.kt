package com.dororong.rodi.feature.home.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.userMessage
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewLevelFilter
import com.dororong.rodi.core.domain.model.review.ReviewSubmissionResult
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import com.dororong.rodi.core.domain.usecase.member.GetMyPageUseCase
import com.dororong.rodi.core.domain.usecase.review.GetPlaceReviewsUseCase
import com.dororong.rodi.core.domain.usecase.review.GetReportedReviewIdsUseCase
import com.dororong.rodi.core.domain.usecase.review.GetReviewSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CourseReviewViewModel @Inject constructor(
    private val getReviewSummary: GetReviewSummaryUseCase,
    private val getPlaceReviews: GetPlaceReviewsUseCase,
    private val getAuthSession: GetAuthSessionUseCase,
    private val getMyPage: GetMyPageUseCase,
    private val getReportedReviewIds: GetReportedReviewIdsUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(CourseReviewUiState())
    val state: StateFlow<CourseReviewUiState> = _state.asStateFlow()
    private var summaryJob: Job? = null
    private var initialReviewsJob: Job? = null
    private var nextPageJob: Job? = null
    private val optimisticReviews = mutableMapOf<OptimisticReviewKey, Review>()
    private val handledCreatedReviews = mutableSetOf<OptimisticReviewKey>()
    private val optimisticCountTargets = mutableMapOf<OptimisticReviewKey, Long>()
    private val optimisticRecommendTargets = mutableMapOf<OptimisticReviewKey, Long>()
    private val reportedReviewIds = mutableSetOf<Long>()

    init {
        viewModelScope.launch { reportedReviewIds += getReportedReviewIds() }
    }

    /** 신고 직후 신고자 화면에서만 후기를 감춘다 — 서버는 5명이 모일 때까지 계속 내려준다. */
    fun excludeReportedReview(reviewId: Long) {
        reportedReviewIds += reviewId
        _state.update {
            it.copy(
                latestReviews = it.latestReviews.filterNot { review -> review.reviewId == reviewId },
                reviews = it.reviews.filterNot { review -> review.reviewId == reviewId },
            )
        }
    }

    fun load(placeId: Long) {
        val current = _state.value
        if (current.placeId == placeId && !current.isLoading && current.errorMessage == null) return

        summaryJob?.cancel()
        cancelReviewPageLoads()
        summaryJob = viewModelScope.launch {
            _state.value = CourseReviewUiState(placeId = placeId, isLoading = true)
            try {
                if (!getAuthSession().isLoggedIn) {
                    _state.value = CourseReviewUiState(placeId = placeId, isGuest = true)
                    return@launch
                }
                val all = async { getReviewSummary(placeId, ReviewLevelFilter.All) }
                val mine = async { getReviewSummary(placeId, ReviewLevelFilter.Mine) }
                val mineSummary = mine.await()
                val selectedLevel = mineSummary.getOrNull()?.level ?: OnboardingLevel.SEED
                val allSummary = all.await()
                val latest = getPlaceReviews(placeId, ReviewLevelFilter.Mine, size = 1)
                val error = allSummary.exceptionOrNull() ?: mineSummary.exceptionOrNull() ?: latest.exceptionOrNull()
                val latestItems = latest.getOrNull()?.items.orEmpty()
                acknowledgeOptimisticReviews(placeId, latestItems)
                val mergedLatest = mergeReviews(placeId, selectedLevel, latestItems)
                val totalCount = allSummary.getOrNull()?.totalCount ?: 0
                val recommendCount = allSummary.getOrNull()?.recommendCount ?: 0
                acknowledgeOptimisticCounts(placeId, totalCount)
                acknowledgeOptimisticRecommendCounts(placeId, recommendCount)
                _state.value = CourseReviewUiState(
                    placeId = placeId,
                    selectedLevel = selectedLevel,
                    totalCount = maxOf(totalCount, optimisticCountTarget(placeId), mergedLatest.size.toLong()),
                    recommendCount = maxOf(recommendCount, optimisticRecommendTarget(placeId)),
                    difficultyCounts = mineSummary.getOrNull()?.difficultyCounts.orEmpty(),
                    latestReviews = mergedLatest,
                    errorMessage = error?.userMessage(),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (_state.value.placeId == placeId) {
                    _state.update { it.copy(isLoading = false, errorMessage = error.userMessage()) }
                }
            }
        }
    }

    fun selectLevel(level: OnboardingLevel) {
        selectLevel(level, loadReviews = false)
    }

    fun selectLevelAndLoadReviews(level: OnboardingLevel) {
        selectLevel(level, loadReviews = true)
    }

    private fun selectLevel(level: OnboardingLevel, loadReviews: Boolean) {
        val placeId = _state.value.placeId ?: return
        summaryJob?.cancel()
        cancelReviewPageLoads()
        _state.update {
            it.copy(
                isLoading = true,
                selectedLevel = level,
                difficultyCounts = emptyMap(),
                latestReviews = emptyList(),
                reviews = emptyList(),
                nextCursor = null,
                hasNext = false,
                isNextPageLoading = false,
                errorMessage = null,
            )
        }
        summaryJob = viewModelScope.launch {
            val summary = getReviewSummary(placeId, ReviewLevelFilter.Of(level))
            val latest = getPlaceReviews(placeId, ReviewLevelFilter.Of(level), size = 1)
            if (_state.value.placeId != placeId || _state.value.selectedLevel != level) return@launch
            val latestItems = latest.getOrNull()?.items.orEmpty()
            acknowledgeOptimisticReviews(placeId, latestItems)
            val mergedLatest = mergeReviews(placeId, level, latestItems)
            _state.update {
                it.copy(
                    isLoading = false,
                    difficultyCounts = summary.getOrNull()?.difficultyCounts.orEmpty(),
                    latestReviews = mergedLatest,
                    totalCount = maxOf(it.totalCount, mergedLatest.size.toLong()),
                    errorMessage = (summary.exceptionOrNull() ?: latest.exceptionOrNull())?.userMessage(),
                )
            }
        }
        if (loadReviews) loadInitialReviews(placeId, level)
    }

    fun loadInitialReviews() {
        val placeId = _state.value.placeId ?: return
        loadInitialReviews(placeId, _state.value.selectedLevel)
    }

    private fun loadInitialReviews(placeId: Long, level: OnboardingLevel) {
        initialReviewsJob?.cancel()
        nextPageJob?.cancel()
        _state.update { it.copy(isNextPageLoading = false) }
        initialReviewsJob = viewModelScope.launch {
            val result = getPlaceReviews(placeId, ReviewLevelFilter.Of(level), size = PAGE_SIZE)
            if (_state.value.placeId != placeId || _state.value.selectedLevel != level) return@launch
            result
                .onSuccess { page ->
                    acknowledgeOptimisticReviews(placeId, page.items)
                    _state.update {
                        it.copy(
                            reviews = mergeReviews(placeId, level, page.items),
                            nextCursor = page.nextCursor,
                            hasNext = page.hasNext,
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.userMessage()) } }
        }
    }

    fun loadNextPage() {
        val current = _state.value
        val placeId = current.placeId ?: return
        val cursor = current.nextCursor ?: return
        if (!current.hasNext || current.isNextPageLoading || initialReviewsJob?.isActive == true || nextPageJob?.isActive == true) return
        nextPageJob = viewModelScope.launch {
            _state.update { it.copy(isNextPageLoading = true) }
            getPlaceReviews(placeId, ReviewLevelFilter.Of(current.selectedLevel), cursor, PAGE_SIZE)
                .onSuccess { page ->
                    acknowledgeOptimisticReviews(placeId, page.items)
                    if (_state.value.placeId == placeId && _state.value.selectedLevel == current.selectedLevel) {
                        _state.update {
                            it.copy(
                                reviews = mergeReviews(placeId, current.selectedLevel, it.reviews + page.items),
                                nextCursor = page.nextCursor,
                                hasNext = page.hasNext,
                                isNextPageLoading = false,
                            )
                        }
                    }
                }
                .onFailure { error -> _state.update { it.copy(isNextPageLoading = false, errorMessage = error.userMessage()) } }
        }
    }

    fun excludeMemberReviews(memberId: Long) {
        _state.update {
            it.copy(
                latestReviews = it.latestReviews.filterNot { review -> review.memberId == memberId },
                reviews = it.reviews.filterNot { review -> review.memberId == memberId },
            )
        }
    }

    fun removeReview(reviewId: Long) {
        val current = _state.value
        val removedReview = current.latestReviews.firstOrNull { it.reviewId == reviewId }
            ?: current.reviews.firstOrNull { it.reviewId == reviewId }
        val placeId = current.placeId
        optimisticReviews.keys
            .filter { it.reviewId == reviewId && (placeId == null || it.placeId == placeId) }
            .forEach(optimisticReviews::remove)
        optimisticCountTargets.keys
            .filter { it.reviewId == reviewId && (placeId == null || it.placeId == placeId) }
            .forEach(optimisticCountTargets::remove)
        optimisticRecommendTargets.keys
            .filter { it.reviewId == reviewId && (placeId == null || it.placeId == placeId) }
            .forEach(optimisticRecommendTargets::remove)
        _state.update {
            it.copy(
                latestReviews = it.latestReviews.filterNot { review -> review.reviewId == reviewId },
                reviews = it.reviews.filterNot { review -> review.reviewId == reviewId },
                totalCount = if (removedReview == null) {
                    it.totalCount
                } else {
                    (it.totalCount - 1).coerceAtLeast(0)
                },
            )
        }
        // 목록 응답은 더 이상 isRecommended를 내려주지 않아(서버 스키마 변경) 로컬 리스트만으로는
        // 지운 후기가 추천이었는지 알 수 없다 — 항상 서버에서 다시 받아온다.
        if (removedReview != null && placeId != null) {
            refreshRecommendCount(placeId)
        }
    }

    fun onReviewSubmitted(result: ReviewSubmissionResult) {
        val key = OptimisticReviewKey(result.placeId, result.reviewId)
        if (!result.isEditing && !handledCreatedReviews.add(key)) return
        val current = _state.value
        val existing = current.latestReviews.firstOrNull { it.reviewId == result.reviewId }
            ?: current.reviews.firstOrNull { it.reviewId == result.reviewId }
            ?: optimisticReviews[key]
        val fallbackLevel = current.selectedLevel
        if (!result.isEditing) {
            val baseCount = maxOf(current.totalCount, optimisticCountTarget(result.placeId))
            optimisticCountTargets[key] = baseCount + 1
            if (result.draft.isRecommended) {
                val baseRecommendCount = maxOf(current.recommendCount, optimisticRecommendTarget(result.placeId))
                optimisticRecommendTargets[key] = baseRecommendCount + 1
            }
        }

        viewModelScope.launch {
            val optimistic = result.toReview(
                existing = existing,
                fallbackLevel = fallbackLevel,
                nickname = "나",
                createdAt = clock.instant(),
            )
            upsertOptimistic(key, optimistic)

            getMyPage().getOrNull()?.let { myPage ->
                if (myPage.nickname.isNotBlank() && optimisticReviews[key] != null) {
                    upsertOptimistic(
                        key,
                        optimisticReviews.getValue(key).copy(
                            nickname = myPage.nickname,
                        ),
                    )
                }
            }
        }
    }

    fun refresh() {
        val placeId = _state.value.placeId ?: return
        _state.update { it.copy(placeId = null) }
        load(placeId)
    }

    private fun refreshRecommendCount(placeId: Long) {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            getReviewSummary(placeId, ReviewLevelFilter.All).onSuccess { summary ->
                if (_state.value.placeId == placeId) {
                    _state.update { it.copy(recommendCount = summary.recommendCount) }
                }
            }
        }
    }

    private fun cancelReviewPageLoads() {
        initialReviewsJob?.cancel()
        nextPageJob?.cancel()
    }

    private fun upsertOptimistic(key: OptimisticReviewKey, review: Review) {
        optimisticReviews[key] = review
        if (_state.value.placeId != key.placeId) return
        _state.update { current ->
            val latest = mergeReviews(key.placeId, current.selectedLevel, current.latestReviews)
            val reviews = mergeReviews(key.placeId, current.selectedLevel, current.reviews)
            current.copy(
                latestReviews = latest,
                reviews = reviews,
                totalCount = maxOf(current.totalCount, optimisticCountTarget(key.placeId), latest.size.toLong(), reviews.size.toLong()),
                recommendCount = maxOf(current.recommendCount, optimisticRecommendTarget(key.placeId)),
            )
        }
    }

    private fun acknowledgeOptimisticReviews(placeId: Long, networkReviews: List<Review>) {
        networkReviews.forEach { networkReview ->
            val key = OptimisticReviewKey(placeId, networkReview.reviewId)
            val optimistic = optimisticReviews[key] ?: return@forEach
            if (optimistic.matches(networkReview)) optimisticReviews.remove(key)
        }
    }

    private fun acknowledgeOptimisticCounts(placeId: Long, networkTotalCount: Long) {
        optimisticCountTargets.entries
            .filter { (key, target) -> key.placeId == placeId && networkTotalCount >= target }
            .forEach { optimisticCountTargets.remove(it.key) }
    }

    private fun optimisticCountTarget(placeId: Long): Long = optimisticCountTargets
        .filterKeys { it.placeId == placeId }
        .values
        .maxOrNull()
        ?: 0

    private fun acknowledgeOptimisticRecommendCounts(placeId: Long, networkRecommendCount: Long) {
        optimisticRecommendTargets.entries
            .filter { (key, target) -> key.placeId == placeId && networkRecommendCount >= target }
            .forEach { optimisticRecommendTargets.remove(it.key) }
    }

    private fun optimisticRecommendTarget(placeId: Long): Long = optimisticRecommendTargets
        .filterKeys { it.placeId == placeId }
        .values
        .maxOrNull()
        ?: 0

    private fun mergeReviews(
        placeId: Long,
        level: OnboardingLevel,
        networkReviews: List<Review>,
    ): List<Review> {
        val pending = optimisticReviews
            .filter { (key, review) -> key.placeId == placeId && review.memberLevel == level }
            .values
        // 목록은 항상 특정 레벨로 필터링해서 받아오므로 응답 항목의 작성자 레벨은 그 필터 레벨이다.
        // 서버 응답에 레벨 필드가 없어서, 여기서 채워야 카드에 레벨별 프로필을 그릴 수 있다.
        val leveled = networkReviews.map { it.copy(memberLevel = it.memberLevel ?: level) }
        return (pending + leveled)
            .distinctBy(Review::reviewId)
            .filterNot { it.reviewId in reportedReviewIds }
    }

    private fun Review.matches(other: Review): Boolean =
        isRecommended.matchesNetworkValue(other.isRecommended) &&
            difficulty.matchesNetworkValue(other.difficulty) &&
            congestion.matchesNetworkValue(other.congestion) &&
            practiceMethod.matchesNetworkValue(other.practiceMethod) &&
            content.matchesNetworkValue(other.content) &&
            caution.matchesNetworkValue(other.caution)

    private fun <T> T.matchesNetworkValue(networkValue: T?): Boolean =
        networkValue == null || this == networkValue

    private fun ReviewSubmissionResult.toReview(
        existing: Review?,
        fallbackLevel: OnboardingLevel,
        nickname: String,
        createdAt: java.time.Instant,
    ) = Review(
        reviewId = reviewId,
        memberId = existing?.memberId ?: OPTIMISTIC_MEMBER_ID,
        nickname = existing?.nickname ?: nickname,
        memberLevel = existing?.memberLevel ?: fallbackLevel,
        isRecommended = draft.isRecommended,
        difficulty = draft.difficulty,
        congestion = draft.congestion,
        practiceMethod = draft.practiceMethod,
        content = draft.content,
        caution = draft.caution,
        isMine = true,
        isEditable = true,
        isHidden = existing?.isHidden ?: false,
        createdAt = existing?.createdAt ?: createdAt,
        isVerifiedVisit = existing?.isVerifiedVisit,
    )

    private data class OptimisticReviewKey(
        val placeId: Long,
        val reviewId: Long,
    )

    companion object {
        private const val PAGE_SIZE = 10
        private const val OPTIMISTIC_MEMBER_ID = -1L
    }
}
