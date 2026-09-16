package com.dororong.rodi.feature.settings.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.driving.LiveUpdateSettings
import com.dororong.rodi.core.domain.usecase.driving.ObserveLiveUpdateSettingsUseCase
import com.dororong.rodi.core.domain.usecase.driving.SetLiveUpdateEnabledUseCase
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
    observeLiveUpdateSettings: ObserveLiveUpdateSettingsUseCase,
    private val setLiveUpdateEnabledUseCase: SetLiveUpdateEnabledUseCase,
) : ViewModel() {
    val liveUpdateSettings: Flow<LiveUpdateSettings> = observeLiveUpdateSettings()

    fun setLiveUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch { setLiveUpdateEnabledUseCase(enabled) }
    }

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
