package com.example.anonx

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    private const val KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 16
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256

    fun encrypt(data: String, password: String): String? {
        return try {
            val salt = ByteArray(SALT_SIZE)
            SecureRandom().nextBytes(salt)

            val factory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, ALGORITHM)

            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)
            val ivParameterSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
            val encryptedData = cipher.doFinal(data.toByteArray())

            val finalPayload = salt + iv + encryptedData
            Base64.encodeToString(finalPayload, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun decrypt(encryptedPayload: String, password: String): String? {
        return try {
            val decodedPayload = Base64.decode(encryptedPayload, Base64.DEFAULT)

            val salt = decodedPayload.copyOfRange(0, SALT_SIZE)
            val iv = decodedPayload.copyOfRange(SALT_SIZE, SALT_SIZE + IV_SIZE)
            val encryptedData = decodedPayload.copyOfRange(SALT_SIZE + IV_SIZE, decodedPayload.size)

            val factory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, ALGORITHM)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val decryptedData = cipher.doFinal(encryptedData)

            String(decryptedData)
        } catch (e: Exception) {
            e.printStackTrace()
            // THE FIX: Return null on failure instead of an error string.
            null
        }
    }
}
