package com.dororong.rodi.core.ui.text

/**
 * UTF-16 code unit이 아니라 사용자가 인지하는 문자(grapheme cluster) 단위로 길이를 센다.
 * 서로게이트 쌍으로 이뤄진 이모지(예: 😁)나 ZWJ로 묶인 이모지 시퀀스도 하나로 센다 —
 * [String.length]는 이런 이모지를 2개 이상으로 센다.
 */
fun String.graphemeLength(): Int = graphemeBoundaries().size

/** 사용자가 인지하는 문자 [maxLength]개까지만 남기고 자른다. 이모지 시퀀스를 중간에서 깨지 않는다. */
fun String.takeGraphemes(maxLength: Int): String {
    if (maxLength <= 0) return ""
    val boundaries = graphemeBoundaries()
    val boundary = boundaries.getOrNull(maxLength - 1) ?: return this
    return substring(0, boundary)
}

/**
 * 실기기에서는 [android.icu.text.BreakIterator]를 쓴다 — 플랫폼 java.text.BreakIterator보다
 * 최신 유니코드 grapheme cluster 규칙(가족 이모지, 국기, 피부톤 변형자 등)을 더 정확히 따른다.
 * 로컬 JVM 단위 테스트에서는 android.icu가 스텁이라 java.text.BreakIterator로 대체한다.
 */
private fun String.graphemeBoundaries(): List<Int> = runCatching {
    val iterator = android.icu.text.BreakIterator.getCharacterInstance()
    iterator.setText(this)
    buildList {
        var boundary = iterator.next()
        while (boundary != android.icu.text.BreakIterator.DONE) {
            add(boundary)
            boundary = iterator.next()
        }
    }
}.getOrElse {
    val iterator = java.text.BreakIterator.getCharacterInstance()
    iterator.setText(this)
    buildList {
        var boundary = iterator.next()
        while (boundary != java.text.BreakIterator.DONE) {
            add(boundary)
            boundary = iterator.next()
        }
    }
}
