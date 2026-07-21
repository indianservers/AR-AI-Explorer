package com.indianservers.aiexplorer.assistant.privacy

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.Base64

enum class SharedDataCategory { CONFIRMED_QUESTION_TEXT, SELECTED_CAMERA_CROP, CONFIRMED_VOICE_TRANSCRIPT, SELECTED_LEARNER_STEPS, CONCEPT_AND_LEVEL, SELECTED_VISUAL_STATE }
data class AssistantConsent(
    val cloudEnabled: Boolean = false,
    val cameraMayBeSent: Boolean = false,
    val voiceMayBeSent: Boolean = false,
    val learnerStepsMayBeSent: Boolean = false,
    val allowedCategories: Set<SharedDataCategory> = setOf(SharedDataCategory.CONFIRMED_QUESTION_TEXT, SharedDataCategory.CONCEPT_AND_LEVEL),
)
enum class ProviderKind { NONE, GROQ, OPENAI, ANTHROPIC }
data class ProviderSettings(val selectedProvider: ProviderKind = ProviderKind.NONE, val consent: AssistantConsent = AssistantConsent())

interface SecureSecretStore { fun put(providerId: String, secret: CharArray); fun get(providerId: String): CharArray?; fun delete(providerId: String) }

/** API keys are encrypted with a non-exportable Android Keystore AES key before preferences storage. */
class AndroidKeystoreSecretStore(private val context: Context) : SecureSecretStore {
    private val prefs = context.getSharedPreferences("assistant_encrypted_secrets", Context.MODE_PRIVATE)
    private val alias = "aiexplorer_assistant_provider_key_v1"
    override fun put(providerId: String, secret: CharArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val payload = cipher.doFinal(String(secret).toByteArray(Charsets.UTF_8))
        secret.fill('\u0000')
        prefs.edit().putString(providerId, Base64.getEncoder().encodeToString(cipher.iv + payload)).apply()
    }
    override fun get(providerId: String): CharArray? {
        val bytes = prefs.getString(providerId, null)?.let(Base64.getDecoder()::decode) ?: return null
        if (bytes.size <= 12) return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8).toCharArray()
    }
    override fun delete(providerId: String) { prefs.edit().remove(providerId).apply() }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
            generateKey()
        }
    }
}

object PrivacyGate {
    fun shareableCategories(requestHasCamera: Boolean, requestHasVoice: Boolean, requestHasSteps: Boolean, consent: AssistantConsent): Set<SharedDataCategory> {
        if (!consent.cloudEnabled) return emptySet()
        return consent.allowedCategories.filterTo(mutableSetOf()) { category -> when (category) {
            SharedDataCategory.SELECTED_CAMERA_CROP -> requestHasCamera && consent.cameraMayBeSent
            SharedDataCategory.CONFIRMED_VOICE_TRANSCRIPT -> requestHasVoice && consent.voiceMayBeSent
            SharedDataCategory.SELECTED_LEARNER_STEPS -> requestHasSteps && consent.learnerStepsMayBeSent
            else -> true
        } }
    }
}
