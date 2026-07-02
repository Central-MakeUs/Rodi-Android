package com.dororong.rodi.core.data.network

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException

suspend fun <T> safeApiCall(block: suspend () -> T): NetworkResult<T> =
    try {
        NetworkResult.Success(block())
    } catch (e: SocketTimeoutException) {
        Timber.w(e, "Network request timed out")
        NetworkResult.Failure(DataError.Network.TIMEOUT)
    } catch (e: IOException) {
        Timber.w(e, "Network request failed due to connectivity")
        NetworkResult.Failure(DataError.Network.NO_INTERNET)
    } catch (e: HttpException) {
        Timber.w(e, "Network request failed with HTTP %s", e.code())
        val error = when (e.code()) {
            in 500..599 -> DataError.Network.SERVER
            in 400..499 -> DataError.Network.CLIENT
            else -> DataError.Network.UNKNOWN
        }
        NetworkResult.Failure(error)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Unexpected network request failure")
        NetworkResult.Failure(DataError.Network.UNKNOWN)
    }
