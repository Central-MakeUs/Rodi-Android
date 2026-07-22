package com.dororong.rodi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dororong.rodi.R
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.auth.LoginScreen
import com.dororong.rodi.feature.entry.EntryFlow
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RodiApp(
    viewModel: RodiAppViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val backStack = remember { mutableStateListOf<Any>() }
    var splashElapsed by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.retryPendingOnboardingSync()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        delay(1_000.milliseconds)
        splashElapsed = true
    }

    if (!state.isReady || !splashElapsed) {
        SplashScreen()
        return
    }

    LaunchedEffect(state.isEntryCompleted, state.hasGuestAccess, state.authSession.isLoggedIn) {
        if (backStack.isEmpty()) {
            val destination = if (state.authSession.isLoggedIn || state.hasGuestAccess) {
                if (state.isEntryCompleted) MainRoute else EntryRoute
            } else {
                LoginRoute
            }
            backStack.add(destination)
        }
    }

    if (backStack.isEmpty()) {
        SplashScreen()
        return
    }

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = { key ->
            when (key) {
                LoginRoute -> NavEntry(key) {
                    LoginScreen(
                        showRecentKakaoLogin = state.authSession.hasRecentKakaoLogin,
                        onNavigateNext = { isNewMember ->
                            backStack.clear()
                            val destination = when {
                                isNewMember == false -> MainRoute
                                state.isEntryCompleted -> MainRoute
                                else -> EntryRoute
                            }
                            backStack.add(destination)
                        },
                    )
                }
                EntryRoute -> NavEntry(key) {
                    EntryFlow(
                        onComplete = {
                            backStack.clear()
                            backStack.add(MainRoute)
                        },
                    )
                }
                MainRoute -> NavEntry(key) {
                    MainScreen(
                        onSessionEnded = {
                            viewModel.onSessionEnded()
                            backStack.clear()
                            backStack.add(LoginRoute)
                        },
                    )
                }
                else -> error("Unknown route: $key")
            }
        },
    )
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.offset(y = (-46.1f).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_rodi_wordmark),
                contentDescription = null,
                modifier = Modifier
                    .width(146.dp)
                    .height(45.dp),
            )
            BasicText(
                text = "운전연습의 시작, 로디",
                style = RodiTheme.typography.body1Medium.copy(
                    color = RodiTheme.colors.black,
                ),
            )
        }
    }
}
