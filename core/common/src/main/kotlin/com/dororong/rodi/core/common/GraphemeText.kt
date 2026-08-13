package com.dororong.rodi.core.common

import java.text.BreakIterator

/**
 * UTF-16 code unit이 아니라 사용자가 인지하는 문자(grapheme cluster) 단위로 길이를 센다.
 * 서로게이트 쌍으로 이뤄진 이모지(예: 😁)나 ZWJ로 묶인 이모지 시퀀스도 하나로 센다 —
 * [String.length]는 이런 이모지를 2개 이상으로 센다.
 */
fun String.graphemeLength(): Int {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)
    var count = 0
    while (iterator.next() != BreakIterator.DONE) count++
    return count
}

/** 사용자가 인지하는 문자 [maxLength]개까지만 남기고 자른다. 이모지 시퀀스를 중간에서 깨지 않는다. */
fun String.takeGraphemes(maxLength: Int): String {
    if (maxLength <= 0) return ""
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)
    var boundary = 0
    var count = 0
    var next = iterator.next()
    while (next != BreakIterator.DONE && count < maxLength) {
        boundary = next
        count++
        next = iterator.next()
    }
    return substring(0, boundary)
}
