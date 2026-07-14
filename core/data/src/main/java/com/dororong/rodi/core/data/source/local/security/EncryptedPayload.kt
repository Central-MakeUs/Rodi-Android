package com.dororong.rodi.core.data.source.local.security

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedPayload(
    val version: Int,
    val iv: String,
    val ciphertext: String,
)
