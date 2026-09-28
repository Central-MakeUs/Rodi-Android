package com.dororong.rodi.feature.settings.permission

import com.dororong.rodi.core.ui.permission.LocationPermissionAction
import com.dororong.rodi.core.ui.permission.resolveLocationPermissionAction
import com.dororong.rodi.core.ui.permission.PermissionAction
import com.dororong.rodi.core.ui.permission.resolvePermissionAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LocationPermissionActionTest {

    @Test
    fun `위치 권한이 이미 있으면 앱 설정을 연다`() {
        val result = resolveLocationPermissionAction(
            isLocationGranted = true,
            hasRequestedLocationPermission = true,
            shouldShowRationale = false,
        )

        assertEquals(LocationPermissionAction.OpenAppSettings, result)
    }

    @Test
    fun `처음 요청이면 시스템 권한을 요청한다`() {
        val result = resolveLocationPermissionAction(
            isLocationGranted = false,
            hasRequestedLocationPermission = false,
            shouldShowRationale = false,
        )

        assertEquals(LocationPermissionAction.RequestSystemPermission, result)
    }

    @Test
    fun `한 번 거부한 뒤에는 시스템 권한을 다시 요청한다`() {
        val result = resolveLocationPermissionAction(
            isLocationGranted = false,
            hasRequestedLocationPermission = true,
            shouldShowRationale = true,
        )

        assertEquals(LocationPermissionAction.RequestSystemPermission, result)
    }

    @Test
    fun `영구 거부한 뒤에는 앱 설정을 연다`() {
        val result = resolveLocationPermissionAction(
            isLocationGranted = false,
            hasRequestedLocationPermission = true,
            shouldShowRationale = false,
        )

        assertEquals(LocationPermissionAction.OpenAppSettings, result)
    }

    @Test
    fun `공통 권한 판정은 거부 후 설명이 필요 없으면 앱 설정을 연다`() {
        assertEquals(
            PermissionAction.OpenAppSettings,
            resolvePermissionAction(isGranted = false, hasRequestedPermission = true, shouldShowRationale = false),
        )
    }
}
