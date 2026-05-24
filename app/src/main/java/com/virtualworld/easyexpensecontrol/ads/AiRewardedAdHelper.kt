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
 * Controla el acceso a las funciones de IA mediante un rewarded ad por sesión de app.
 *
 * - La primera vez que el usuario intenta usar IA en una sesión debe ver un anuncio recompensado.
 * - Tras obtener la recompensa, el acceso queda desbloqueado hasta que se cierre el proceso.
 * - Si los rewarded ads están desactivados en Remote Config, el acceso es libre.
 */
object AiRewardedAdHelper {

    private const val TAG = "AiRewardedAd"

    private val mainHandler = Handler(Looper.getMainLooper())

    private var preloadedAd: RewardedAd? = null
    private var isLoading = false
    private var sessionUnlocked = false

    fun hasSessionAccess(): Boolean {
        return sessionUnlocked || !RemoteConfigManager.isRewardedAdEnabled()
    }

    fun preload(context: Context) {
        if (!RemoteConfigManager.isRewardedAdEnabled()) return
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
                    Log.e(
                        TAG,
                        "Rewarded preload failed: code=${error.code}, message=${error.message}, domain=${error.domain}"
                    )
                    isLoading = false
                    preloadedAd = null
                }
            }
        )
    }

    /**
     * Muestra el rewarded ad tras la confirmación del usuario en el diálogo informativo.
     * [onGranted] se invoca solo si el usuario obtiene la recompensa (ve el anuncio completo).
     */
    fun showForSessionAccess(
        activity: Activity,
        onGranted: () -> Unit,
        onAdFailed: () -> Unit,
        onAdNotCompleted: () -> Unit
    ) {
        if (hasSessionAccess()) {
            onGranted()
            return
        }

        if (!RemoteConfigManager.isRewardedAdEnabled()) {
            sessionUnlocked = true
            onGranted()
            return
        }

        AppOpenAdManager.suppressNextAppOpen()

        val ad = preloadedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded ad not ready for AI access")
            preload(activity)
            onAdFailed()
            return
        }

        var rewardEarned = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed (rewardEarned=$rewardEarned)")
                ad.fullScreenContentCallback = null
                preloadedAd = null
                preload(activity)
                continueOnUiAfterAd(activity) {
                    if (rewardEarned) onGranted() else onAdNotCompleted()
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: code=${error.code}, message=${error.message}")
                ad.fullScreenContentCallback = null
                preloadedAd = null
                preload(activity)
                continueOnUiAfterAd(activity, onAdFailed)
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            rewardEarned = true
            sessionUnlocked = true
        }
    }

    private fun adUnitId(context: Context): String = if (BuildConfig.DEBUG) {
        context.getString(R.string.admob_rewarded_ai_test)
    } else {
        context.getString(R.string.admob_rewarded_ai)
    }

    private fun continueOnUiAfterAd(activity: Activity, action: () -> Unit) {
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
