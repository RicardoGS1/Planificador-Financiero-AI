package com.virtualworld.easyexpensecontrol.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.virtualworld.easyexpensecontrol.analytics.AnalyticsConsent

/**
 * Gestiona el flujo de consentimiento UMP (User Messaging Platform).
 *
 * Uso:
 *  - En la primera Activity, llama a [gatherConsent]; el callback recibirá `true` cuando se
 *    pueda solicitar anuncios (porque el usuario ha aceptado o porque la región no requiere
 *    formulario). Sólo entonces debe llamarse a `MobileAds.initialize`.
 *  - Si el formulario falla o el SDK reporta error, asumimos `canRequestAds()` y, normalmente,
 *    no se cargarán anuncios — pero la app no se bloquea.
 */
object ConsentManager {

    private const val TAG = "mylog_ads"

    /** True si UMP indica que ya se pueden solicitar anuncios. */
    fun canRequestAds(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).canRequestAds()

    /**
     * Solicita la información de consentimiento y muestra el formulario si es necesario.
     * El [onConsentResolved] se invoca SIEMPRE (éxito o error) con el valor actual de
     * `canRequestAds()` para que el llamador decida si inicializar el SDK de ads.
     */
    fun gatherConsent(activity: Activity, onConsentResolved: (Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(
                            TAG,
                            "Consent form error: code=${formError.errorCode}, message=${formError.message}"
                        )
                    }
                    onConsentResolved(consentInformation.canRequestAds())
                }
            },
            { requestError ->
                Log.w(
                    TAG,
                    "Consent info update error: code=${requestError.errorCode}, message=${requestError.message}"
                )
                onConsentResolved(consentInformation.canRequestAds())
            }
        )
    }

    /**
     * Permite al usuario revisar y cambiar sus elecciones de privacidad si UMP lo permite.
     * Si el formulario de privacidad no está disponible (por ejemplo fuera de EEA/UK), no hace
     * nada visible.
     */
    fun showPrivacyOptionsForm(activity: Activity, onDone: (Boolean) -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) {
                Log.w(
                    TAG,
                    "Privacy options form error: code=${error.errorCode}, message=${error.message}"
                )
            }
            AnalyticsConsent.applyFromUmp(activity)
            onDone(canRequestAds(activity))
        }
    }
}
