package com.virtualworld.easyexpensecontrol.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.virtualworld.easyexpensecontrol.BuildConfig
import com.virtualworld.easyexpensecontrol.R

/**
 * Muestra un intersticial antes de abrir la cámara.
 *
 * - Precarga el anuncio mediante [preload] (idealmente al entrar en la pantalla) para evitar latencia.
 * - Aplica un frequency cap de [MIN_INTERVAL_BETWEEN_ADS_MS] entre intersticiales.
 * - Si el anuncio no está listo, no carga, falla o aún estamos dentro del cap, se llama a
 *   [onContinue] de inmediato para no bloquear el flujo del usuario.
 * - Suprime el App Open Ad mientras la app pase a background por el Intent de cámara.
 *
 * Los callbacks de AdMob pueden venir fuera del hilo principal; [ActivityResultLauncher.launch]
 * debe ejecutarse en el UI thread y tras un frame al cerrar el anuncio para que la cámara abra bien.
 */
object CameraInterstitialAdHelper {

    private const val TAG = "CameraInterstitialAd"
    private const val MIN_INTERVAL_BETWEEN_ADS_MS = 5_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private var preloadedAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShownAtMs = 0L

    /** Precarga el intersticial si no hay uno listo ya. Llamar al entrar a la pantalla. */
    fun preload(context: Context) {
        if (preloadedAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context.applicationContext,
            adUnitId(context),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad preloaded")
                    isLoading = false
                    preloadedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial preload failed: code=${error.code}, message=${error.message}, domain=${error.domain}")
                    isLoading = false
                    preloadedAd = null
                }
            }
        )
    }

    fun showThenContinue(activity: Activity, onContinue: () -> Unit) {
        // Vamos a abrir un Intent externo (cámara). Evita que el App Open Ad
        // aparezca al volver al primer plano.
        AppOpenAdManager.suppressNextAppOpen()

        val now = System.currentTimeMillis()
        val tooSoon = lastShownAtMs > 0 && now - lastShownAtMs < MIN_INTERVAL_BETWEEN_ADS_MS
        val ad = preloadedAd

        if (tooSoon || ad == null) {
            Log.d(TAG, "Skipping interstitial (tooSoon=$tooSoon, preloaded=${ad != null})")
            // Aprovechamos para preparar el siguiente intersticial.
            if (!tooSoon) preload(activity)
            continueOnUiAfterAd(activity, onContinue)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                ad.fullScreenContentCallback = null
                preloadedAd = null
                lastShownAtMs = System.currentTimeMillis()
                preload(activity)
                continueOnUiAfterAd(activity, onContinue)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: code=${error.code}, message=${error.message}")
                ad.fullScreenContentCallback = null
                preloadedAd = null
                preload(activity)
                continueOnUiAfterAd(activity, onContinue)
            }
        }
        ad.show(activity)
    }

    private fun adUnitId(context: Context): String = if (BuildConfig.DEBUG) {
        context.getString(R.string.admob_interstitial_camera_test)
    } else {
        context.getString(R.string.admob_interstitial_camera)
    }

    private fun continueOnUiAfterAd(activity: Activity, onContinue: () -> Unit) {
        val run = Runnable {
            if (activity.isFinishing || activity.isDestroyed) return@Runnable
            onContinue()
        }
        // Siempre al hilo principal; decorView.post deja pasar un frame tras cerrar el anuncio.
        mainHandler.post {
            if (activity.isFinishing || activity.isDestroyed) return@post
            val decor = activity.window?.decorView
            if (decor != null) {
                decor.post(run)
            } else {
                run.run()
            }
        }
    }
}
