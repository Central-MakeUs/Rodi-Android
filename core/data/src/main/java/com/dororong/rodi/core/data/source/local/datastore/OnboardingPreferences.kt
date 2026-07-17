package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.PracticeSituation
import com.dororong.rodi.core.domain.model.onboarding.RecentDrivingFrequency
import com.dororong.rodi.core.domain.model.onboarding.RoadExperience
import com.dororong.rodi.core.domain.model.onboarding.SoloDrivingRange
import com.dororong.rodi.core.domain.model.onboarding.SoloParkingLevel
import com.dororong.rodi.core.domain.model.onboarding.VehicleType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")

/**
 * 온보딩 설문(닉네임·경력·선호) 응답을 로컬에 저장한다.
 * 서버 API/점수 계산 스펙이 없어 지금은 DataStore 로컬 저장만 한다(BACKLOG 참고).
 */
@Singleton
class OnboardingPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    val profile: Flow<OnboardingProfile> =
        context.onboardingDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs ->
                OnboardingProfile(
                    nickname = prefs[KEY_NICKNAME].orEmpty(),
                    drivingPeriod = prefs[KEY_DRIVING_PERIOD].toEnumOrNull<DrivingPeriod>(),
                    recentFrequency = prefs[KEY_RECENT_FREQUENCY].toEnumOrNull<RecentDrivingFrequency>(),
                    roadExperiences = prefs
                        .readStringSet(KEY_ROAD_EXPERIENCES, LEGACY_ROAD_EXPERIENCE_KEY)
                        .toEnumList<RoadExperience>(),
                    soloDrivingRange = prefs[KEY_SOLO_DRIVING_RANGE].toEnumOrNull<SoloDrivingRange>(),
                    soloParkingLevel = prefs[KEY_SOLO_PARKING_LEVEL].toEnumOrNull<SoloParkingLevel>(),
                    practiceSituations = prefs
                        .readStringSet(KEY_PRACTICE_SITUATIONS, LEGACY_PRACTICE_SITUATIONS_KEY)
                        .toEnumList<PracticeSituation>(),
                    vehicleType = prefs[KEY_VEHICLE_TYPE].toEnumOrNull<VehicleType>(),
                    goal = prefs[KEY_GOAL].orEmpty(),
                )
            }

    suspend fun saveProfile(profile: OnboardingProfile) {
        context.onboardingDataStore.edit { prefs ->
            prefs[KEY_NICKNAME] = profile.nickname
            profile.drivingPeriod?.let { prefs[KEY_DRIVING_PERIOD] = it.name } ?: prefs.remove(KEY_DRIVING_PERIOD)
            profile.recentFrequency?.let { prefs[KEY_RECENT_FREQUENCY] = it.name } ?: prefs.remove(KEY_RECENT_FREQUENCY)
            prefs[KEY_ROAD_EXPERIENCES] = profile.roadExperiences.map { it.name }.toSet()
            profile.soloDrivingRange?.let { prefs[KEY_SOLO_DRIVING_RANGE] = it.name } ?: prefs.remove(
                KEY_SOLO_DRIVING_RANGE,
            )
            profile.soloParkingLevel?.let { prefs[KEY_SOLO_PARKING_LEVEL] = it.name } ?: prefs.remove(
                KEY_SOLO_PARKING_LEVEL,
            )
            prefs[KEY_PRACTICE_SITUATIONS] = profile.practiceSituations.map { it.name }.toSet()
            profile.vehicleType?.let { prefs[KEY_VEHICLE_TYPE] = it.name } ?: prefs.remove(KEY_VEHICLE_TYPE)
            prefs[KEY_GOAL] = profile.goal
        }
    }

    private companion object {
        val KEY_NICKNAME = stringPreferencesKey("nickname")
        val KEY_DRIVING_PERIOD = stringPreferencesKey("driving_period")
        val KEY_RECENT_FREQUENCY = stringPreferencesKey("recent_frequency")
        val KEY_ROAD_EXPERIENCES = stringSetPreferencesKey("road_experiences")
        val KEY_SOLO_DRIVING_RANGE = stringPreferencesKey("solo_driving_range")
        val KEY_SOLO_PARKING_LEVEL = stringPreferencesKey("solo_parking_level")
        val KEY_PRACTICE_SITUATIONS = stringSetPreferencesKey("practice_situation_set")
        val KEY_VEHICLE_TYPE = stringPreferencesKey("vehicle_type")
        val KEY_GOAL = stringPreferencesKey("goal")

        const val LEGACY_ROAD_EXPERIENCE_KEY = "road_experience"
        const val LEGACY_PRACTICE_SITUATIONS_KEY = "practice_situations"
    }
}

internal fun Preferences.readStringSet(
    key: Preferences.Key<Set<String>>,
    legacyKeyName: String,
): Set<String> {
    val values = asMap()
    val savedValues = values.entries
        .firstOrNull { it.key.name == key.name }
        ?.value as? Set<*>
    if (savedValues != null) return savedValues.filterIsInstance<String>().toSet()

    return when (val legacyValue = values.entries
        .firstOrNull { it.key.name == legacyKeyName }
        ?.value
    ) {
        is Set<*> -> legacyValue.filterIsInstance<String>().toSet()
        is String -> legacyValue.split(',', '|', '\n')
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
        else -> emptySet()
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    this?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() }

private inline fun <reified T : Enum<T>> Set<String>.toEnumList(): List<T> {
    val values = this
    return enumValues<T>().filter { values.contains(it.name) }
}
