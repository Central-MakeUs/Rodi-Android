package com.dororong.rodi.core.domain.model.driving

/**
 * 실시간 업데이트는 앱이 요청만 하고 승격 여부는 OS가 정한다. 제조사마다 구현이 달라
 * 시스템 플래그(FLAG_PROMOTED_ONGOING)가 안 붙어도 화면에는 실시간으로 보이는 기기가 있다
 * (One UI 8 실측, 2026-09-16). 그래서 기기 판정 대신 사용자의 앱 내 선택만 저장한다.
 */
data class LiveUpdateSettings(
    /** 끄면 승격 요청과 진행바 스타일을 모두 빼고 일반 진행 알림으로 띄운다. */
    val isEnabled: Boolean = true,
)
