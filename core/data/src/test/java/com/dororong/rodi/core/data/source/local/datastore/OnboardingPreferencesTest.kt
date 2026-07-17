package com.dororong.rodi.core.data.source.local.datastore

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OnboardingPreferencesTest {
    @Test
    fun `reads legacy road experience string without a type cast`() {
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("road_experience") to "WITH_COMPANION",
        )

        assertEquals(
            setOf("WITH_COMPANION"),
            preferences.readStringSet(
                key = stringSetPreferencesKey("road_experiences"),
                legacyKeyName = "road_experience",
            ),
        )
    }

    @Test
    fun `prefers the current set over a legacy string`() {
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("practice_situations") to "PARKING, U_TURN",
            stringSetPreferencesKey("practice_situation_set") to setOf("LANE_CHANGE"),
        )

        assertEquals(
            setOf("LANE_CHANGE"),
            preferences.readStringSet(
                key = stringSetPreferencesKey("practice_situation_set"),
                legacyKeyName = "practice_situations",
            ),
        )
    }
}
