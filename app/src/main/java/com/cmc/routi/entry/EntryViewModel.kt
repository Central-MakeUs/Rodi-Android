package com.cmc.routi.entry

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cmc.routi.data.EntryPreferences
import kotlinx.coroutines.launch

enum class EntryStep { LOCATION, TERMS, PRECAUTIONS, TERMS_WEBVIEW }

/**
 * 진입 게이트 단계 상태 머신. 마지막 단계 완료 시 DataStore에 완료를 저장하고 [onDone] 호출.
 */
class EntryViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = EntryPreferences(app)

    var step by mutableStateOf(EntryStep.LOCATION)
        private set

    var webViewUrl by mutableStateOf("")
        private set

    fun next() {
        step = when (step) {
            EntryStep.LOCATION -> EntryStep.TERMS
            EntryStep.TERMS -> EntryStep.PRECAUTIONS
            EntryStep.PRECAUTIONS -> EntryStep.PRECAUTIONS
            EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
        }
    }

    fun openWebView(url: String) {
        webViewUrl = url
        step = EntryStep.TERMS_WEBVIEW
    }

    /** 뒤로. 첫 단계면 false(처리할 것 없음). */
    fun back(): Boolean {
        step = when (step) {
            EntryStep.PRECAUTIONS -> EntryStep.TERMS
            EntryStep.TERMS -> EntryStep.LOCATION
            EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
            EntryStep.LOCATION -> return false
        }
        return true
    }

    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.setCompleted()
            onDone()
        }
    }
}
