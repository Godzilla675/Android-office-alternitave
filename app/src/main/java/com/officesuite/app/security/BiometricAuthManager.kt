package com.officesuite.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manager for biometric authentication and secure document access.
 * Supports fingerprint and face recognition authentication.
 */
class BiometricAuthManager(private val context: Context) {

    companion object {
        private const val SECURE_PREFS_NAME = "secure_documents"
        private const val KEY_LOCKED_DOCUMENTS = "locked_documents"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    /**
     * Result of authentication attempt
     */
    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val message: String, val errorCode: Int) : AuthResult()
        object Cancelled : AuthResult()
        object NotAvailable : AuthResult()
    }

    /**
     * Biometric availability status
     */
    sealed class BiometricStatus {
        object Available : BiometricStatus()
        object NotEnrolled : BiometricStatus()
        object NoHardware : BiometricStatus()
        object SecurityVulnerability : BiometricStatus()
        object Unavailable : BiometricStatus()
    }

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                SECURE_PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular shared preferences if encryption fails
            context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Check if biometric authentication is available on the device.
     */
    fun checkBiometricStatus(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.Unavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SecurityVulnerability
            else -> BiometricStatus.Unavailable
        }
    }

    /**
     * Show biometric prompt for authentication.
     * 
     * @param activity The activity to show the prompt in
     * @param title The title of the prompt
     * @param subtitle The subtitle of the prompt
     * @param negativeButtonText Text for the negative button
     * @return AuthResult indicating success or failure
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Authenticate",
        subtitle: String = "Use your biometric credential",
        negativeButtonText: String = "Cancel"
    ): AuthResult = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) {
                    continuation.resume(AuthResult.Success)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (continuation.isActive) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        continuation.resume(AuthResult.Cancelled)
                    } else {
                        continuation.resume(AuthResult.Error(errString.toString(), errorCode))
                    }
                }
            }

            override fun onAuthenticationFailed() {
                // Don't resume here - this is called for each failed attempt
                // The user can try again until they cancel or succeed
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(AuthResult.Error(e.message ?: "Authentication failed", -1))
            }
        }

        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }
    }

    /**
     * Check if biometric protection is enabled.
     */
    fun isBiometricEnabled(): Boolean {
        return securePrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    /**
     * Enable or disable biometric protection.
     */
    fun setBiometricEnabled(enabled: Boolean) {
        securePrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    /**
     * Check if a document is locked (requires biometric authentication).
     * 
     * @param documentPath The path or URI of the document
     * @return true if the document is locked
     */
    fun isDocumentLocked(documentPath: String): Boolean {
        val lockedDocs = getLockedDocuments()
        return lockedDocs.contains(documentPath)
    }

    /**
     * Lock a document (require biometric authentication to access).
     * 
     * @param documentPath The path or URI of the document
     */
    fun lockDocument(documentPath: String) {
        val lockedDocs = getLockedDocuments().toMutableSet()
        lockedDocs.add(documentPath)
        saveLockedDocuments(lockedDocs)
    }

    /**
     * Unlock a document (remove biometric protection).
     * 
     * @param documentPath The path or URI of the document
     */
    fun unlockDocument(documentPath: String) {
        val lockedDocs = getLockedDocuments().toMutableSet()
        lockedDocs.remove(documentPath)
        saveLockedDocuments(lockedDocs)
    }

    /**
     * Get list of all locked documents.
     */
    fun getLockedDocuments(): Set<String> {
        return securePrefs.getStringSet(KEY_LOCKED_DOCUMENTS, emptySet()) ?: emptySet()
    }

    private fun saveLockedDocuments(documents: Set<String>) {
        securePrefs.edit().putStringSet(KEY_LOCKED_DOCUMENTS, documents).apply()
    }
}
