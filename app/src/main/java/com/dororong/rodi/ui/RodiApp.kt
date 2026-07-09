package com.dororong.rodi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dororong.rodi.R
import com.dororong.rodi.core.data.EntryPreferences
import com.dororong.rodi.core.data.auth.AuthTokenStoreEntryPoint
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.auth.LoginScreen
import com.dororong.rodi.feature.entry.EntryFlow
import com.dororong.rodi.feature.home.HomeScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun RodiApp() {
    val context = LocalContext.current
    val prefs = remember { EntryPreferences(context) }
    val appContext = remember(context) { context.applicationContext }
    val completed by prefs.isCompleted.collectAsStateWithLifecycle(initialValue = null)
    val isLoggedIn by produceState<Boolean?>(initialValue = null, appContext) {
        value = withContext(Dispatchers.IO) {
            EntryPointAccessors.fromApplication(
                appContext,
                AuthTokenStoreEntryPoint::class.java,
            ).authTokenStore().isLoggedIn
        }
    }
    val backStack = remember { mutableStateListOf<Any>() }
    var splashElapsed by remember { mutableStateOf(false) }

    val completedValue = completed
    val isLoggedInValue = isLoggedIn

    LaunchedEffect(Unit) {
        delay(1_000)
        splashElapsed = true
    }

    if (completedValue == null || isLoggedInValue == null || !splashElapsed) {
        SplashScreen()
        return
    }

    LaunchedEffect(completedValue, isLoggedInValue) {
        if (backStack.isEmpty()) {
            val destination = if (isLoggedInValue) {
                if (completedValue) HomeRoute else EntryRoute
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
                        onNavigateNext = {
                            backStack.clear()
                            backStack.add(if (completedValue) HomeRoute else EntryRoute)
                        },
                    )
                }
                EntryRoute -> NavEntry(key) {
                    EntryFlow(
                        onComplete = {
                            backStack.clear()
                            backStack.add(HomeRoute)
                        },
                    )
                }
                HomeRoute -> NavEntry(key) {
                    HomeScreen()
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
