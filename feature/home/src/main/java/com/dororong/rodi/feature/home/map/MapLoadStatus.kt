package com.dororong.rodi.feature.home.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dororong.rodi.core.ui.network.isNetworkAvailable
import kotlinx.coroutines.delay

internal const val MAP_RETRY_DEBOUNCE_MILLIS = 1_500L

/** 오프라인이 이만큼 이어지면 지도를 덮고 안내 화면을 띄운다. */
internal const val MAP_NETWORK_ERROR_GRACE_MILLIS = 3_000L

/**
 * 홈 지도의 로딩·네트워크 오류 표시 상태.
 *
 * 최초 진입이 오프라인이어도 곧장 [MapScreenState.NetworkError]로 시작하지 않는다. 그러면
 * [awaitOfflineGrace]의 3초 유예를 건너뛰게 된다. 유예는 연결 상태 이펙트가 책임진다.
 */
@Stable
internal class MapLoadStatus(
    isOnline: Boolean,
    hasLoadedMapBefore: Boolean,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    var screenState by mutableStateOf(if (hasLoadedMapBefore) MapScreenState.Ready else MapScreenState.Loading)
        private set
    var isOnline by mutableStateOf(isOnline)
    var showNetworkSnackbar by mutableStateOf(!isOnline)
        private set

    /** 값이 바뀌면 지도 뷰를 새로 만든다. */
    var retryKey by mutableIntStateOf(0)
        private set
    private var lastRetryAtMillis = 0L

    /** 지도 뷰를 새로 만들어야 하면 true. 호출하는 쪽은 그때 이전 KakaoMap 참조를 버린다. */
    fun retry(): Boolean {
        if (!isOnline) {
            screenState = MapScreenState.NetworkError
            return false
        }
        val now = clock()
        if (now - lastRetryAtMillis < MAP_RETRY_DEBOUNCE_MILLIS) return false
        lastRetryAtMillis = now
        if (screenState != MapScreenState.Ready) screenState = MapScreenState.Loading
        retryKey += 1
        return true
    }

    /** 연결이 돌아왔을 때, 오류를 보여주고 있었다면 다시 시도한다. */
    val shouldRetryOnReconnect: Boolean
        get() = screenState == MapScreenState.NetworkError || showNetworkSnackbar

    /**
     * 끊기자마자 지도를 덮으면 잠깐 끊겼다 붙는 구간에서 화면이 번쩍인다. 토스트는 바로,
     * 안내 화면은 유예 시간을 넘겨 계속 끊겨 있을 때만 덮는다(iOS와 동일).
     * 연결이 돌아오면 호출한 이펙트가 재시작되며 delay가 취소돼 원래 화면으로 돌아온다.
     */
    suspend fun awaitOfflineGrace() {
        showNetworkSnackbar = true
        delay(MAP_NETWORK_ERROR_GRACE_MILLIS)
        screenState = MapScreenState.NetworkError
    }

    /**
     * SDK 초기화·렌더링 실패도 이 콜백을 타므로, 온라인 상태에서까지 "네트워크 연결이 원활하지
     * 않아요"로 안내하면 원인과 다른 메시지가 뜬다.
     */
    fun onMapError() {
        if (isOnline) {
            showNetworkSnackbar = false
            screenState = MapScreenState.Error
        } else {
            // 오프라인 안내 화면 전환은 3초 유예를 갖고 있는 awaitOfflineGrace에 맡긴다.
            showNetworkSnackbar = true
        }
    }

    /** 지도가 실제로 그려졌으면 true. 오프라인에서 캐시로 그려진 경우는 로드 완료로 치지 않는다. */
    fun onMapRendered(): Boolean {
        if (!isOnline) return false
        screenState = MapScreenState.Ready
        showNetworkSnackbar = false
        return true
    }
}

@Composable
internal fun rememberMapLoadStatus(context: Context): MapLoadStatus = remember {
    MapLoadStatus(
        isOnline = context.isNetworkAvailable(),
        hasLoadedMapBefore = hasLoadedMapInSession || context.hasLoadedMapBefore(),
    )
}
