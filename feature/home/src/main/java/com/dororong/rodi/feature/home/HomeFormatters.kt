package com.dororong.rodi.feature.home

import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.domain.model.course.Waypoint
import java.util.Locale

fun distanceText(route: RouteResult?, isRouting: Boolean): String = when {
    isRouting || route == null -> "주행거리 · 측정 중…"
    route.totalDistanceMeters >= 1000 ->
        "주행거리 · ${String.format(Locale.US, "%.1f", route.totalDistanceMeters / 1000.0)}km"
    else -> "주행거리 · ${route.totalDistanceMeters}m"
}

fun routeDistanceValue(route: RouteResult?, isRouting: Boolean): String = when {
    isRouting || route == null -> "측정 중…"
    route.totalDistanceMeters >= 1000 ->
        String.format(Locale.US, "%.1fkm", route.totalDistanceMeters / 1000.0)
    else -> "${route.totalDistanceMeters}m"
}

// ── 주소 단축 헬퍼 ────────────────────────────────────────────────────────────

private val CITY_PREFIXES = listOf(
    "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시",
    "대전광역시", "울산광역시", "세종특별자치시", "강원특별자치도", "경기도",
    "충청남도", "충청북도", "전라남도", "전라북도", "경상남도", "경상북도",
    "제주특별자치도", "서울", "부산", "대구", "인천", "광주", "대전", "울산",
    "세종", "강원", "경기", "제주",
)

/** 시/도 레벨 접두어만 제거 (구·군·동은 유지). VerticalStepList용. */
private fun String.stripCityPrefix(): String {
    val s = trim()
    CITY_PREFIXES.forEach { city -> if (s.startsWith("$city ")) return s.removePrefix("$city ").trim() }
    return s
}

/** 시/도 + 구/군 접두어까지 제거. 공백 구분 주소용. */
private fun String.stripCityAndDistrict(): String =
    stripCityPrefix().replace(Regex("^[가-힣0-9]+[구군]\\s+"), "")

fun Waypoint.displayStepAddress(): String {
    val source = address.takeIf { it.isNotBlank() } ?: name
    return source.toRoadNameWithNumberOrShortName()
}

private fun String.toRoadNameWithNumberOrShortName(): String {
    val compact = stripCityAndDistrict().trim()
    if (compact.isBlank()) return trim()

    val roadMatch = Regex("""^(.+?(?:대로|로|길))\s+(\d+(?:-\d+)?)""").find(compact)
    if (roadMatch != null) {
        return "${roadMatch.groupValues[1]} ${roadMatch.groupValues[2]}"
    }

    return compact
}

/** 도로명 주소를 짧게 표시. "마포나루길 467 스타벅스" 형태. */
fun String.shortenRoadAddress(): String {
    if (isBlank()) return this
    return if (contains(',')) shortenCommaRoad() else stripCityAndDistrict()
}

private fun String.shortenCommaRoad(): String {
    val parts = split(',').map { it.trim() }.filter {
        it.isNotEmpty() && !it.matches(Regex("\\d{5}")) && it != "대한민국"
    }
    // 도로명: 로/길/대로 로 끝나는 토큰
    val roadIdx = parts.indexOfFirst { p ->
        p.endsWith("로") || p.endsWith("길") || p.endsWith("대로") ||
                Regex("로\\d|길\\d").containsMatchIn(p)
    }
    if (roadIdx < 0) return parts.firstOrNull() ?: this

    val road = parts[roadIdx]
    // 번지: 도로명 바로 앞 토큰이 숫자/지하 면 사용
    val number = parts.getOrNull(roadIdx - 1)
        ?.takeIf { it.matches(Regex("\\d+(-\\d+)?|지하\\s*\\d+")) }
    // 건물명: 첫 토큰이 숫자·행정구역 접미사가 아닌 경우
    val building = parts.firstOrNull()?.takeIf { first ->
        parts.indexOf(first) != roadIdx &&
                !first.matches(Regex("\\d.*")) &&
                !first.endsWith("동") && !first.endsWith("구") && !first.endsWith("시") &&
                !first.endsWith("읍") && !first.endsWith("면") &&
                !first.endsWith("로") && !first.endsWith("길") && !first.endsWith("대로")
    }

    return buildString {
        append(road)
        if (number != null) append(" $number")
        if (building != null) append(" $building")
    }
}

/** 지번 주소를 짧게 표시. "망원동 205-4" 형태. */
fun String.shortenJibunAddress(): String {
    if (isBlank()) return this

    // 공백 구분 형식: "xxx동 번지" 패턴 탐색
    if (!contains(',')) {
        val match = Regex("([가-힣]+(?:동|리))\\s+(\\d+(?:-\\d+)?)").find(this)
        if (match != null) return "${match.groupValues[1]} ${match.groupValues[2]}"
        return stripCityAndDistrict()
    }

    // 쉼표 형식: 동/리 토큰 + 번지 토큰
    val parts = split(',').map { it.trim() }.filter {
        it.isNotEmpty() && !it.matches(Regex("\\d{5}")) && it != "대한민국"
    }
    val dong = parts.firstOrNull { it.endsWith("동") || it.endsWith("리") }
    val num = parts.firstOrNull { it.matches(Regex("\\d+(-\\d+)?")) }

    return when {
        dong != null && num != null -> "$dong $num"
        dong != null -> dong
        else -> shortenCommaRoad() // 동이 없으면 도로명으로 폴백
    }
}
