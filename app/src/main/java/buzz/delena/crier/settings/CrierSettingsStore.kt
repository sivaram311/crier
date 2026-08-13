package buzz.delena.crier.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import buzz.delena.crier.gemini.GeminiTtsClient
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted, on-device only config. Mirrors the AES-GCM / Android Keystore
 * pattern already proven in forgecity-launcher's AssistantSettingsStore —
 * the API key ciphertext lives in SharedPreferences, the AES key never
 * leaves the Keystore.
 */
class CrierSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var assistantEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    val hasApiKey: Boolean
        get() = prefs.contains(KEY_API_KEY_CIPHERTEXT) && prefs.contains(KEY_API_KEY_IV)

    fun saveApiKey(value: String): Boolean {
        val key = value.trim()
        if (key.isEmpty()) {
            prefs.edit().remove(KEY_API_KEY_CIPHERTEXT).remove(KEY_API_KEY_IV).apply()
            return true
        }
        return runCatching {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val encrypted = cipher.doFinal(key.toByteArray(Charsets.UTF_8))
            prefs.edit()
                .putString(KEY_API_KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(KEY_API_KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .apply()
        }.isSuccess
    }

    fun apiKey(): String? {
        val encrypted = prefs.getString(KEY_API_KEY_CIPHERTEXT, null) ?: return null
        val iv = prefs.getString(KEY_API_KEY_IV, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(GCM_TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)),
            )
            cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    var ttsModel: String
        get() = prefs.getString(KEY_TTS_MODEL, GeminiTtsClient.DEFAULT_TTS_MODEL)
            .orEmpty()
            .ifBlank { GeminiTtsClient.DEFAULT_TTS_MODEL }
        set(value) = prefs.edit().putString(KEY_TTS_MODEL, value.trim()).apply()

    var voiceName: String
        get() = prefs.getString(KEY_VOICE, GeminiTtsClient.DEFAULT_VOICE)
            .orEmpty()
            .ifBlank { GeminiTtsClient.DEFAULT_VOICE }
        set(value) = prefs.edit().putString(KEY_VOICE, value.trim()).apply()

    var languageCode: String
        get() = prefs.getString(KEY_LANGUAGE, GeminiTtsClient.DEFAULT_LANGUAGE)
            .orEmpty()
            .ifBlank { GeminiTtsClient.DEFAULT_LANGUAGE }
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value.trim()).apply()

    var quietStartMinutes: Int
        get() = prefs.getInt(KEY_QUIET_START, 22 * 60)
        set(value) = prefs.edit().putInt(KEY_QUIET_START, value).apply()

    var quietEndMinutes: Int
        get() = prefs.getInt(KEY_QUIET_END, 7 * 60)
        set(value) = prefs.edit().putInt(KEY_QUIET_END, value).apply()

    fun allowedPackages(): Set<String> =
        prefs.getStringSet(KEY_ALLOW, emptySet())?.toSet().orEmpty()

    fun setPackageAllowed(packageName: String, allowed: Boolean) {
        val next = allowedPackages().toMutableSet()
        if (allowed) next += packageName else next -= packageName
        prefs.edit().putStringSet(KEY_ALLOW, next).apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
            ?: error("Crier API key is unavailable")
    }

    private fun getOrCreateSecretKey(): SecretKey {
        runCatching { secretKey() }.getOrNull()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val PREFS = "crier_settings"
        private const val KEY_ENABLED = "assistant_enabled"
        private const val KEY_API_KEY_CIPHERTEXT = "gemini_api_key_ciphertext"
        private const val KEY_API_KEY_IV = "gemini_api_key_iv"
        private const val KEY_TTS_MODEL = "tts_model"
        private const val KEY_VOICE = "voice_name"
        private const val KEY_LANGUAGE = "language_code"
        private const val KEY_QUIET_START = "quiet_start"
        private const val KEY_QUIET_END = "quiet_end"
        private const val KEY_ALLOW = "allowed_packages"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "crier_gemini_api_key"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }
}
