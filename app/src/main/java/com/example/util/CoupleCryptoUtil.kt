package com.example.util

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@JsonClass(generateAdapter = true)
data class EncryptedPayload(
    val version: Int = 1,
    val algorithm: String = "AES-256-GCM",
    val salt: String = "",
    val iv: String = "",
    val ciphertext: String = ""
)

object CryptoBase64 {
    fun encode(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (t: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    fun decode(str: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(str.trim())
        } catch (t: Throwable) {
            android.util.Base64.decode(str.trim(), android.util.Base64.NO_WRAP)
        }
    }
}

object CoupleCryptoUtil {
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12 // 96 bits recommended for AES-GCM
    private const val SALT_LENGTH = 16 // 128 bits
    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256

    private val secureRandom = SecureRandom()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(EncryptedPayload::class.java)

    /**
     * Derives a 256-bit AES SecretKey from a passphrase and salt using PBKDF2WithHmacSHA256,
     * with SHA-256 key stretching fallback if PBKDF2 is unavailable.
     */
    fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } catch (e: Exception) {
            // Key stretching using SHA-256
            val digest = MessageDigest.getInstance("SHA-256")
            var current = digest.digest(salt + passphrase.toByteArray(Charsets.UTF_8))
            for (i in 1 until PBKDF2_ITERATIONS) {
                digest.reset()
                current = digest.digest(current)
            }
            SecretKeySpec(current, "AES")
        }
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Returns an EncryptedPayload containing the salt, IV, and ciphertext.
     */
    fun encrypt(plaintext: String, passphrase: String): EncryptedPayload {
        require(passphrase.isNotBlank()) { "Encryption passphrase cannot be empty" }
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return EncryptedPayload(
            version = 1,
            algorithm = "AES-256-GCM",
            salt = CryptoBase64.encode(salt),
            iv = CryptoBase64.encode(iv),
            ciphertext = CryptoBase64.encode(ciphertext)
        )
    }

    /**
     * Decrypts an EncryptedPayload using AES-256-GCM.
     * Throws an exception if passphrase is wrong or data was tampered with.
     */
    fun decrypt(payload: EncryptedPayload, passphrase: String): String {
        require(passphrase.isNotBlank()) { "Decryption passphrase cannot be empty" }
        require(payload.ciphertext.isNotBlank()) { "Ciphertext cannot be empty" }
        val salt = CryptoBase64.decode(payload.salt)
        val iv = CryptoBase64.decode(payload.iv)
        val ciphertext = CryptoBase64.decode(payload.ciphertext)
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Serializes plaintext into an encrypted JSON string envelope.
     */
    fun encryptToJson(plaintext: String, passphrase: String): String {
        val payload = encrypt(plaintext, passphrase)
        return payloadAdapter.toJson(payload)
    }

    /**
     * Decrypts an encrypted JSON string envelope into plaintext.
     */
    fun decryptFromJson(encryptedJson: String, passphrase: String): String {
        val payload = payloadAdapter.fromJson(encryptedJson)
            ?: throw IllegalArgumentException("Invalid encrypted payload JSON format")
        return decrypt(payload, passphrase)
    }

    /**
     * Generates a 256-bit SHA-256 hex hash of an identifier (such as email or pairing code)
     * so that plaintext identifiers are never exposed on relays or in unauthenticated storage.
     */
    fun hashIdentifier(identifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(identifier.trim().lowercase().toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
