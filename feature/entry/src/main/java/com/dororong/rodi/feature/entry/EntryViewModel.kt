package com.dororong.rodi.feature.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.usecase.SetEntryCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EntryStep { LOCATION, TERMS, PRECAUTIONS, TERMS_WEBVIEW }

/**
 * 진입 게이트 단계 상태 머신. 마지막 단계 완료 시 DataStore에 완료를 저장하고 [onDone] 호출.
 */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val setEntryCompletedUseCase: SetEntryCompletedUseCase,
) : ViewModel() {

    var step by mutableStateOf(EntryStep.LOCATION)
        private set

    var webViewUrl by mutableStateOf("")
        private set

    var serviceTermsChecked by mutableStateOf(false)
        private set

    var privacyTermsChecked by mutableStateOf(false)
        private set

    var locationTermsChecked by mutableStateOf(false)
        private set

    var licenseChecked by mutableStateOf(false)
        private set

    var companionChecked by mutableStateOf(false)
        private set

    var precautionAgreementChecked by mutableStateOf(false)
        private set

    fun setAllTermsChecked(checked: Boolean) {
        serviceTermsChecked = checked
        privacyTermsChecked = checked
        locationTermsChecked = checked
    }

    fun toggleServiceTerms() {
        serviceTermsChecked = !serviceTermsChecked
    }

    fun togglePrivacyTerms() {
        privacyTermsChecked = !privacyTermsChecked
    }

    fun toggleLocationTerms() {
        locationTermsChecked = !locationTermsChecked
    }

    fun toggleLicense() {
        licenseChecked = !licenseChecked
    }

    fun toggleCompanion() {
        companionChecked = !companionChecked
    }

    fun togglePrecautionAgreement() {
        precautionAgreementChecked = !precautionAgreementChecked
    }

    fun next() {
        step = when (step) {
            EntryStep.LOCATION -> EntryStep.TERMS
            EntryStep.TERMS -> EntryStep.PRECAUTIONS
            EntryStep.PRECAUTIONS -> EntryStep.PRECAUTIONS
            EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
        }
    }

    fun openWebView(url: String) {
        webViewUrl = url
        step = EntryStep.TERMS_WEBVIEW
    }

    /** 뒤로. 첫 단계면 false(처리할 것 없음). */
    fun back(): Boolean {
        step = when (step) {
            EntryStep.PRECAUTIONS -> EntryStep.TERMS
            EntryStep.TERMS -> EntryStep.LOCATION
            EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
            EntryStep.LOCATION -> return false
        }
        return true
    }

    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            setEntryCompletedUseCase()
            onDone()
        }
    }
}
