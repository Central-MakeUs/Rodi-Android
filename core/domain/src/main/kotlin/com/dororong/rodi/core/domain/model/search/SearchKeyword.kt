package com.dororong.rodi.core.domain.model.search

fun String.normalizeSearchKeyword(): String = trim().also {
    require(it.length in 1..50) { "검색어는 1~50자여야 합니다." }
}
