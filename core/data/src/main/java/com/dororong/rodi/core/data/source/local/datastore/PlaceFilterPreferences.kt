package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.domain.model.place.PracticeType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.placeFilterDataStore by preferencesDataStore(name = "place_filters")

@Singleton
class PlaceFilterPreferences internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.placeFilterDataStore)

    suspend fun getPracticeTypes(): Set<PracticeType> =
        dataStore.data.first()[KEY_PRACTICE_TYPES].orEmpty().mapNotNull { value ->
            PracticeType.entries.firstOrNull { it.name == value }
        }.toSet()

    suspend fun setPracticeTypes(practiceTypes: Set<PracticeType>) {
        dataStore.edit { it[KEY_PRACTICE_TYPES] = practiceTypes.map(PracticeType::name).toSet() }
    }

    private companion object {
        val KEY_PRACTICE_TYPES = stringSetPreferencesKey("practice_types")
    }
}
