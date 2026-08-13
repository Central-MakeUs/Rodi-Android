package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.domain.repository.PracticePromptDismissalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.practicePromptDataStore by preferencesDataStore(name = "practice_prompt")
private val DISMISSED_IDS_KEY = stringSetPreferencesKey("dismissed_practice_ids")

// refreshToken을 유저 스코프 키로 썼던 이전 버전은 refresh token이 재발급마다 회전하는 걸
// 놓쳐, 액세스 토큰이 만료될 때마다 닫은 기록이 조용히 사라지고 RV-01이 다시 떴다(자기 자신을
// 재현하는 버그였다). practiceId는 로그인한 회원의 서버 목록에서만 오므로(loadPlannedPractice가
// GET /members/me/practices 결과에서 걸러낸다) 계정 간 충돌 위험이 없어 유저 스코프가 필요 없다.
@Singleton
class PracticePromptDismissalStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PracticePromptDismissalRepository {
    override suspend fun readDismissedPracticeIds(): Set<Long> = withContext(Dispatchers.IO) {
        try {
            context.practicePromptDataStore.data.first()[DISMISSED_IDS_KEY]
                .orEmpty()
                .mapNotNull(String::toLongOrNull)
                .toSet()
        } catch (_: IOException) {
            emptySet()
        }
    }

    override suspend fun dismiss(practiceId: Long) {
        updateIds { it + practiceId.toString() }
    }

    override suspend fun restore(practiceId: Long) {
        updateIds { it - practiceId.toString() }
    }

    private suspend fun updateIds(transform: (Set<String>) -> Set<String>) = withContext(Dispatchers.IO) {
        try {
            context.practicePromptDataStore.edit { preferences ->
                preferences[DISMISSED_IDS_KEY] = transform(preferences[DISMISSED_IDS_KEY].orEmpty())
            }
        } catch (_: IOException) {
        }
    }
}
