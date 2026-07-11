package com.dororong.rodi.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dororong.rodi.core.ui.terms.TermsDocument
import com.dororong.rodi.core.ui.terms.TermsWebView

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var selectedTermsDocument by remember { mutableStateOf<TermsDocument?>(null) }
    val termsDocument = selectedTermsDocument

    BackHandler {
        if (termsDocument != null) {
            selectedTermsDocument = null
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
            onTermsClick = { selectedTermsDocument = it },
        )
    }
}
