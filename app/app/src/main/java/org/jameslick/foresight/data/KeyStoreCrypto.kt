package org.jameslick.foresight.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encryption at rest (spec §6): one AES-256-GCM key that never leaves the Android Keystore.
 *
 * - `encrypt` / `decrypt` wrap small secrets (contact numbers) and whole image files.
 * - `databasePassphrase` is a random 32-byte secret, generated once, stored in filesDir encrypted with the
 *   Keystore key, and handed to SQLCipher. Losing the Keystore key (factory reset) makes the data
 *   unrecoverable, which is the intended failure mode for a screening device.
 *
 * Ciphertext layout: 12-byte IV || GCM ciphertext+tag. Each call uses a fresh random IV.
 */
class KeyStoreCrypto private constructor(private val key: SecretKey) {

    fun encrypt(plain: ByteArray): ByteArray {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return iv + cipher.doFinal(plain)
    }

    fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > IV_BYTES) { "ciphertext too short" }
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES))
        return cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES)
    }

    fun encryptString(s: String): ByteArray = encrypt(s.toByteArray(Charsets.UTF_8))
    fun decryptString(blob: ByteArray): String = String(decrypt(blob), Charsets.UTF_8)

    /** The SQLCipher passphrase, created on first use. */
    fun databasePassphrase(context: Context): ByteArray {
        val f = File(context.filesDir, DB_KEY_FILE)
        if (f.exists()) return decrypt(f.readBytes())
        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        f.writeBytes(encrypt(fresh))
        return fresh
    }

    companion object {
        private const val ALIAS = "foresight-master-v1"
        private const val TRANSFORM = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val DB_KEY_FILE = "db.key"

        @Volatile private var instance: KeyStoreCrypto? = null

        fun get(): KeyStoreCrypto = instance ?: synchronized(this) {
            instance ?: KeyStoreCrypto(loadOrCreateKey()).also { instance = it }
        }

        private fun loadOrCreateKey(): SecretKey {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
            val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            gen.init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(false)   // we supply our own random IV per call
                    .build()
            )
            return gen.generateKey()
        }
    }
}
