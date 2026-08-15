package com.dororong.rodi.feature.home.detail.components

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.Review
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReviewCardTest {
    @Test
    fun `other review exposes report and block actions`() {
        assertEquals(
            listOf("신고하기", "차단"),
            review(isMine = false, isEditable = false).menuItems(),
        )
    }

    @Test
    fun `editable own review exposes edit and delete actions`() {
        assertEquals(
            listOf("수정하기", "삭제하기"),
            review(isMine = true, isEditable = true).menuItems(),
        )
    }

    @Test
    fun `locked own review exposes delete only`() {
        assertEquals(
            listOf("삭제하기"),
            review(isMine = true, isEditable = false).menuItems(),
        )
    }

    private fun review(isMine: Boolean, isEditable: Boolean) = Review(
        reviewId = 1L,
        memberId = 2L,
        nickname = "로디",
        memberLevel = OnboardingLevel.SEED,
        isRecommended = true,
        difficulty = null,
        congestion = null,
        practiceMethod = null,
        content = null,
        caution = null,
        isMine = isMine,
        isEditable = isEditable,
        isHidden = false,
        createdAt = Instant.EPOCH,
        isVerifiedVisit = true,
    )
}
