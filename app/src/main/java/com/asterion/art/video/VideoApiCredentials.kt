package com.asterion.art.video

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** AndroidKeystore AES-GCM for the local Nightmare API bearer credential. */
class VideoApiCredentials(ctx: Context) {
    private val store = ctx.applicationContext.getSharedPreferences("asterion_video_api", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keys.getKey(ALIAS,null)?.let { return it as SecretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }
    fun save(value: String) {
        val token = value.trim()
        require(token.matches(Regex("[0-9a-fA-F]{48}"))) { "Expected 48-character Nightmare token" }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE,key())
        val encrypted = c.doFinal(token.toByteArray(Charsets.UTF_8))
        check(store.edit()
            .putString("encrypted",Base64.encodeToString(encrypted,Base64.NO_WRAP))
            .putString("iv",Base64.encodeToString(c.iv,Base64.NO_WRAP))
            .commit()) { "Credentials could not be saved" }
    }
    fun read(): String? = runCatching {
        val encrypted=store.getString("encrypted",null) ?: return null
        val iv=store.getString("iv",null) ?: return null
        val c=Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE,key(),
            GCMParameterSpec(128,Base64.decode(iv,Base64.DEFAULT)))
        String(c.doFinal(Base64.decode(encrypted,Base64.DEFAULT)),Charsets.UTF_8)
    }.getOrNull()
    fun clear() { store.edit().clear().commit() }
    companion object { const val ALIAS="asterion_nightmare_api_v1" }
}
