package com.dororong.rodi.feature.home.review

import androidx.lifecycle.SavedStateHandle
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty

/**
 * 제출 전 후기 입력. SavedStateHandle은 Bundle에 들어가는 값만 프로세스 종료를 넘기므로 enum은 이름으로 둔다.
 * 어느 후기에 대한 입력인지(장소·수정 대상)를 함께 저장해 다른 대상에 되살리지 않는다.
 */
internal data class SavedReviewDraft(
    val placeId: Long,
    val reviewId: Long?,
    val step: ReviewWriteStep,
    val isRecommended: Boolean?,
    val difficulty: ReviewDifficulty?,
    val congestion: ReviewCongestion?,
    val caution: String,
    val practiceMethod: PracticeMethod?,
    val content: String,
) {
    fun write(handle: SavedStateHandle) {
        handle[KEY_PLACE_ID] = placeId
        handle[KEY_REVIEW_ID] = reviewId
        handle[KEY_STEP] = step.name
        handle[KEY_RECOMMENDED] = isRecommended
        handle[KEY_DIFFICULTY] = difficulty?.name
        handle[KEY_CONGESTION] = congestion?.name
        handle[KEY_CAUTION] = caution
        handle[KEY_PRACTICE_METHOD] = practiceMethod?.name
        handle[KEY_CONTENT] = content
    }

    companion object {
        private const val KEY_PLACE_ID = "review_draft_place_id"
        private const val KEY_REVIEW_ID = "review_draft_review_id"
        private const val KEY_STEP = "review_draft_step"
        private const val KEY_RECOMMENDED = "review_draft_recommended"
        private const val KEY_DIFFICULTY = "review_draft_difficulty"
        private const val KEY_CONGESTION = "review_draft_congestion"
        private const val KEY_CAUTION = "review_draft_caution"
        private const val KEY_PRACTICE_METHOD = "review_draft_practice_method"
        private const val KEY_CONTENT = "review_draft_content"
        private val KEYS = listOf(
            KEY_PLACE_ID,
            KEY_REVIEW_ID,
            KEY_STEP,
            KEY_RECOMMENDED,
            KEY_DIFFICULTY,
            KEY_CONGESTION,
            KEY_CAUTION,
            KEY_PRACTICE_METHOD,
            KEY_CONTENT,
        )

        fun from(state: ReviewWriteUiState) = SavedReviewDraft(
            placeId = state.placeId,
            reviewId = state.editingReviewId,
            step = state.step,
            isRecommended = state.isRecommended,
            difficulty = state.difficulty,
            congestion = state.congestion,
            caution = state.caution,
            practiceMethod = state.practiceMethod,
            content = state.content,
        )

        fun read(handle: SavedStateHandle): SavedReviewDraft? {
            val placeId = handle.get<Long>(KEY_PLACE_ID) ?: return null
            return SavedReviewDraft(
                placeId = placeId,
                reviewId = handle.get<Long>(KEY_REVIEW_ID),
                step = ReviewWriteStep.entries.firstOrNull { it.name == handle.get<String>(KEY_STEP) }
                    ?: ReviewWriteStep.Basics,
                isRecommended = handle.get<Boolean>(KEY_RECOMMENDED),
                difficulty = ReviewDifficulty.entries.firstOrNull { it.name == handle.get<String>(KEY_DIFFICULTY) },
                congestion = ReviewCongestion.entries.firstOrNull { it.name == handle.get<String>(KEY_CONGESTION) },
                caution = handle.get<String>(KEY_CAUTION).orEmpty(),
                practiceMethod = PracticeMethod.entries.firstOrNull { it.name == handle.get<String>(KEY_PRACTICE_METHOD) },
                content = handle.get<String>(KEY_CONTENT).orEmpty(),
            )
        }

        fun clear(handle: SavedStateHandle) {
            KEYS.forEach { handle.remove<Any>(it) }
        }
    }
}
