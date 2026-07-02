package com.dororong.rodi.core.data.network

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

suspend fun <T> safeApiCall(block: suspend () -> T): NetworkResult<T> =
    try {
        NetworkResult.Success(block())
    } catch (e: SocketTimeoutException) {
        NetworkResult.Failure(DataError.Network.TIMEOUT)
    } catch (e: IOException) {
        NetworkResult.Failure(DataError.Network.NO_INTERNET)
    } catch (e: HttpException) {
        val error = when (e.code()) {
            in 500..599 -> DataError.Network.SERVER
            in 400..499 -> DataError.Network.CLIENT
            else -> DataError.Network.UNKNOWN
        }
        NetworkResult.Failure(error)
    } catch (e: Exception) {
        NetworkResult.Failure(DataError.Network.UNKNOWN)
    }
