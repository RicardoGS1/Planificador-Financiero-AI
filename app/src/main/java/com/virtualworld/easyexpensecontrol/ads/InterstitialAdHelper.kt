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
 * Muestra un intersticial al navegar a ciertas pantallas (p. ej. presupuestos).
 *
 * - Precarga el anuncio mediante [preload] para evitar latencia.
 * - Aplica un frequency cap de [MIN_INTERVAL_MS] entre intersticiales.
 * - Si el anuncio no está listo o estamos dentro del cap, se llama a
 *   [onDone] de inmediato para no bloquear el flujo del usuario.
 */
object InterstitialAdHelper {

    private const val TAG = "InterstitialAd"
    private const val MIN_INTERVAL_MS = 5_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private var loadedAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShownAtMs = 0L

    fun preload(context: Context) {
        if (!RemoteConfigManager.isInterstitialAdEnabled()) return
        if (loadedAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context.applicationContext,
            adUnitId(context),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial preloaded")
                    isLoading = false
                    loadedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial preload failed: code=${error.code}, message=${error.message}, domain=${error.domain}")
                    isLoading = false
                    loadedAd = null
                }
            }
        )
    }

    fun show(activity: Activity, onDone: () -> Unit = {}) {
        if (!RemoteConfigManager.isInterstitialAdEnabled()) {
            onDone()
            return
        }

        AppOpenAdManager.suppressNextAppOpen()

        val now = System.currentTimeMillis()
        val tooSoon = lastShownAtMs > 0 && now - lastShownAtMs < MIN_INTERVAL_MS
        val ad = loadedAd

        if (tooSoon || ad == null) {
            Log.d(TAG, "Skipping interstitial (tooSoon=$tooSoon, loaded=${ad != null})")
            if (!tooSoon) preload(activity)
            runOnUi(activity, onDone)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial dismissed")
                ad.fullScreenContentCallback = null
                loadedAd = null
                lastShownAtMs = System.currentTimeMillis()
                preload(activity)
                runOnUi(activity, onDone)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Interstitial failed to show: code=${error.code}, message=${error.message}")
                ad.fullScreenContentCallback = null
                loadedAd = null
                preload(activity)
                runOnUi(activity, onDone)
            }
        }
        ad.show(activity)
    }

    private fun adUnitId(context: Context): String = if (BuildConfig.DEBUG) {
        context.getString(R.string.admob_interstitial_camera_test)
    } else {
        context.getString(R.string.admob_interstitial_camera)
    }

    private fun runOnUi(activity: Activity, action: () -> Unit) {
        val run = Runnable {
            if (activity.isFinishing || activity.isDestroyed) return@Runnable
            action()
        }
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
