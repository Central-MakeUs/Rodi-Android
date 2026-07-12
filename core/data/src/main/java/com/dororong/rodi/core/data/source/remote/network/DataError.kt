package com.dororong.rodi.core.data.source.remote.network

sealed interface DataError {
    enum class Network : DataError {
        NO_INTERNET,
        TIMEOUT,
        SERVER,
        CLIENT,
        UNKNOWN,
    }
}
