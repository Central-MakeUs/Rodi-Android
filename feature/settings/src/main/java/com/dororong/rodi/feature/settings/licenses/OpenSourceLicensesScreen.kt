package com.dororong.rodi.feature.settings.licenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.util.withContext as withLibraryContext
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.settings.SettingsTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OpenSourceLicensesScreen(onBack: () -> Unit) {
    OpenSourceLicensesContent(
        onBack = onBack,
    )
}

@Composable
private fun OpenSourceLicensesContent(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val libraries by produceState<Libs?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) {
            Libs.Builder().withLibraryContext(context).build()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        SettingsTopBar(title = "오픈소스 라이센스", onBack = onBack)
        libraries?.let {
            LibrariesContainer(
                libraries = it,
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
