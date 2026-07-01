package com.dororong.rodi.core.ui.terms

data class TermsDocument(
    val title: String,
    val requiredTitle: String,
    val url: String,
)

object TermsDocuments {
    val SERVICE = TermsDocument(
        title = "서비스 이용약관",
        requiredTitle = "서비스 이용약관(필수)",
        url = "https://sites.google.com/view/rodingggggg/홈",
    )

    val PRIVACY = TermsDocument(
        title = "개인정보처리방침",
        requiredTitle = "개인정보처리방침(필수)",
        url = "https://sites.google.com/view/rodingg/홈",
    )

    val LOCATION = TermsDocument(
        title = "위치기반 서비스 이용약관",
        requiredTitle = "위치기반 서비스 이용약관(필수)",
        url = "https://sites.google.com/view/rodingggg/홈",
    )

    val ALL = listOf(SERVICE, PRIVACY, LOCATION)
}
