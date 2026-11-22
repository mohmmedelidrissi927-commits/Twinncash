package com.example.data.local

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PasswordHasher {
    private val random = SecureRandom()

    fun hashPassword(password: String): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val digest = sha256(salt + password.toByteArray(Charsets.UTF_8))
        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val hashB64 = Base64.getEncoder().encodeToString(digest)
        return "$saltB64:$hashB64"
    }

    fun verifyPassword(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        return try {
            val salt = Base64.getDecoder().decode(parts[0])
            val expectedHash = Base64.getDecoder().decode(parts[1])
            val actualHash = sha256(salt + password.toByteArray(Charsets.UTF_8))
            MessageDigest.isEqual(expectedHash, actualHash)
        } catch (_: Exception) {
            false
        }
    }

    private fun sha256(input: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input)
}
