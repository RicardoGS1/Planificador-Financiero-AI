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
import com.virtualworld.easyexpensecontrol.analytics.AnalyticsEvents
import com.virtualworld.easyexpensecontrol.analytics.AnalyticsManager

/**
 * Gestiona intersticiales en distintos placements de la app.
 *
 * Presupuestos ([showOnBudgetIfEnabled]):
 *  - Respeta [RemoteConfigManager.isInterstitialAdEnabled] y
 *    [RemoteConfigManager.getInterstitialAdFrequency].
 *
 * Guardar transacciones ([showOnAddTransactionIfEnabled]):
 *  - Respeta [RemoteConfigManager.isInterstitialAdOnAddTransactionEnabled] y
 *    [RemoteConfigManager.getInterstitialAdOnAddTransactionFrequency].
 *
 * Si el anuncio no está listo, se llama a [onDone] de inmediato para no bloquear
 * el flujo del usuario. Los frequency caps persisten entre sesiones.
 */
object InterstitialAdHelper {

    private const val TAG = "mylog_ads"
    private const val AD_TYPE = "interstitial"
    private const val PREFS_NAME = "interstitial_ad_prefs"
    private const val KEY_BUDGET_SHOW_REQUEST_COUNT = "budget_show_request_count"
    private const val KEY_ADD_TRANSACTION_SHOW_REQUEST_COUNT = "add_transaction_show_request_count"
    private const val LEGACY_SHOW_REQUEST_COUNT = "show_request_count"

    private val mainHandler = Handler(Looper.getMainLooper())

    private var loadedAd: InterstitialAd? = null
    private var isLoading = false
    private var budgetShowRequestCount = -1
    private var addTransactionShowRequestCount = -1

    fun preloadForBudget(context: Context, force: Boolean = false) {
        if (!RemoteConfigManager.isInterstitialAdEnabled()) return
        ensureBudgetCountLoaded(context)
        if (!force) {
            val frequency = RemoteConfigManager.getInterstitialAdFrequency().toInt()
            if (!shouldPreloadForNextShow(budgetShowRequestCount, frequency)) return
        }
        loadAdIfNeeded(context)
    }

    fun preloadForAddTransaction(context: Context, force: Boolean = false) {
        if (!RemoteConfigManager.isInterstitialAdOnAddTransactionEnabled()) return
        ensureAddTransactionCountLoaded(context)
        if (!force) {
            val frequency = RemoteConfigManager.getInterstitialAdOnAddTransactionFrequency().toInt()
            if (!shouldPreloadForNextShow(addTransactionShowRequestCount, frequency)) return
        }
        loadAdIfNeeded(context)
    }

    fun showOnBudgetIfEnabled(activity: Activity, onDone: () -> Unit = {}) {
        if (!RemoteConfigManager.isInterstitialAdEnabled()) {
            AnalyticsManager.current()?.logAdInterstitialSkipped(AnalyticsEvents.SKIP_DISABLED)
            onDone()
            return
        }

        ensureBudgetCountLoaded(activity)
        val frequency = RemoteConfigManager.getInterstitialAdFrequency().toInt()

        if (!shouldShowBudgetOnThisRequest(activity, frequency)) {
            Log.d(
                TAG,
                "Anuncio omitido (presupuesto): tipo=$AD_TYPE, frequencyCap=$frequency, " +
                    "requestCount=$budgetShowRequestCount"
            )
            AnalyticsManager.current()?.logAdInterstitialSkipped(AnalyticsEvents.SKIP_FREQUENCY_CAP)
            if (shouldPreloadForNextShow(budgetShowRequestCount, frequency)) {
                preloadForBudget(activity)
            }
            runOnUi(activity, onDone)
            return
        }

        Log.d(
            TAG,
            "Anuncio elegible (presupuesto): tipo=$AD_TYPE, frequency=$frequency, " +
                "requestCount=$budgetShowRequestCount"
        )

        showLoadedAd(
            activity = activity,
            onDone = onDone,
            onDismissed = {
                resetBudgetShowRequestCount(activity)
                preloadForBudget(activity)
            }
        )
    }

    fun showOnAddTransactionIfEnabled(activity: Activity, onDone: () -> Unit = {}) {
        if (!RemoteConfigManager.isInterstitialAdOnAddTransactionEnabled()) {
            onDone()
            return
        }

        ensureAddTransactionCountLoaded(activity)
        val frequency = RemoteConfigManager.getInterstitialAdOnAddTransactionFrequency().toInt()

        if (!shouldShowAddTransactionOnThisRequest(activity, frequency)) {
            Log.d(
                TAG,
                "Anuncio omitido (guardar transacción): tipo=$AD_TYPE, frequencyCap=$frequency, " +
                    "requestCount=$addTransactionShowRequestCount"
            )
            AnalyticsManager.current()?.logAdInterstitialSkipped(AnalyticsEvents.SKIP_FREQUENCY_CAP)
            if (shouldPreloadForNextShow(addTransactionShowRequestCount, frequency)) {
                preloadForAddTransaction(activity)
            }
            runOnUi(activity, onDone)
            return
        }

        Log.d(
            TAG,
            "Anuncio elegible (guardar transacción): tipo=$AD_TYPE, frequency=$frequency, " +
                "requestCount=$addTransactionShowRequestCount"
        )

        showLoadedAd(
            activity = activity,
            onDone = onDone,
            onDismissed = {
                resetAddTransactionShowRequestCount(activity)
                preloadForAddTransaction(activity)
            }
        )
    }

    private fun showLoadedAd(
        activity: Activity,
        onDone: () -> Unit,
        onDismissed: () -> Unit
    ) {
        AppOpenAdManager.suppressNextAppOpen()

        val ad = loadedAd
        if (ad == null) {
            Log.d(TAG, "Anuncio no listo: tipo=$AD_TYPE")
            AnalyticsManager.current()?.logAdInterstitialSkipped(AnalyticsEvents.SKIP_NOT_READY)
            loadAdIfNeeded(activity)
            runOnUi(activity, onDone)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Anuncio cerrado: tipo=$AD_TYPE")
                ad.fullScreenContentCallback = null
                loadedAd = null
                onDismissed()
                runOnUi(activity, onDone)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Error al mostrar: tipo=$AD_TYPE, code=${error.code}, message=${error.message}")
                ad.fullScreenContentCallback = null
                loadedAd = null
                onDismissed()
                runOnUi(activity, onDone)
            }
        }
        Log.d(TAG, "Mostrando anuncio: tipo=$AD_TYPE")
        AnalyticsManager.current()?.logAdInterstitialShown()
        ad.show(activity)
    }

    private fun loadAdIfNeeded(context: Context) {
        if (loadedAd != null || isLoading) return
        isLoading = true
        Log.d(TAG, "Iniciando carga de anuncio: tipo=$AD_TYPE")
        InterstitialAd.load(
            context.applicationContext,
            adUnitId(context),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Anuncio cargado: tipo=$AD_TYPE")
                    isLoading = false
                    loadedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(
                        TAG,
                        "Error al cargar: tipo=$AD_TYPE, code=${error.code}, " +
                            "message=${error.message}, domain=${error.domain}"
                    )
                    isLoading = false
                    loadedAd = null
                }
            }
        )
    }

    private fun shouldShowBudgetOnThisRequest(context: Context, frequency: Int): Boolean {
        if (budgetShowRequestCount >= frequency) {
            return true
        }
        budgetShowRequestCount++
        persistBudgetCount(context)
        return budgetShowRequestCount >= frequency
    }

    private fun shouldShowAddTransactionOnThisRequest(context: Context, frequency: Int): Boolean {
        if (addTransactionShowRequestCount >= frequency) {
            return true
        }
        addTransactionShowRequestCount++
        persistAddTransactionCount(context)
        return addTransactionShowRequestCount >= frequency
    }

    private fun resetBudgetShowRequestCount(context: Context) {
        budgetShowRequestCount = 0
        persistBudgetCount(context)
    }

    private fun resetAddTransactionShowRequestCount(context: Context) {
        addTransactionShowRequestCount = 0
        persistAddTransactionCount(context)
    }

    private fun shouldPreloadForNextShow(requestCount: Int, frequency: Int): Boolean {
        if (frequency <= 1) return true
        return requestCount >= frequency - 1
    }

    private fun ensureBudgetCountLoaded(context: Context) {
        if (budgetShowRequestCount >= 0) return
        val prefs = prefs(context)
        budgetShowRequestCount = when {
            prefs.contains(KEY_BUDGET_SHOW_REQUEST_COUNT) ->
                prefs.getInt(KEY_BUDGET_SHOW_REQUEST_COUNT, 0)
            prefs.contains(LEGACY_SHOW_REQUEST_COUNT) ->
                prefs.getInt(LEGACY_SHOW_REQUEST_COUNT, 0)
            else -> 0
        }
    }

    private fun persistBudgetCount(context: Context) {
        prefs(context).edit()
            .putInt(KEY_BUDGET_SHOW_REQUEST_COUNT, budgetShowRequestCount)
            .apply()
    }

    private fun ensureAddTransactionCountLoaded(context: Context) {
        if (addTransactionShowRequestCount >= 0) return
        addTransactionShowRequestCount = prefs(context)
            .getInt(KEY_ADD_TRANSACTION_SHOW_REQUEST_COUNT, 0)
    }

    private fun persistAddTransactionCount(context: Context) {
        prefs(context).edit()
            .putInt(KEY_ADD_TRANSACTION_SHOW_REQUEST_COUNT, addTransactionShowRequestCount)
            .apply()
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
