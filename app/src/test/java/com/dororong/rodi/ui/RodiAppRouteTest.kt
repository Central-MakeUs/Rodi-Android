package com.dororong.rodi.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RodiAppRouteTest {
    @Test
    fun `온보딩이 필요한 회원은 로컬 진입이 완료돼 있어도 온보딩으로 이동한다`() {
        assertEquals(EntryRoute, postLoginDestination(needsOnboarding = true, isEntryCompleted = true))
        assertEquals(EntryRoute, postLoginDestination(needsOnboarding = true, isEntryCompleted = false))
    }

    @Test
    fun `온보딩을 마친 회원은 로컬 진입 상태와 관계없이 메인으로 이동한다`() {
        assertEquals(MainRoute, postLoginDestination(needsOnboarding = false, isEntryCompleted = false))
        assertEquals(MainRoute, postLoginDestination(needsOnboarding = false, isEntryCompleted = true))
    }

    @Test
    fun `온보딩 여부를 모르면 로컬 진입 상태를 따른다`() {
        assertEquals(MainRoute, postLoginDestination(needsOnboarding = null, isEntryCompleted = true))
        assertEquals(EntryRoute, postLoginDestination(needsOnboarding = null, isEntryCompleted = false))
    }
}
