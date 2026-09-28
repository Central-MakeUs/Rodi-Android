package com.dororong.rodi.core.data.source.local.datastore

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OnboardingPreferencesTest {
    @Test
    fun `예전 문자열 형식의 도로 경험 값도 타입 변환 오류 없이 읽는다`() {
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
    fun `예전 문자열보다 현재 집합 값을 우선한다`() {
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
