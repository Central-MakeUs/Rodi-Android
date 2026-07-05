package com.dororong.rodi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dororong.rodi.core.data.EntryPreferences
import com.dororong.rodi.core.data.auth.AuthTokenStoreEntryPoint
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.auth.LoginScreen
import com.dororong.rodi.feature.entry.EntryFlow
import com.dororong.rodi.feature.home.HomeScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
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

    val completedValue = completed
    val isLoggedInValue = isLoggedIn
    if (completedValue == null || isLoggedInValue == null) {
        LoadingScreen()
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
        LoadingScreen()
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
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(RodiTheme.colors.white))
}
