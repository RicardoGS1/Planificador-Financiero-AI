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
 * - Respeta [RemoteConfigManager.isInterstitialAdEnabled] y el frequency cap remoto
 *   ([RemoteConfigManager.getInterstitialAdFrequency]): solo muestra el intersticial
 *   cada N solicitudes de [show].
 * - Si el anuncio no está listo, se llama a [onDone] de inmediato para no bloquear
 *   el flujo del usuario.
 * - El contador de solicitudes persiste entre sesiones (SharedPreferences).
 */
object InterstitialAdHelper {

    private const val TAG = "InterstitialAd"
    private const val PREFS_NAME = "interstitial_ad_prefs"
    private const val KEY_SHOW_REQUEST_COUNT = "show_request_count"

    private val mainHandler = Handler(Looper.getMainLooper())

    private var loadedAd: InterstitialAd? = null
    private var isLoading = false
    private var showRequestCount = -1

    fun preload(context: Context) {
        if (!RemoteConfigManager.isInterstitialAdEnabled()) return
        ensureCountLoaded(context)
        val frequency = RemoteConfigManager.getInterstitialAdFrequency().toInt()
        if (!shouldPreloadForNextShow(frequency)) return
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

        ensureCountLoaded(activity)
        val frequency = RemoteConfigManager.getInterstitialAdFrequency().toInt()

        if (!shouldShowOnThisRequest(activity, frequency)) {
            Log.d(
                TAG,
                "Skipping interstitial (frequency cap $frequency, requestCount=$showRequestCount)"
            )
            if (shouldPreloadForNextShow(frequency)) preload(activity)
            runOnUi(activity, onDone)
            return
        }

        Log.d(TAG, "Interstitial eligible (frequency=$frequency, requestCount=$showRequestCount)")

        AppOpenAdManager.suppressNextAppOpen()

        val ad = loadedAd
        if (ad == null) {
            Log.d(TAG, "Skipping interstitial (not loaded yet, keeping requestCount=$showRequestCount)")
            preload(activity)
            runOnUi(activity, onDone)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial dismissed")
                ad.fullScreenContentCallback = null
                loadedAd = null
                resetShowRequestCount(activity)
                preload(activity)
                runOnUi(activity, onDone)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Interstitial failed to show: code=${error.code}, message=${error.message}")
                ad.fullScreenContentCallback = null
                loadedAd = null
                resetShowRequestCount(activity)
                preload(activity)
                runOnUi(activity, onDone)
            }
        }
        ad.show(activity)
    }

    /**
     * Incrementa el contador de solicitudes y devuelve true si corresponde intentar
     * mostrar el intersticial en esta solicitud. El contador persiste entre sesiones.
     * Solo se reinicia tras mostrar el anuncio (o fallo al mostrarlo).
     */
    private fun shouldShowOnThisRequest(context: Context, frequency: Int): Boolean {
        showRequestCount++
        persistCount(context)
        return showRequestCount >= frequency
    }

    private fun resetShowRequestCount(context: Context) {
        showRequestCount = 0
        persistCount(context)
    }

    /** Precarga solo cuando falta una solicitud para llegar al umbral de frecuencia. */
    private fun shouldPreloadForNextShow(frequency: Int): Boolean {
        if (frequency <= 1) return true
        return showRequestCount == frequency - 1
    }

    private fun ensureCountLoaded(context: Context) {
        if (showRequestCount >= 0) return
        showRequestCount = prefs(context).getInt(KEY_SHOW_REQUEST_COUNT, 0)
    }

    private fun persistCount(context: Context) {
        prefs(context).edit().putInt(KEY_SHOW_REQUEST_COUNT, showRequestCount).apply()
    }

    private fun prefs(context: Context) =
        (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
