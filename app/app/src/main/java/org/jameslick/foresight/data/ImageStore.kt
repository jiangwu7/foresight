package org.jameslick.foresight.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Encrypted image files (spec §3.6, §6). Each capture's photo is PNG-encoded, AES-GCM encrypted with the
 * Keystore key, and written to filesDir/images/<captureId>.enc. The plain image never touches disk, the
 * folder is excluded from backups (data_extraction_rules.xml), and `purge` implements the 30-day retention
 * rule unless the participant consented to share.
 */
class ImageStore(context: Context, private val crypto: KeyStoreCrypto = KeyStoreCrypto.get()) {
    private val dir = File(context.filesDir, "images").apply { mkdirs() }

    fun save(captureId: String, bitmap: Bitmap): String {
        val png = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); out.toByteArray()
        }
        val f = File(dir, "$captureId.enc")
        f.writeBytes(crypto.encrypt(png))
        return f.name
    }

    fun load(fileName: String): Bitmap? {
        val f = File(dir, fileName)
        if (!f.exists()) return null
        val png = crypto.decrypt(f.readBytes())
        return BitmapFactory.decodeByteArray(png, 0, png.size)
    }

    fun delete(fileName: String?): Boolean = fileName?.let { File(dir, it).delete() } ?: false

    /** Raw decrypted bytes, for the password-protected clinic export only (spec §3.7). */
    fun loadBytes(fileName: String): ByteArray? = File(dir, fileName).takeIf { it.exists() }?.let { crypto.decrypt(it.readBytes()) }

    companion object {
        const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }
}
