package com.dororong.rodi.feature.settings.licenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.settings.SettingsTopBar

@Composable
fun OpenSourceLicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val librariesResourceId = context.resources.getIdentifier("aboutlibraries", "raw", context.packageName)
    val librariesMetadata = remember(librariesResourceId) {
        librariesResourceId.takeIf { it != 0 }?.let { resourceId ->
            context.resources.openRawResource(resourceId).use { input -> input.readBytes() }
        }
    }

    OpenSourceLicensesContent(
        onBack = onBack,
        librariesMetadata = librariesMetadata,
    )
}

@Composable
private fun OpenSourceLicensesContent(
    onBack: () -> Unit,
    librariesMetadata: ByteArray? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        SettingsTopBar(title = "오픈소스 라이센스", onBack = onBack)
        librariesMetadata?.let { metadata ->
            val libraries by produceLibraries(metadata)
            LibrariesContainer(
                libraries = libraries,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 375, heightDp = 812)
@Composable
private fun OpenSourceLicensesContentPreview() {
    RodiTheme {
        OpenSourceLicensesContent(onBack = {})
    }
}
