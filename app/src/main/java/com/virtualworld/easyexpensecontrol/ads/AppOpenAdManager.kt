package com.virtualworld.easyexpensecontrol.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.virtualworld.easyexpensecontrol.BuildConfig
import com.virtualworld.easyexpensecontrol.R
import java.util.Date

/**
 * App Open al volver al primer plano (incluye arranque en frío cuando el anuncio ya cargó).
 *
 * Para evitar que el App Open interrumpa flujos iniciados por el usuario que abren un Intent
 * externo (cámara, picker, share, etc.) llama a [suppressNextAppOpen] justo antes de lanzar
 * el Intent. Tras la duración indicada, si la app vuelve al primer plano no se mostrará el ad.
 */
class AppOpenAdManager(
    private val application: Application,
    initialActivity: Activity? = null,
) : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var currentActivity: Activity? = null
    private var loadTime: Long = 0

    /** Solo mostrar al terminar de cargar si antes pedimos mostrar (p. ej. app al primer plano sin caché). */
    private var pendingShowWhenLoaded = false

    /**
     * En arranque en frío el manager se construye DESPUÉS de que MainActivity ya recibió
     * `onActivityStarted`, por lo que `currentActivity` puede quedar `null` cuando el observer
     * de [ProcessLifecycleOwner] dispare su `onStart` inicial. Esta marca permite que el primer
     * `onActivityStarted` posterior dispare la muestra del App Open.
     */
    private var pendingShowOnNextActivity = false

    /** Tras cerrar el anuncio, ProcessLifecycleOwner vuelve a disparar onStart; ignoramos ese lapso. */
    private var lastDismissTimeMs: Long = 0

    /** Marca temporal hasta la cual no se debe mostrar el App Open (p. ej. al lanzar la cámara). */
    @Volatile
    private var suppressUntilMs: Long = 0

    init {
        instance = this
        // Capturamos la Activity en primer plano en cold start para que el observer de
        // ProcessLifecycleOwner —que se dispara inmediatamente al añadirse si el lifecycle
        // ya está STARTED— pueda mostrar el ad sin esperar a otro evento de Activity.
        currentActivity = initialActivity
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                val now = System.currentTimeMillis()
                if (now - lastDismissTimeMs < DISMISS_COOLDOWN_MS) return
                if (now < suppressUntilMs) {
                    suppressUntilMs = 0
                    return
                }
                val activity = currentActivity
                if (activity != null) {
                    showAdIfAvailable(activity)
                } else {
                    pendingShowOnNextActivity = true
                }
            }
        })
        loadAd()
    }

    private fun wasLoadTimeLessThanNHoursAgo(hours: Long): Boolean {
        val diff = Date().time - loadTime
        return diff < hours * 3_600_000L
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true
        appOpenAd = null
        val adUnitId = if (BuildConfig.DEBUG) {
            application.getString(R.string.admob_app_open_test)
        } else {
            application.getString(R.string.admob_app_open)
        }
        AppOpenAd.load(
            application,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App open ad loaded successfully")
                    isLoadingAd = false
                    appOpenAd = ad
                    loadTime = Date().time
                    if (pendingShowWhenLoaded) {
                        pendingShowWhenLoaded = false
                        currentActivity?.let { showAdIfAvailable(it) }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "App open ad failed to load: code=${loadAdError.code}, message=${loadAdError.message}, domain=${loadAdError.domain}")
                    isLoadingAd = false
                    pendingShowWhenLoaded = false
                }
            }
        )
    }

    private fun showAdIfAvailable(activity: Activity) {
        if (isShowingAd) return
        if (System.currentTimeMillis() < suppressUntilMs) {
            suppressUntilMs = 0
            return
        }
        if (!isAdAvailable()) {
            pendingShowWhenLoaded = true
            loadAd()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return

        val ad = appOpenAd ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                lastDismissTimeMs = System.currentTimeMillis()
                pendingShowWhenLoaded = false
                appOpenAd?.fullScreenContentCallback = null
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "App open ad failed to show: code=${adError.code}, message=${adError.message}")
                lastDismissTimeMs = System.currentTimeMillis()
                pendingShowWhenLoaded = false
                appOpenAd?.fullScreenContentCallback = null
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App open ad shown")
            }
        }
        isShowingAd = true
        ad.show(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (activity.javaClass.name.startsWith("com.google.android.gms.ads")) return
        currentActivity = activity
        if (pendingShowOnNextActivity) {
            pendingShowOnNextActivity = false
            val now = System.currentTimeMillis()
            if (now - lastDismissTimeMs < DISMISS_COOLDOWN_MS) return
            if (now < suppressUntilMs) {
                suppressUntilMs = 0
                return
            }
            showAdIfAvailable(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    companion object {
        private const val TAG = "AppOpenAdManager"
        private const val DISMISS_COOLDOWN_MS = 2_000L
        private const val DEFAULT_SUPPRESS_DURATION_MS = 60_000L

        @Volatile
        private var instance: AppOpenAdManager? = null

        /**
         * Suprime el App Open Ad en el siguiente regreso al primer plano dentro del intervalo dado.
         * Llamar antes de lanzar Intents externos (cámara, picker, share, etc.) que harán que la app
         * pase a background y vuelva inmediatamente — así evitamos un App Open intrusivo y posibles
         * incumplimientos de la política de AdMob.
         */
        fun suppressNextAppOpen(durationMs: Long = DEFAULT_SUPPRESS_DURATION_MS) {
            val mgr = instance ?: return
            mgr.suppressUntilMs = System.currentTimeMillis() + durationMs
        }
    }
}
