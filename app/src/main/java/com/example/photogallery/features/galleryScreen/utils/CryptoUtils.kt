package com.example.photogallery.features.galleryScreen.utils

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val MAGIC = "PGAL"
    private const val VERSION: Byte = 1
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_LEN_BITS = 128
    private const val KEY_LEN_BITS = 256
    private const val PBKDF2_ITER = 200_000
    private val rnd = SecureRandom()

    // transforma parola in cheie
    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITER, KEY_LEN_BITS)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = skf.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
    }

    // cripteaza poza intr-un fisier .env
    fun encryptFile(
        resolver: ContentResolver,
        src: Uri,
        dstEnc: File,
        password: CharArray
    ): Boolean {
        val salt = ByteArray(SALT_LEN).also { rnd.nextBytes(it) }
        val iv   = ByteArray(IV_LEN).also { rnd.nextBytes(it) }
        val key  = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(
                Cipher.ENCRYPT_MODE,
                key,
                GCMParameterSpec(TAG_LEN_BITS, iv)
            )
        }

        resolver.openInputStream(src)?.use { input ->
            FileOutputStream(dstEnc).use { fos ->
                fos.write(MAGIC.toByteArray(Charsets.US_ASCII))
                fos.write(byteArrayOf(VERSION))
                fos.write(salt)
                fos.write(iv)
                fos.flush()

                CipherOutputStream(fos, cipher).use { cos ->
                    input.copyTo(cos)
                }
            }
        } ?: return false

        password.fill('\u0000')
        return true
    }

    // decripteaza poza
    @Throws(Exception::class)
    fun decryptToBytes(srcEnc: File, password: CharArray): ByteArray {
        FileInputStream(srcEnc).use { fis ->
            val magic = ByteArray(4)
            if (fis.read(magic) != 4 || !magic.contentEquals(MAGIC.toByteArray(Charsets.US_ASCII))) {
                error("Invalid file magic")
            }
            val ver = fis.read()
            if (ver != VERSION.toInt()) error("Unsupported version")

            val salt = ByteArray(SALT_LEN)
            fis.readFully(salt)
            val iv   = ByteArray(IV_LEN)
            fis.readFully(iv)

            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(
                    Cipher.DECRYPT_MODE,
                    key,
                    GCMParameterSpec(TAG_LEN_BITS, iv)
                )
            }

            ByteArrayOutputStream().use { bos ->
                CipherInputStream(fis, cipher).use { cis -> cis.copyTo(bos) }
                password.fill('\u0000')
                return bos.toByteArray()
            }
        }
    }

    // citeste exact N bytes din buffer
    private fun InputStream.readFully(buf: ByteArray) {
        var off = 0
        while (off < buf.size) {
            val r = this.read(buf, off, buf.size - off)
            if (r == -1) throw EOFException("Unexpected EOF")
            off += r
        }
    }
}