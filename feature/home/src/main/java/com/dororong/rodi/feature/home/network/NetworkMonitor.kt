package com.dororong.rodi.feature.home.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * `ConnectivityManager`의 기본 네트워크 콜백을 관찰해 인터넷 연결 가능 여부를 흘려보낸다.
 * 지도가 오프라인 상태에서 벗어났을 때 자동으로 재시도를 트리거하기 위해 쓴다.
 * `ACCESS_NETWORK_STATE`는 :app 매니페스트에서 선언한다(다른 feature:home 위치 코드와 동일 패턴).
 */
@SuppressLint("MissingPermission")
fun networkAvailabilityFlow(context: Context): Flow<Boolean> = callbackFlow {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            trySend(networkCapabilities.hasValidatedInternet())
        }

        override fun onLost(network: Network) {
            trySend(false)
        }
    }
    connectivityManager.registerDefaultNetworkCallback(callback)
    trySend(context.isNetworkAvailable())
    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()

/** 진입 시점에 한 번 확인하는 동기 버전. 지도 상태 초기값을 정하는 데 쓴다. */
@SuppressLint("MissingPermission")
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return true
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasValidatedInternet()
}

private fun NetworkCapabilities.hasValidatedInternet(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
