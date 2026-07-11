package com.dororong.rodi.core.data.source.remote.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val data: T? = null,
)
