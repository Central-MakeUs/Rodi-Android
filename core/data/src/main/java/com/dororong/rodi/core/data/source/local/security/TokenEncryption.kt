package com.dororong.rodi.core.data.source.local.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.CancellationException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenEncryption @Inject constructor() {
    fun encrypt(plainText: String, associatedData: String): EncryptedPayload = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(associatedData.toByteArray(Charsets.UTF_8))
        EncryptedPayload(
            version = VERSION,
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(cipher.doFinal(plainText.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP),
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        throw TokenEncryptionException(exception)
    }

    fun decrypt(payload: EncryptedPayload, associatedData: String): String = try {
        if (payload.version != VERSION) throw TokenEncryptionUnsupportedVersionException(payload.version)
        val key = getKey() ?: throw TokenEncryptionMissingKeyException()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(payload.iv, Base64.NO_WRAP)),
        )
        cipher.updateAAD(associatedData.toByteArray(Charsets.UTF_8))
        String(cipher.doFinal(Base64.decode(payload.ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: TokenEncryptionException) {
        throw exception
    } catch (exception: Exception) {
        throw TokenEncryptionException(exception)
    }

    private fun getKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey
    }

    private fun getOrCreateKey(): SecretKey =
        getKey() ?: synchronized(this) { getKey() ?: generateKey() }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(KEY_SIZE_BITS)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "rodi_auth_token_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_LENGTH_BITS = 128
        const val VERSION = 1
    }
}

open class TokenEncryptionException(cause: Throwable) : RuntimeException(cause)

class TokenEncryptionMissingKeyException : TokenEncryptionException(
    IllegalStateException("Keystore key not found; session must be recreated"),
)

class TokenEncryptionUnsupportedVersionException(version: Int) : TokenEncryptionException(
    IllegalArgumentException("Unsupported token payload version: $version"),
)
