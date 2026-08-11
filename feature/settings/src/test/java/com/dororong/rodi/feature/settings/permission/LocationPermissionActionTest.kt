package com.dororong.rodi.feature.settings.permission

import com.dororong.rodi.core.ui.permission.LocationPermissionAction
import com.dororong.rodi.core.ui.permission.resolveLocationPermissionAction
import com.dororong.rodi.core.ui.permission.PermissionAction
import com.dororong.rodi.core.ui.permission.resolvePermissionAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LocationPermissionActionTest {

    @Test
    fun `opens app settings when location is already granted`() {
        val result = resolveLocationPermissionAction(
            isLocationGranted = true,
            hasRequestedLocationPermission = true,
            shouldShowRationale = false,
        )

        assertEquals(LocationPermissionAction.OpenAppSettings, result)
    }

    @Test
    fun `requests system permission before the first request`() {
        val result = resolveLocationPermissionAction(
            isLocationGranted = false,
            hasRequestedLocationPermission = false,
            shouldShowRationale = false,
        )

        assertEquals(LocationPermissionAction.RequestSystemPermission, result)
    }

    @Test
    fun `requests system permission after a single denial`() {
        val result = resolveLocationPermissionAction(
            isLocationGranted = false,
            hasRequestedLocationPermission = true,
            shouldShowRationale = true,
        )

        assertEquals(LocationPermissionAction.RequestSystemPermission, result)
    }

    @Test
    fun `opens app settings after permanent denial`() {
        val result = resolveLocationPermissionAction(
            isLocationGranted = false,
            hasRequestedLocationPermission = true,
            shouldShowRationale = false,
        )

        assertEquals(LocationPermissionAction.OpenAppSettings, result)
    }

    @Test
    fun `generic permission resolver uses settings after a denied legacy permission`() {
        assertEquals(
            PermissionAction.OpenAppSettings,
            resolvePermissionAction(isGranted = false, hasRequestedPermission = true, shouldShowRationale = false),
        )
    }
}
