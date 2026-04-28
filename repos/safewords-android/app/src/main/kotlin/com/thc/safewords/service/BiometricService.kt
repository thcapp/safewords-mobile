package com.thc.safewords.service

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wraps AndroidX BiometricPrompt for Settings → "Require Biometrics to open".
 *
 * Behavior:
 *   - canAuthenticate() returns true only if the device has a usable strong/weak biometric.
 *   - authenticate() shows the system biometric prompt; onResult fires with true on success.
 */
object BiometricService {

    fun canAuthenticate(activity: FragmentActivity): Boolean {
        val mgr = BiometricManager.from(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return mgr.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Safewords",
        subtitle: String = "Confirm it's you to view your safeword",
        onResult: (success: Boolean) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(true)
            }
            override fun onAuthenticationFailed() { /* let user retry */ }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(false)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
    }
}
