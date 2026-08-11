package com.dororong.rodi.feature.settings.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.usecase.entry.GetLocationPermissionRequestedUseCase
import com.dororong.rodi.core.domain.usecase.entry.MarkLocationPermissionRequestedUseCase
import com.dororong.rodi.core.domain.usecase.entry.GetNotificationPermissionRequestedUseCase
import com.dororong.rodi.core.domain.usecase.entry.MarkNotificationPermissionRequestedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@HiltViewModel
class PermissionSettingsViewModel @Inject constructor(
    getLocationPermissionRequested: GetLocationPermissionRequestedUseCase,
    private val markLocationPermissionRequestedUseCase: MarkLocationPermissionRequestedUseCase,
    getNotificationPermissionRequested: GetNotificationPermissionRequestedUseCase,
    private val markNotificationPermissionRequestedUseCase: MarkNotificationPermissionRequestedUseCase,
) : ViewModel() {
    val hasRequestedLocationPermission: Flow<Boolean> = getLocationPermissionRequested()
    val hasRequestedNotificationPermission: Flow<Boolean> = getNotificationPermissionRequested()

    fun markLocationPermissionRequested() {
        viewModelScope.launch {
            markLocationPermissionRequestedUseCase()
        }
    }

    fun markNotificationPermissionRequested() {
        viewModelScope.launch { markNotificationPermissionRequestedUseCase() }
    }
}
