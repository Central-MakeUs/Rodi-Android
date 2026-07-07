package com.dororong.rodi.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.domain.OnboardingProfile

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")

/**
 * 온보딩 설문(닉네임·경력·선호) 응답을 로컬에 저장한다.
 * 서버 API/점수 계산 스펙이 없어 지금은 DataStore 로컬 저장만 한다(BACKLOG 참고).
 */
class OnboardingPreferences(private val context: Context) {

    suspend fun saveProfile(profile: OnboardingProfile) {
        context.onboardingDataStore.edit { prefs ->
            prefs[KEY_NICKNAME] = profile.nickname
            profile.drivingPeriod?.let { prefs[KEY_DRIVING_PERIOD] = it.name }
            profile.recentFrequency?.let { prefs[KEY_RECENT_FREQUENCY] = it.name }
            profile.roadExperience?.let { prefs[KEY_ROAD_EXPERIENCE] = it.name }
            profile.soloDrivingRange?.let { prefs[KEY_SOLO_DRIVING_RANGE] = it.name }
            profile.soloParkingLevel?.let { prefs[KEY_SOLO_PARKING_LEVEL] = it.name }
            prefs[KEY_PRACTICE_SITUATIONS] = profile.practiceSituations.map { it.name }.toSet()
            profile.vehicleType?.let { prefs[KEY_VEHICLE_TYPE] = it.name }
            prefs[KEY_GOAL] = profile.goal
        }
    }

    private companion object {
        val KEY_NICKNAME = stringPreferencesKey("nickname")
        val KEY_DRIVING_PERIOD = stringPreferencesKey("driving_period")
        val KEY_RECENT_FREQUENCY = stringPreferencesKey("recent_frequency")
        val KEY_ROAD_EXPERIENCE = stringPreferencesKey("road_experience")
        val KEY_SOLO_DRIVING_RANGE = stringPreferencesKey("solo_driving_range")
        val KEY_SOLO_PARKING_LEVEL = stringPreferencesKey("solo_parking_level")
        val KEY_PRACTICE_SITUATIONS = stringSetPreferencesKey("practice_situations")
        val KEY_VEHICLE_TYPE = stringPreferencesKey("vehicle_type")
        val KEY_GOAL = stringPreferencesKey("goal")
    }
}
