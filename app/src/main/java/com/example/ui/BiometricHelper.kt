package com.example.ui


import android.content.Context

import android.content.ContextWrapper

import androidx.biometric.BiometricManager

import androidx.biometric.BiometricPrompt

import androidx.core.content.ContextCompat

import androidx.fragment.app.FragmentActivity

import java.util.concurrent.Executor

object BiometricHelper {

    fun Context.findActivity(): FragmentActivity? {
        var currentContext = this
        while (currentContext is ContextWrapper) {
            if (currentContext is FragmentActivity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        context: Context,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val activity = context.findActivity() ?: run {
            onError("Activity not found")
            return
        }

        val executor: Executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                activity.runOnUiThread { onSuccess() }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                activity.runOnUiThread { onError(errString.toString()) }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                activity.runOnUiThread { onError("Authentication failed") }
            }
        }

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.message ?: "Error launching biometrics")
        }
    }

    fun hasDeviceSecurity(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuth = biometricManager.canAuthenticate(authenticators)
        
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        val isDeviceSecure = keyguardManager?.isDeviceSecure ?: false
        
        return (canAuth == BiometricManager.BIOMETRIC_SUCCESS) || 
               (canAuth == BiometricManager.BIOMETRIC_STATUS_UNKNOWN) || // sometimes emulator status
               isDeviceSecure
    }

    fun openSecuritySettings(context: Context) {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback 1: ACTION_SETTINGS
                val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (ex: Exception) {
                // No-op
            }
        }
    }

    fun showBiometricAndCredentialPrompt(
        context: Context,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val activity = context.findActivity() ?: run {
            onError("Activity not found")
            return
        }

        val executor: Executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                activity.runOnUiThread { onSuccess() }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                activity.runOnUiThread { onError(errString.toString()) }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                activity.runOnUiThread { onError("Authentication failed") }
            }
        }

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.message ?: "Error launching biometrics")
        }
    }
}
