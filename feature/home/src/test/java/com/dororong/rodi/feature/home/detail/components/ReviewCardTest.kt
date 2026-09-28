package com.dororong.rodi.feature.home.detail.components

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.Review
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReviewCardTest {
    @Test
    fun `다른 사람의 후기는 신고와 차단 메뉴를 보여준다`() {
        assertEquals(
            listOf("신고하기", "차단"),
            review(isMine = false, isEditable = false).menuItems(),
        )
    }

    @Test
    fun `수정할 수 있는 내 후기는 수정과 삭제 메뉴를 보여준다`() {
        assertEquals(
            listOf("수정하기", "삭제하기"),
            review(isMine = true, isEditable = true).menuItems(),
        )
    }

    @Test
    fun `잠긴 내 후기는 삭제 메뉴만 보여준다`() {
        assertEquals(
            listOf("삭제하기"),
            review(isMine = true, isEditable = false).menuItems(),
        )
    }

    @Test
    fun `연습 방식 문구는 작성자가 어떻게 왔는지 보여준다`() {
        assertEquals("혼자 왔어요", PracticeMethod.SOLO.label)
        assertEquals("동행했어요", PracticeMethod.WITH_COMPANION.label)
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
