package com.dororong.rodi.ui.testmenu

import android.content.Context
import com.dororong.rodi.core.domain.usecase.entry.ClearEntryStateUseCase
import com.dororong.rodi.feature.mypage.testmenu.TestMenuAction
import com.dororong.rodi.feature.mypage.testmenu.TestMenuSection
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface EntryTestEntryPoint {
    fun clearEntryState(): ClearEntryStateUseCase
}

/**
 * 약관 동의·권한 안내·운전 주의사항은 한 번 지나가면 앱을 지우기 전엔 다시 볼 수 없다.
 * 진입 상태만 지우고 로그인 세션은 건드리지 않는다 — 다시 로그인할 필요 없이 진입 화면부터 확인한다.
 */
internal object EntryTestTools {
    fun section(context: Context): TestMenuSection {
        val appContext = context.applicationContext
        return TestMenuSection(
            title = "진입 화면",
            actions = listOf(
                TestMenuAction("약관·권한 안내부터 다시 보기") {
                    EntryPointAccessors
                        .fromApplication(appContext, EntryTestEntryPoint::class.java)
                        .clearEntryState()
                        .invoke()
                    "초기화했어요.\n앱을 완전히 종료한 뒤 다시 켜면 약관 동의부터 시작합니다."
                },
            ),
        )
    }
}
