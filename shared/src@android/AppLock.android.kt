package com.zerotwodev.recall

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager.Authenticators
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import com.zerotwodev.recall.domain.AppLock
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

/** The system prompt offers fingerprint or face when set up, and the phone's PIN, pattern or password. */
class AndroidAppLock(private val context: Context) : AppLock {
    private var activity = WeakReference<Activity>(null)

    fun attach(activity: Activity?) {
        this.activity = WeakReference(activity)
    }

    override fun canLock(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.getSystemService(KeyguardManager::class.java).isDeviceSecure

    override suspend fun unlock(): Boolean {
        val host = activity.get() ?: return false
        if (!canLock()) return false
        return suspendCancellableCoroutine { continuation ->
            val builder = BiometricPrompt.Builder(host)
                .setTitle("Unlock Recall")
                .setSubtitle("Your memories are locked")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
            } else {
                @Suppress("DEPRECATION")
                builder.setDeviceCredentialAllowed(true)
            }
            val cancel = CancellationSignal()
            continuation.invokeOnCancellation { cancel.cancel() }
            builder.build().authenticate(
                cancel,
                host.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                    // A failed fingerprint try keeps the prompt open, so there is nothing to do here.
                },
            )
        }
    }
}
