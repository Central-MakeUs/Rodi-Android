package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.place.PracticeType

interface MemberRepository {
    suspend fun getMyPage(): MyPage
    suspend fun updateDrivingGoal(drivingGoal: String)
    suspend fun updateFilterTags(filterTags: List<PracticeType>)
    suspend fun blockMember(memberId: Long)
    suspend fun unblockMember(memberId: Long)
    suspend fun withdraw()
}
