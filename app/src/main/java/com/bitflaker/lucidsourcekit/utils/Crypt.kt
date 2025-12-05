package com.bitflaker.lucidsourcekit.utils

import android.util.Base64
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Crypt {
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun encryptString(value: String, salt: ByteArray): String {
        val spec = PBEKeySpec(value.toCharArray(), salt, 21845, 128)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    fun encryptStringBlowfish(value: String, key: ByteArray): String? {
        val sks = SecretKeySpec(key, "Blowfish")
        val cipher = Cipher.getInstance("Blowfish")
        cipher.init(Cipher.ENCRYPT_MODE, sks)
        val encrypted = cipher.doFinal(value.toByteArray())
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    @Throws(NoSuchAlgorithmException::class)
    fun generateSecretKey(): ByteArray = KeyGenerator.getInstance("Blowfish")
        .generateKey()
        .encoded
}
