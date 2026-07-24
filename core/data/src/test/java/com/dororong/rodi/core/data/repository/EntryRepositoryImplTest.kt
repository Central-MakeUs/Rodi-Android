package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.datastore.EntryPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class EntryRepositoryImplTest {
    @Test
    fun `clear delegates to local preferences`() = runTest {
        val prefs = mockk<EntryPreferences>(relaxed = true)
        coEvery { prefs.clear() } returns Unit
        val repository = EntryRepositoryImpl(prefs)

        repository.clear()

        coVerify { prefs.clear() }
    }
}
