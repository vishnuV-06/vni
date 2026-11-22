package com.vni.app.ui.security

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256 encryption helper for encrypting app backup payloads
 * before uploading to Firebase, and decrypting when downloading.
 */
object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private val FIXED_IV = ByteArray(16) { 0x42.toByte() }

    private fun getKey(secret: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(secret.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plainText: String, secretKey: String): String {
        if (plainText.isBlank()) return plainText
        val key = if (secretKey.isBlank()) "vni_default_key" else secretKey
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, getKey(key), IvParameterSpec(FIXED_IV))
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    fun decrypt(cipherText: String, secretKey: String): String {
        if (cipherText.isBlank()) return cipherText
        val key = if (secretKey.isBlank()) "vni_default_key" else secretKey
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, getKey(key), IvParameterSpec(FIXED_IV))
            val decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // If already plain text (e.g. unencrypted backup), return as is
            cipherText
        }
    }
}
