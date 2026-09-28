package com.dororong.rodi.feature.course.registration.map

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InitialCameraZoomTest {
    @Test
    fun `초기 카메라 복원에는 저장된 줌을 쓴다`() {
        assertEquals(
            9,
            initialOrDefaultZoom(
                isFirstApply = true,
                hadInitialCenter = false,
                savedZoom = 9,
                defaultZoom = 13,
            ),
        )
    }

    @Test
    fun `중심이 이미 지정됐으면 기본 줌을 쓴다`() {
        assertEquals(
            13,
            initialOrDefaultZoom(
                isFirstApply = true,
                hadInitialCenter = true,
                savedZoom = 9,
                defaultZoom = 13,
            ),
        )
    }

    @Test
    fun `검색 이동처럼 나중에 다시 중심을 잡으면 기본 줌을 쓴다`() {
        assertEquals(
            13,
            initialOrDefaultZoom(
                isFirstApply = false,
                hadInitialCenter = false,
                savedZoom = 9,
                defaultZoom = 13,
            ),
        )
    }

    @Test
    fun `저장된 줌이 없으면 기본 줌을 쓴다`() {
        assertEquals(
            13,
            initialOrDefaultZoom(
                isFirstApply = true,
                hadInitialCenter = false,
                savedZoom = null,
                defaultZoom = 13,
            ),
        )
    }
}
