package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.member.MyPage

interface MemberRepository {
    suspend fun getMyPage(): MyPage
    suspend fun updateDrivingGoal(drivingGoal: String)
    suspend fun withdraw()
}
