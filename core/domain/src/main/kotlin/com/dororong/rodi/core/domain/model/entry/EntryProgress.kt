package com.dororong.rodi.core.domain.model.entry

data class EntryProgress(
    val mode: EntryMode = EntryMode.AUTHENTICATED,
    val step: EntryProgressStep = EntryProgressStep.TERMS,
    val webViewUrl: String = "",
    val serviceTermsChecked: Boolean = false,
    val privacyTermsChecked: Boolean = false,
    val locationTermsChecked: Boolean = false,
    val licenseChecked: Boolean = false,
    val companionChecked: Boolean = false,
    val precautionAgreementChecked: Boolean = false,
)

enum class EntryProgressStep {
    TERMS,
    NICKNAME,
    CAREER,
    PREFERENCE,
    PRECAUTIONS,
    LOCATION,
    TERMS_WEBVIEW,
}
