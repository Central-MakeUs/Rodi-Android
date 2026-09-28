package com.dororong.rodi.feature.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BottomSheetViewportPolicyTest {
    @Test
    fun `시트가 부분 펼침이면 시트 위의 지도만 보이는 영역으로 둔다`() {
        assertEquals(380, BottomSheetViewportPolicy.bottomPaddingPx(1_000, 620f))
    }

    @Test
    fun `시트가 펼쳐지면 가려진 지도 영역을 뺀다`() {
        assertEquals(1_000, BottomSheetViewportPolicy.bottomPaddingPx(1_000, 0f))
    }

    @Test
    fun `시트가 넘쳐 올라가도 하단 여백이 지도보다 커지지 않는다`() {
        assertEquals(1_000, BottomSheetViewportPolicy.bottomPaddingPx(1_000, -80f))
    }

    @Test
    fun `시트가 숨으면 지도를 가리지 않는다`() {
        assertEquals(0, BottomSheetViewportPolicy.bottomPaddingPx(1_000, 1_000f))
    }
}
