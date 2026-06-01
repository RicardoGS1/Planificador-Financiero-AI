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
    private var pendingLoadWait: LoadWait? = null

    private data class LoadWait(
        val onReady: () -> Unit,
        val onFailed: () -> Unit
    )

    fun hasSessionAccess(): Boolean {
        return sessionUnlocked || !RemoteConfigManager.isRewardedAdEnabled()
    }

    fun isAdReady(): Boolean = preloadedAd != null

    fun isAdLoading(): Boolean = isLoading

    /**
     * Invoca [onReady] en el hilo principal cuando el anuncio está listo.
     * Si falla la carga, invoca [onFailed]. Si ya hay una precarga en curso, espera su resultado.
     */
    fun whenAdReady(context: Context, onReady: () -> Unit, onFailed: () -> Unit) {
        if (!RemoteConfigManager.isRewardedAdEnabled()) {
            mainHandler.post { onReady() }
            return
        }
        if (preloadedAd != null) {
            mainHandler.post { onReady() }
            return
        }
        pendingLoadWait = LoadWait(onReady, onFailed)
        preload(context)
    }

    fun cancelPendingLoadWait() {
        pendingLoadWait = null
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
                    val wait = pendingLoadWait
                    pendingLoadWait = null
                    if (wait != null) {
                        mainHandler.post { wait.onReady() }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(
                        TAG,
                        "Rewarded preload failed: code=${error.code}, message=${error.message}, domain=${error.domain}"
                    )
                    isLoading = false
                    preloadedAd = null
                    val wait = pendingLoadWait
                    pendingLoadWait = null
                    if (wait != null) {
                        mainHandler.post { wait.onFailed() }
                    }
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
