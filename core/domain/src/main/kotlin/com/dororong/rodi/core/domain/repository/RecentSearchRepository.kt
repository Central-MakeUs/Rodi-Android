package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.search.RecentSearch
import com.dororong.rodi.core.domain.model.search.RecentSearchRegistration

interface RecentSearchRepository {
    suspend fun getRecentSearches(): List<RecentSearch>
    suspend fun registerRecentSearch(search: RecentSearchRegistration)
    suspend fun deleteAllRecentSearches()
    suspend fun deleteRecentSearch(id: Long)
}
