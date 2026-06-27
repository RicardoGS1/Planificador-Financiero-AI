package com.virtualworld.easyexpensecontrol.analytics

import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.setConsent
import com.virtualworld.easyexpensecontrol.BuildConfig

/**
 * Aplica Firebase Consent Mode v2 según el estado del formulario UMP.
 */
object AnalyticsConsent {

    private const val TAG = "AnalyticsConsent"

    @Volatile
    private var analyticsStorageGranted = false

    fun isAnalyticsStorageGranted(): Boolean = analyticsStorageGranted

    fun applyDeniedByDefault() {
        analyticsStorageGranted = false
        Firebase.analytics.setAnalyticsCollectionEnabled(false)
        Firebase.analytics.setConsent {
            analyticsStorage = FirebaseAnalytics.ConsentStatus.DENIED
            adStorage = FirebaseAnalytics.ConsentStatus.DENIED
            adUserData = FirebaseAnalytics.ConsentStatus.DENIED
            adPersonalization = FirebaseAnalytics.ConsentStatus.DENIED
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Consent applied: analyticsStorage=DENIED (default)")
        }
    }

    fun applyFromUmp(context: Context) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        val canRequestAds = consentInformation.canRequestAds()
        val analyticsGranted = shouldGrantAnalyticsStorage(consentInformation)

        analyticsStorageGranted = analyticsGranted
        val adStatus = if (canRequestAds) {
            FirebaseAnalytics.ConsentStatus.GRANTED
        } else {
            FirebaseAnalytics.ConsentStatus.DENIED
        }
        val analyticsStatus = if (analyticsGranted) {
            FirebaseAnalytics.ConsentStatus.GRANTED
        } else {
            FirebaseAnalytics.ConsentStatus.DENIED
        }

        Firebase.analytics.setAnalyticsCollectionEnabled(analyticsGranted)
        Firebase.analytics.setConsent {
            analyticsStorage = analyticsStatus
            adStorage = adStatus
            adUserData = adStatus
            adPersonalization = adStatus
        }

        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "Consent applied: status=${consentInformation.consentStatus}, " +
                    "canRequestAds=$canRequestAds, analyticsStorage=$analyticsGranted"
            )
        }
    }

    private fun shouldGrantAnalyticsStorage(consentInformation: ConsentInformation): Boolean {
        return when (consentInformation.consentStatus) {
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> true
            ConsentInformation.ConsentStatus.OBTAINED -> consentInformation.canRequestAds()
            ConsentInformation.ConsentStatus.UNKNOWN,
            ConsentInformation.ConsentStatus.REQUIRED -> false
            else -> false
        }
    }
}
