package com.dororong.rodi.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RodiAppRouteTest {
    @Test
    fun `member who needs onboarding enters it even when local entry was completed`() {
        assertEquals(EntryRoute, postLoginDestination(needsOnboarding = true, isEntryCompleted = true))
        assertEquals(EntryRoute, postLoginDestination(needsOnboarding = true, isEntryCompleted = false))
    }

    @Test
    fun `onboarded member enters main regardless of local entry state`() {
        assertEquals(MainRoute, postLoginDestination(needsOnboarding = false, isEntryCompleted = false))
        assertEquals(MainRoute, postLoginDestination(needsOnboarding = false, isEntryCompleted = true))
    }

    @Test
    fun `unknown onboarding state falls back to local entry state`() {
        assertEquals(MainRoute, postLoginDestination(needsOnboarding = null, isEntryCompleted = true))
        assertEquals(EntryRoute, postLoginDestination(needsOnboarding = null, isEntryCompleted = false))
    }
}
