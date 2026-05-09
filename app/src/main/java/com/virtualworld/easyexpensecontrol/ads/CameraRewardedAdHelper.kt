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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.virtualworld.easyexpensecontrol.BuildConfig
import com.virtualworld.easyexpensecontrol.R

/**
 * Muestra un rewarded ad antes de abrir la cámara o iniciar la grabación de audio.
 *
 * - Precarga el anuncio mediante [preload] (idealmente al entrar en la pantalla) para evitar latencia.
 * - Aplica un frequency cap de [MIN_INTERVAL_BETWEEN_ADS_MS] entre anuncios.
 * - Si el anuncio no está listo, no carga, falla o aún estamos dentro del cap, se llama a
 *   [onContinue] de inmediato para no bloquear el flujo del usuario.
 * - Suprime el App Open Ad mientras la app pase a background por el Intent de cámara.
 */
object CameraRewardedAdHelper {

    private const val TAG = "CameraRewardedAd"
    private const val MIN_INTERVAL_BETWEEN_ADS_MS = 5_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private var preloadedAd: RewardedAd? = null
    private var isLoading = false
    private var lastShownAtMs = 0L

    fun preload(context: Context) {
        if (preloadedAd != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            context.applicationContext,
            adUnitId(context),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad preloaded")
                    isLoading = false
                    preloadedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded preload failed: code=${error.code}, message=${error.message}, domain=${error.domain}")
                    isLoading = false
                    preloadedAd = null
                }
            }
        )
    }

    fun showThenContinue(activity: Activity, onContinue: () -> Unit) {
        AppOpenAdManager.suppressNextAppOpen()

        val now = System.currentTimeMillis()
        val tooSoon = lastShownAtMs > 0 && now - lastShownAtMs < MIN_INTERVAL_BETWEEN_ADS_MS
        val ad = preloadedAd

        if (tooSoon || ad == null) {
            Log.d(TAG, "Skipping rewarded (tooSoon=$tooSoon, preloaded=${ad != null})")
            if (!tooSoon) preload(activity)
            continueOnUiAfterAd(activity, onContinue)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed")
                ad.fullScreenContentCallback = null
                preloadedAd = null
                lastShownAtMs = System.currentTimeMillis()
                preload(activity)
                continueOnUiAfterAd(activity, onContinue)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: code=${error.code}, message=${error.message}")
                ad.fullScreenContentCallback = null
                preloadedAd = null
                preload(activity)
                continueOnUiAfterAd(activity, onContinue)
            }
        }
        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
    }

    private fun adUnitId(context: Context): String = if (BuildConfig.DEBUG) {
        context.getString(R.string.admob_rewarded_camera_test)
    } else {
        context.getString(R.string.admob_rewarded_camera)
    }

    private fun continueOnUiAfterAd(activity: Activity, onContinue: () -> Unit) {
        val run = Runnable {
            if (activity.isFinishing || activity.isDestroyed) return@Runnable
            onContinue()
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
