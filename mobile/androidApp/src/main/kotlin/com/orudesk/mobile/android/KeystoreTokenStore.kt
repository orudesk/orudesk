package com.orudesk.mobile.android

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.orudesk.mobile.auth.TokenStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The refresh token, encrypted with AES-GCM under a key that lives in the Android Keystore (Day 73). The key
 * never leaves the secure hardware (or the Keystore's process), so a copy of the app's files, from a backup or
 * a rooted phone's file system, holds only ciphertext.
 *
 * Same format idea as the API's secret box: base64(iv || ciphertext), in the app's private preferences.
 */
class KeystoreTokenStore(context: Context) : TokenStore {

    private val prefs = context.getSharedPreferences("orudesk-auth", Context.MODE_PRIVATE)

    override fun load(): String? {
        val sealed = prefs.getString(KEY, null) ?: return null
        return runCatching {
            val bytes = Base64.decode(sealed, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_SIZE))
            String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE), Charsets.UTF_8)
        }.getOrElse {
            clear()   // the key was reset (a new phone, cleared credentials): sign in again
            null
        }
    }

    override fun save(refreshToken: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())   // the Keystore picks a fresh random IV
        val sealed = cipher.iv + cipher.doFinal(refreshToken.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(KEY, Base64.encodeToString(sealed, Base64.NO_WRAP)).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "orudesk-refresh-token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val KEY = "refresh-token"
    }
}
