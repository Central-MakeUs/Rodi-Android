package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.member.PracticeRecordItem
import com.dororong.rodi.core.domain.model.member.MyReview
import com.dororong.rodi.core.domain.model.member.BlockedMember
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PracticeType

interface MemberRepository {
    suspend fun getMyPage(): MyPage
    suspend fun getPracticeRecords(cursor: String?, size: Int): CursorPage<PracticeRecordItem>
    suspend fun getMyReviews(cursor: String?, size: Int): CursorPage<MyReview>
    suspend fun getBlockedMembers(cursor: String?, size: Int): CursorPage<BlockedMember>
    suspend fun updateDrivingGoal(drivingGoal: String)
    suspend fun updateFilterTags(filterTags: List<PracticeType>)
    suspend fun blockMember(memberId: Long)
    suspend fun unblockMember(memberId: Long)
    suspend fun withdraw()
}
