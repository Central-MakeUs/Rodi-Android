package com.dororong.rodi.core.ui.terms

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.dororong.rodi.core.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsWebViewScreen(
    title: String,
    url: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_left),
                            contentDescription = "뒤로",
                        )
                    }
                },
            )
        }
    ) { padding ->
        TermsWebView(
            url = url,
            modifier = Modifier.padding(padding),
        )
    }
}
