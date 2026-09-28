package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.datastore.EntryPreferences
import com.dororong.rodi.core.domain.model.entry.EntryMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class EntryRepositoryImplTest {
    @Test
    fun `진입 시작은 진입 모드를 로컬 저장소에 위임한다`() = runTest {
        val prefs = mockk<EntryPreferences>(relaxed = true)
        val repository = EntryRepositoryImpl(prefs)

        repository.start(EntryMode.GUEST_SIGN_UP)

        coVerify { prefs.start(EntryMode.GUEST_SIGN_UP) }
    }

    @Test
    fun `초기화는 로컬 저장소에 위임한다`() = runTest {
        val prefs = mockk<EntryPreferences>(relaxed = true)
        coEvery { prefs.clear() } returns Unit
        val repository = EntryRepositoryImpl(prefs)

        repository.clear()

        coVerify { prefs.clear() }
    }
}
