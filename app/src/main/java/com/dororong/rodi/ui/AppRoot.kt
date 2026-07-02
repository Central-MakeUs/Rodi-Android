package com.dororong.rodi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dororong.rodi.core.data.EntryPreferences
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.entry.EntryFlow
import com.dororong.rodi.feature.home.HomeScreen

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val prefs = remember { EntryPreferences(context) }
    val completed by prefs.isCompleted.collectAsStateWithLifecycle(initialValue = null)
    val backStack = remember { mutableStateListOf<Any>() }

    val completedValue = completed
    if (completedValue == null) {
        LoadingScreen()
        return
    }

    LaunchedEffect(completedValue) {
        if (backStack.isEmpty()) {
            backStack.add(if (completedValue) HomeRoute else EntryRoute)
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
