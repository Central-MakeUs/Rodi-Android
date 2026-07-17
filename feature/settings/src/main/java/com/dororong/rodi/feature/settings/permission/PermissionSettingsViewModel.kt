package com.dororong.rodi.feature.settings.permission

import androidx.lifecycle.ViewModel
import com.dororong.rodi.core.domain.usecase.entry.GetLocationPermissionRequestedUseCase
import com.dororong.rodi.core.domain.usecase.entry.MarkLocationPermissionRequestedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class PermissionSettingsViewModel @Inject constructor(
    getLocationPermissionRequested: GetLocationPermissionRequestedUseCase,
    private val markLocationPermissionRequestedUseCase: MarkLocationPermissionRequestedUseCase,
) : ViewModel() {
    val hasRequestedLocationPermission: Flow<Boolean> = getLocationPermissionRequested()

    suspend fun markLocationPermissionRequested() {
        markLocationPermissionRequestedUseCase()
    }
}
