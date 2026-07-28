package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.search.RecentSearch

interface RecentSearchRepository {
    suspend fun getRecentSearches(): List<RecentSearch>
    suspend fun deleteAllRecentSearches()
    suspend fun deleteRecentSearch(id: Long)
}
