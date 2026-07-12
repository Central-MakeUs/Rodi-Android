package com.dororong.rodi.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dororong.rodi.core.ui.terms.TermsDocuments
import com.dororong.rodi.core.ui.terms.TermsWebView

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var selectedTermsUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val termsDocument = selectedTermsUrl?.let { url ->
        TermsDocuments.ALL.firstOrNull { it.url == url }
    }

    BackHandler {
        if (termsDocument != null) {
            selectedTermsUrl = null
        } else {
            onBack()
        }
    }

    if (termsDocument != null) {
        TermsWebView(
            url = termsDocument.url,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        SettingsContent(
            onBack = onBack,
            onTermsClick = { selectedTermsUrl = it.url },
        )
    }
}
