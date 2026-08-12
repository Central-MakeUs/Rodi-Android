package com.dororong.rodi.core.ui.terms

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.dororong.rodi.core.ui.theme.RodiTheme
import timber.log.Timber

@Composable
fun TermsWebView(
    url: String,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var loadError by remember(url) { mutableStateOf<String?>(null) }
    var retryCount by remember(url) { mutableIntStateOf(0) }

    DisposableEffect(view) {
        val activity = view.context.findActivity()
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatusBars = controller?.isAppearanceLightStatusBars
        val previousLightNavigationBars = controller?.isAppearanceLightNavigationBars

        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false

        onDispose {
            if (previousLightStatusBars != null) {
                controller.isAppearanceLightStatusBars = previousLightStatusBars
            }
            if (previousLightNavigationBars != null) {
                controller.isAppearanceLightNavigationBars = previousLightNavigationBars
            }
        }
    }

    if (loadError != null) {
        TermsWebViewError(
            message = loadError.orEmpty(),
            onRetry = {
                loadError = null
                retryCount = 0
            },
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColor.Black),
    ) {
        key(url) {
            var webView by remember { mutableStateOf<WebView?>(null) }

            DisposableEffect(Unit) {
                onDispose {
                    webView?.apply {
                        stopLoading()
                        webViewClient = WebViewClient()
                        destroy()
                    }
                    webView = null
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true

                            loadWithOverviewMode = true
                            useWideViewPort = true

                            javaScriptCanOpenWindowsAutomatically = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                            userAgentString = WebSettings.getDefaultUserAgent(context)
                        }

                        CookieManager.getInstance().setAcceptCookie(true)

                        webViewClient = object : WebViewClient() {
                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                Timber.e("TermsWebView error: ${error?.description}")
                                if (request?.isForMainFrame == true) {
                                    if (retryCount == 0) {
                                        retryCount = 1
                                        view?.reload()
                                    } else {
                                        loadError = error?.description?.toString() ?: "약관 페이지를 불러오지 못했어요."
                                    }
                                }
                            }
                        }

                        loadUrl(url)
                    }
                },
                update = { view ->
                    webView = view
                    if (view.url != url) view.loadUrl(url)
                },
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun TermsWebViewError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "약관 페이지를 불러오지 못했어요.",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
        )
        Text(
            text = message,
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.gray700,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Button(onClick = onRetry) {
            Text("다시 시도")
        }
    }
}

// TermsWebView 본체는 WebView라 프리뷰가 렌더되지 않는다. 에러 화면만 확인한다.
@Preview(name = "약관 로드 실패", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun TermsWebViewErrorPreview() {
    RodiTheme {
        TermsWebViewError(
            message = "net::ERR_INTERNET_DISCONNECTED",
            onRetry = {},
        )
    }
}

@Preview(name = "약관 로드 실패 - 긴 메시지", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun TermsWebViewErrorLongMessagePreview() {
    RodiTheme {
        TermsWebViewError(
            message = "약관 페이지를 불러오지 못했어요. 네트워크 상태를 확인한 뒤 다시 시도해주세요.",
            onRetry = {},
        )
    }
}
