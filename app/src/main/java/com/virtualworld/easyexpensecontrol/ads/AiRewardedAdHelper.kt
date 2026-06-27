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
import com.virtualworld.easyexpensecontrol.analytics.AnalyticsEvents
import com.virtualworld.easyexpensecontrol.analytics.AnalyticsManager

/**
 * Controla el acceso a las funciones de IA mediante un rewarded ad por sesión de app.
 *
 * - La primera vez que el usuario intenta usar IA en una sesión debe ver un anuncio recompensado.
 * - Tras obtener la recompensa, el acceso queda desbloqueado hasta que se cierre el proceso.
 * - Si los rewarded ads están desactivados en Remote Config, el acceso es libre.
 */
object AiRewardedAdHelper {

    private const val TAG = "mylog_ads"
    private const val AD_TYPE = "rewarded_ai"
    const val LOAD_TIMEOUT_MS = 15_000L
    const val LOAD_TIMEOUT_SECONDS = (LOAD_TIMEOUT_MS / 1000).toInt()

    private val mainHandler = Handler(Looper.getMainLooper())

    private var preloadedAd: RewardedAd? = null
    private var isLoading = false
    private var sessionUnlocked = false
    private var pendingLoadWait: LoadWait? = null
    private var loadTimeoutRunnable: Runnable? = null

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
        scheduleLoadTimeout()
        preload(context)
    }

    fun cancelPendingLoadWait() {
        pendingLoadWait = null
        cancelLoadTimeout()
    }

    private fun scheduleLoadTimeout() {
        cancelLoadTimeout()
        val runnable = Runnable {
            loadTimeoutRunnable = null
            if (preloadedAd != null || pendingLoadWait == null) return@Runnable
            Log.w(TAG, "Timeout precarga: tipo=$AD_TYPE, ms=$LOAD_TIMEOUT_MS")
            isLoading = false
            val wait = pendingLoadWait
            pendingLoadWait = null
            if (wait != null) {
                mainHandler.post { wait.onFailed() }
            }
        }
        loadTimeoutRunnable = runnable
        mainHandler.postDelayed(runnable, LOAD_TIMEOUT_MS)
    }

    private fun cancelLoadTimeout() {
        loadTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        loadTimeoutRunnable = null
    }

    private fun completeLoadWait(onReady: Boolean) {
        cancelLoadTimeout()
        val wait = pendingLoadWait
        pendingLoadWait = null
        if (wait != null) {
            mainHandler.post {
                if (onReady) wait.onReady() else wait.onFailed()
            }
        }
    }

    fun preload(context: Context) {
        if (!RemoteConfigManager.isRewardedAdEnabled()) return
        if (preloadedAd != null || isLoading) return
        isLoading = true
        val unitId = adUnitId(context)
        Log.d(
            TAG,
            "Iniciando carga de anuncio: tipo=$AD_TYPE, unitId=$unitId, debug=${BuildConfig.DEBUG}"
        )
        RewardedAd.load(
            context.applicationContext,
            unitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Anuncio cargado: tipo=$AD_TYPE, unitId=$unitId")
                    isLoading = false
                    preloadedAd = ad
                    completeLoadWait(onReady = true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    logLoadFailure(unitId, error)
                    isLoading = false
                    preloadedAd = null
                    completeLoadWait(onReady = false)
                }
            }
        )
    }

    private fun logLoadFailure(unitId: String, error: LoadAdError) {
        Log.e(
            TAG,
            "Error al cargar: tipo=$AD_TYPE, unitId=$unitId, code=${error.code}, " +
                "message=${error.message}, domain=${error.domain}"
        )
        error.cause?.let { cause ->
            Log.e(
                TAG,
                "Error al cargar: tipo=$AD_TYPE, cause=" +
                    "code=${cause.code}, message=${cause.message}, domain=${cause.domain}"
            )
        }
        val responseInfo = error.responseInfo
        if (responseInfo == null) {
            Log.e(TAG, "Error al cargar: tipo=$AD_TYPE, responseInfo=null")
            return
        }
        Log.e(
            TAG,
            "Error al cargar: tipo=$AD_TYPE, responseId=${responseInfo.responseId}, " +
                "mediationAdapter=${responseInfo.mediationAdapterClassName}, " +
                "adapterResponses=${responseInfo.adapterResponses.size}"
        )
        responseInfo.adapterResponses.forEachIndexed { index, adapter ->
            Log.e(
                TAG,
                "Error al cargar: tipo=$AD_TYPE, adapter[$index]=" +
                    "class=${adapter.adapterClassName}, " +
                    "latencyMs=${adapter.latencyMillis}, " +
                    "adSourceName=${adapter.adSourceName}, " +
                    "adSourceId=${adapter.adSourceId}, " +
                    "adSourceInstanceName=${adapter.adSourceInstanceName}, " +
                    "adSourceInstanceId=${adapter.adSourceInstanceId}"
            )
            adapter.adError?.let { adapterError ->
                Log.e(
                    TAG,
                    "Error al cargar: tipo=$AD_TYPE, adapter[$index] adError=" +
                        "code=${adapterError.code}, message=${adapterError.message}, " +
                        "domain=${adapterError.domain}"
                )
            }
        }
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
            Log.d(TAG, "Anuncio no listo: tipo=$AD_TYPE")
            onAdFailed()
            return
        }

        var rewardEarned = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Anuncio cerrado: tipo=$AD_TYPE, rewardEarned=$rewardEarned")
                ad.fullScreenContentCallback = null
                preloadedAd = null
                preload(activity)
                continueOnUiAfterAd(activity) {
                    if (rewardEarned) {
                        AnalyticsManager.current()?.logAdRewardedCompleted(AnalyticsEvents.PLACEMENT_AI)
                        onGranted()
                    } else {
                        AnalyticsManager.current()?.logAdRewardedDismissed(AnalyticsEvents.PLACEMENT_AI)
                        onAdNotCompleted()
                    }
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Error al mostrar: tipo=$AD_TYPE, code=${error.code}, message=${error.message}")
                ad.fullScreenContentCallback = null
                preloadedAd = null
                preload(activity)
                continueOnUiAfterAd(activity, onAdFailed)
            }
        }

        Log.d(TAG, "Mostrando anuncio: tipo=$AD_TYPE")
        ad.show(activity) { rewardItem ->
            Log.d(TAG, "Recompensa obtenida: tipo=$AD_TYPE, amount=${rewardItem.amount}, rewardType=${rewardItem.type}")
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
