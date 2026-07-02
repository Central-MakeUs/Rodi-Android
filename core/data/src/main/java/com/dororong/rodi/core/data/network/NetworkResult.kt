package com.dororong.rodi.core.data.network

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Failure(val error: DataError) : NetworkResult<Nothing>
}
