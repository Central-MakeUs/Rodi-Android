package com.dororong.rodi.feature.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ListSheetAnchorPolicyTest {

    @Test
    fun `partial anchor sits one peek height above the container bottom`() {
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
    fun `empty sheet has no full anchor so it cannot be dragged up`() {
        val positions = ListSheetAnchorPolicy.positions(
            containerHeightPx = 1_000,
            peekHeightPx = 380f,
            allowFull = false,
        )

        assertNull(positions.fullPx)
    }

    @Test
    fun `peek taller than the container collapses partial onto full`() {
        val positions = ListSheetAnchorPolicy.positions(
            containerHeightPx = 300,
            peekHeightPx = 380f,
            allowFull = true,
        )

        assertEquals(0f, positions.partialPx)
    }

    @Test
    fun `container is not measured yet`() {
        val positions = ListSheetAnchorPolicy.positions(
            containerHeightPx = 0,
            peekHeightPx = 380f,
            allowFull = true,
        )

        assertEquals(0f, positions.hiddenPx)
        assertEquals(0f, positions.partialPx)
    }

    @Test
    fun `expansion progress runs from partial to full`() {
        assertEquals(0f, ListSheetAnchorPolicy.expansionProgress(offsetPx = 620f, partialOffsetPx = 620f))
        assertEquals(0.5f, ListSheetAnchorPolicy.expansionProgress(offsetPx = 310f, partialOffsetPx = 620f))
        assertEquals(1f, ListSheetAnchorPolicy.expansionProgress(offsetPx = 0f, partialOffsetPx = 620f))
    }

    @Test
    fun `progress stays clamped while the sheet is dragged past its anchors`() {
        assertEquals(0f, ListSheetAnchorPolicy.expansionProgress(offsetPx = 900f, partialOffsetPx = 620f))
        assertEquals(1f, ListSheetAnchorPolicy.expansionProgress(offsetPx = -40f, partialOffsetPx = 620f))
    }

    @Test
    fun `progress is fully expanded when partial and full anchors coincide`() {
        assertEquals(1f, ListSheetAnchorPolicy.expansionProgress(offsetPx = 0f, partialOffsetPx = 0f))
    }
}
