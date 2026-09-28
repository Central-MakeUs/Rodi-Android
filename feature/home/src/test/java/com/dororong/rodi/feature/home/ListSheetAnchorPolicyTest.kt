package com.dororong.rodi.feature.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ListSheetAnchorPolicyTest {

    @Test
    fun `부분 펼침 위치는 컨테이너 아래에서 미리보기 높이만큼 위에 있다`() {
        val positions = ListSheetAnchorPolicy.positions(
            containerHeightPx = 1_000,
            peekHeightPx = 380f,
            allowFull = true,
        )

        assertEquals(1_000f, positions.hiddenPx)
        assertEquals(620f, positions.partialPx)
        assertEquals(0f, positions.fullPx)
    }

    @Test
    fun `빈 시트는 전체 펼침 위치가 없어 위로 끌 수 없다`() {
        val positions = ListSheetAnchorPolicy.positions(
            containerHeightPx = 1_000,
            peekHeightPx = 380f,
            allowFull = false,
        )

        assertNull(positions.fullPx)
    }

    @Test
    fun `미리보기 높이가 컨테이너보다 크면 부분 펼침을 전체 펼침 위치로 합친다`() {
        val positions = ListSheetAnchorPolicy.positions(
            containerHeightPx = 300,
            peekHeightPx = 380f,
            allowFull = true,
        )

        assertEquals(0f, positions.partialPx)
    }

    @Test
    fun `컨테이너 크기를 아직 모르면 위치를 모두 0으로 둔다`() {
        val positions = ListSheetAnchorPolicy.positions(
            containerHeightPx = 0,
            peekHeightPx = 380f,
            allowFull = true,
        )

        assertEquals(0f, positions.hiddenPx)
        assertEquals(0f, positions.partialPx)
    }

    @Test
    fun `펼침 진행도는 부분 펼침에서 전체 펼침까지 0에서 1로 간다`() {
        assertEquals(0f, ListSheetAnchorPolicy.expansionProgress(offsetPx = 620f, partialOffsetPx = 620f))
        assertEquals(0.5f, ListSheetAnchorPolicy.expansionProgress(offsetPx = 310f, partialOffsetPx = 620f))
        assertEquals(1f, ListSheetAnchorPolicy.expansionProgress(offsetPx = 0f, partialOffsetPx = 620f))
    }

    @Test
    fun `시트를 위치 밖으로 끌어도 진행도는 0과 1 사이로 제한한다`() {
        assertEquals(0f, ListSheetAnchorPolicy.expansionProgress(offsetPx = 900f, partialOffsetPx = 620f))
        assertEquals(1f, ListSheetAnchorPolicy.expansionProgress(offsetPx = -40f, partialOffsetPx = 620f))
    }

    @Test
    fun `부분 펼침과 전체 펼침 위치가 같으면 진행도는 완전히 펼친 값이다`() {
        assertEquals(1f, ListSheetAnchorPolicy.expansionProgress(offsetPx = 0f, partialOffsetPx = 0f))
    }
}
