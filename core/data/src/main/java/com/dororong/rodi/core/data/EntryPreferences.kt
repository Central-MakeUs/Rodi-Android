package com.dororong.rodi.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.entryDataStore by preferencesDataStore(name = "entry")

/**
 * 진입 게이트(위치권한·약관·주의사항) 완료 여부를 저장한다.
 * 완료되면 재실행 시 게이트를 건너뛰고 바로 홈으로 진입한다.
 */
class EntryPreferences(private val context: Context) {

    val isCompleted: Flow<Boolean?> =
        context.entryDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { it[KEY_COMPLETED] ?: false }

    suspend fun setCompleted() {
        context.entryDataStore.edit { it[KEY_COMPLETED] = true }
    }

    private companion object {
        val KEY_COMPLETED = booleanPreferencesKey("entry_completed")
    }
}
