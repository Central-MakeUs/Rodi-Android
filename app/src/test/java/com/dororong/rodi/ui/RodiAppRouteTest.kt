package com.dororong.rodi.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RodiAppRouteTest {
    @Test
    fun `new member enters onboarding even when local entry was completed`() {
        assertEquals(EntryRoute, postLoginDestination(isNewMember = true, isEntryCompleted = true))
    }

    @Test
    fun `existing member enters main regardless of local entry state`() {
        assertEquals(MainRoute, postLoginDestination(isNewMember = false, isEntryCompleted = false))
    }

    @Test
    fun `unknown member status falls back to local entry state`() {
        assertEquals(MainRoute, postLoginDestination(isNewMember = null, isEntryCompleted = true))
        assertEquals(EntryRoute, postLoginDestination(isNewMember = null, isEntryCompleted = false))
    }
}
