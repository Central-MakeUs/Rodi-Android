package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reportedReviewDataStore by preferencesDataStore(name = "reported_reviews")

/**
 * 내가 신고한 후기 id를 기기에 남긴다.
 *
 * 서버는 서로 다른 5명에게 신고돼야 후기를 감추고, 신고자 본인인지 알려주는 필드도 없다.
 * 그래서 "신고하면 내 눈앞에서 바로 사라진다"는 기기 로컬로만 유지된다 — 재설치하거나
 * 다른 기기로 로그인하면 그 후기가 다시 보인다.
 */
@Singleton
class ReportedReviewPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val reportedReviewIds: Flow<Set<Long>> = context.reportedReviewDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[KEY].orEmpty().mapNotNull(String::toLongOrNull).toSet()
        }

    suspend fun add(reviewId: Long) {
        context.reportedReviewDataStore.edit { prefs ->
            prefs[KEY] = prefs[KEY].orEmpty() + reviewId.toString()
        }
    }

    suspend fun clear() {
        context.reportedReviewDataStore.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringSetPreferencesKey("reported_review_ids")
    }
}
