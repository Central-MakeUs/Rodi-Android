package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.domain.model.entry.EntryProgress
import com.dororong.rodi.core.domain.model.entry.EntryMode
import com.dororong.rodi.core.domain.model.entry.EntryProgressStep
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.entryDataStore by preferencesDataStore(name = "entry")

/**
 * 진입 게이트(위치권한·약관·주의사항) 완료 여부를 저장한다.
 * 완료되면 재실행 시 게이트를 건너뛰고 바로 홈으로 진입한다.
 */
@Singleton
class EntryPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

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

    val hasGuestAccess: Flow<Boolean> =
        context.entryDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { it[KEY_GUEST_ACCESS] ?: false }

    val hasRequestedLocationPermission: Flow<Boolean> =
        context.entryDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs ->
                prefs[KEY_LOCATION_PERMISSION_REQUESTED] ?: (prefs[KEY_COMPLETED] ?: false)
            }

    val progress: Flow<EntryProgress> =
        context.entryDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs ->
                EntryProgress(
                    mode = prefs[KEY_MODE].toEntryModeOrDefault(
                        hasGuestAccess = prefs[KEY_GUEST_ACCESS] ?: false,
                    ),
                    step = prefs[KEY_STEP].toEntryProgressStepOrDefault(),
                    webViewUrl = prefs[KEY_WEB_VIEW_URL].orEmpty(),
                    serviceTermsChecked = prefs[KEY_SERVICE_TERMS_CHECKED] ?: false,
                    privacyTermsChecked = prefs[KEY_PRIVACY_TERMS_CHECKED] ?: false,
                    locationTermsChecked = prefs[KEY_LOCATION_TERMS_CHECKED] ?: false,
                    licenseChecked = prefs[KEY_LICENSE_CHECKED] ?: false,
                    companionChecked = prefs[KEY_COMPANION_CHECKED] ?: false,
                    precautionAgreementChecked = prefs[KEY_PRECAUTION_AGREEMENT_CHECKED] ?: false,
                )
            }

    suspend fun setCompleted() {
        context.entryDataStore.edit { prefs ->
            prefs[KEY_COMPLETED] = true
            prefs[KEY_LOCATION_PERMISSION_REQUESTED] = true
            ENTRY_PROGRESS_KEYS.forEach { prefs.remove(it) }
        }
    }

    suspend fun start(mode: EntryMode) {
        context.entryDataStore.edit { prefs ->
            prefs[KEY_COMPLETED] = false
            prefs[KEY_MODE] = mode.name
            if (mode == EntryMode.GUEST_SIGN_UP) {
                prefs[KEY_STEP] = EntryProgressStep.NICKNAME.name
                prefs.remove(KEY_WEB_VIEW_URL)
            }
        }
    }

    suspend fun markLocationPermissionRequested() {
        context.entryDataStore.edit { it[KEY_LOCATION_PERMISSION_REQUESTED] = true }
    }

    suspend fun grantGuestAccess() {
        context.entryDataStore.edit { it[KEY_GUEST_ACCESS] = true }
    }

    suspend fun clearGuestAccess() {
        context.entryDataStore.edit { it[KEY_GUEST_ACCESS] = false }
    }

    suspend fun clear() {
        context.entryDataStore.edit { it.clear() }
    }

    suspend fun saveProgress(progress: EntryProgress) {
        context.entryDataStore.edit { prefs ->
            prefs[KEY_STEP] = progress.step.name
            prefs[KEY_MODE] = progress.mode.name
            prefs[KEY_WEB_VIEW_URL] = progress.webViewUrl
            prefs[KEY_SERVICE_TERMS_CHECKED] = progress.serviceTermsChecked
            prefs[KEY_PRIVACY_TERMS_CHECKED] = progress.privacyTermsChecked
            prefs[KEY_LOCATION_TERMS_CHECKED] = progress.locationTermsChecked
            prefs[KEY_LICENSE_CHECKED] = progress.licenseChecked
            prefs[KEY_COMPANION_CHECKED] = progress.companionChecked
            prefs[KEY_PRECAUTION_AGREEMENT_CHECKED] = progress.precautionAgreementChecked
        }
    }

    private companion object {
        val KEY_COMPLETED = booleanPreferencesKey("entry_completed")
        val KEY_GUEST_ACCESS = booleanPreferencesKey("guest_access")
        val KEY_LOCATION_PERMISSION_REQUESTED = booleanPreferencesKey("location_permission_requested")
        val KEY_STEP = stringPreferencesKey("entry_step")
        val KEY_WEB_VIEW_URL = stringPreferencesKey("entry_web_view_url")
        val KEY_SERVICE_TERMS_CHECKED = booleanPreferencesKey("service_terms_checked")
        val KEY_PRIVACY_TERMS_CHECKED = booleanPreferencesKey("privacy_terms_checked")
        val KEY_LOCATION_TERMS_CHECKED = booleanPreferencesKey("location_terms_checked")
        val KEY_LICENSE_CHECKED = booleanPreferencesKey("license_checked")
        val KEY_COMPANION_CHECKED = booleanPreferencesKey("companion_checked")
        val KEY_PRECAUTION_AGREEMENT_CHECKED = booleanPreferencesKey("precaution_agreement_checked")
        val KEY_MODE = stringPreferencesKey("entry_mode")
        val ENTRY_PROGRESS_KEYS = listOf(
            KEY_STEP,
            KEY_WEB_VIEW_URL,
            KEY_SERVICE_TERMS_CHECKED,
            KEY_PRIVACY_TERMS_CHECKED,
            KEY_LOCATION_TERMS_CHECKED,
            KEY_LICENSE_CHECKED,
            KEY_COMPANION_CHECKED,
            KEY_PRECAUTION_AGREEMENT_CHECKED,
        )
    }
}

private fun String?.toEntryProgressStepOrDefault(): EntryProgressStep =
    this?.let { value ->
        runCatching { EntryProgressStep.valueOf(value) }.getOrNull()
    } ?: EntryProgressStep.TERMS

private fun String?.toEntryModeOrDefault(hasGuestAccess: Boolean): EntryMode =
    this?.let { value -> EntryMode.entries.firstOrNull { it.name == value } }
        ?: if (hasGuestAccess) EntryMode.GUEST_BROWSE else EntryMode.AUTHENTICATED
