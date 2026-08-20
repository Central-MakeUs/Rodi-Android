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

/**
 * 서버가 글자 수를 [String.length](UTF-16 code unit)로 검증할 때 쓴다.
 *
 * 이모지는 grapheme 1개라도 code unit은 2개 이상이라, grapheme 기준으로만 자르면
 * 화면엔 "30/30"인데 서버에선 길이 초과로 거부당한다. 이모지 시퀀스를 중간에서
 * 깨지 않으면서 code unit 합이 [maxCodeUnits]를 넘지 않는 지점까지만 남긴다.
 */
fun String.takeGraphemesWithinCodeUnits(maxCodeUnits: Int): String {
    if (maxCodeUnits <= 0) return ""
    if (length <= maxCodeUnits) return this
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)
    var boundary = 0
    var next = iterator.next()
    while (next != BreakIterator.DONE && next <= maxCodeUnits) {
        boundary = next
        next = iterator.next()
    }
    return substring(0, boundary)
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
