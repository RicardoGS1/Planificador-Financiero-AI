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

    /**
     * Mientras sea `true`, [ProcessLifecycleOwner] NO mostrará el App Open automáticamente.
     * El SplashScreen controla la primera muestra a través de [showStartupAdIfAvailable].
     */
    @Volatile
    private var startupHandledBySplash = true

    /** Callback del SplashScreen pendiente de ser invocado cuando el ad cargue (o falle). */
    private var startupOnDone: (() -> Unit)? = null

    /** App Open de arranque en pantalla completa (el timeout del splash no cuenta en este estado). */
    @Volatile
    private var isStartupAdShowing = false

    /** El splash agotó la espera: no mostrar ni invocar el callback de startup. */
    @Volatile
    private var startupAbandoned = false

    init {
        instance = this
        // Capturamos la Activity en primer plano en cold start para que el observer de
        // ProcessLifecycleOwner —que se dispara inmediatamente al añadirse si el lifecycle
        // ya está STARTED— pueda mostrar el ad sin esperar a otro evento de Activity.
        currentActivity = initialActivity
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (startupHandledBySplash) return
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
        if (!RemoteConfigManager.isAppOpenAdEnabled()) {
            Log.d(TAG, "Carga omitida (Remote Config): tipo=$AD_TYPE")
            return
        }
        isLoadingAd = true
        Log.d(TAG, "Iniciando carga de anuncio: tipo=$AD_TYPE")
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
                    Log.d(TAG, "Anuncio cargado: tipo=$AD_TYPE")
                    isLoadingAd = false
                    appOpenAd = ad
                    loadTime = Date().time

                    val onDone = startupOnDone
                    if (onDone != null && !startupAbandoned) {
                        startupOnDone = null
                        isStartupAdShowing = true
                        currentActivity?.let { showStartupAd(it, onDone) }
                            ?: run {
                                isStartupAdShowing = false
                                finishStartup(onDone)
                            }
                        return
                    }
                    if (onDone != null) {
                        startupOnDone = null
                    }

                    if (pendingShowWhenLoaded) {
                        pendingShowWhenLoaded = false
                        currentActivity?.let { showAdIfAvailable(it) }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Error al cargar: tipo=$AD_TYPE, code=${loadAdError.code}, message=${loadAdError.message}, domain=${loadAdError.domain}")
                    isLoadingAd = false
                    pendingShowWhenLoaded = false

                    val onDone = startupOnDone
                    if (onDone != null) {
                        startupOnDone = null
                        if (!startupAbandoned) {
                            finishStartup(onDone)
                        } else {
                            clearStartupState()
                        }
                    }
                }
            }
        )
    }

    fun isStartupAdShowing(): Boolean = isStartupAdShowing

    private fun showAdIfAvailable(activity: Activity) {
        if (isShowingAd) return
        if (System.currentTimeMillis() < suppressUntilMs) {
            suppressUntilMs = 0
            return
        }
        if (!RemoteConfigManager.isAppOpenAdEnabled()) {
            Log.d(TAG, "Muestra omitida (Remote Config): tipo=$AD_TYPE")
            pendingShowWhenLoaded = false
            appOpenAd = null
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
                Log.e(TAG, "Error al mostrar: tipo=$AD_TYPE, code=${adError.code}, message=${adError.message}")
                lastDismissTimeMs = System.currentTimeMillis()
                pendingShowWhenLoaded = false
                appOpenAd?.fullScreenContentCallback = null
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

        }
        isShowingAd = true
        Log.d(TAG, "Mostrando anuncio: tipo=$AD_TYPE")
        ad.show(activity)
    }

    /**
     * Llamado desde el SplashScreen. Muestra el App Open Ad si está disponible (o espera a que
     * cargue). Invoca [onDone] una sola vez cuando el ad se cierra, falla o no hay ad disponible.
     */
    fun showStartupAdIfAvailable(activity: Activity, onDone: () -> Unit) {
        startupAbandoned = false
        if (!RemoteConfigManager.isAppOpenAdEnabled()) {
            finishStartup(onDone)
            return
        }
        if (isAdAvailable()) {
            showStartupAd(activity, onDone)
        } else if (isLoadingAd) {
            startupOnDone = onDone
        } else {
            startupOnDone = onDone
            loadAd()
        }
    }

    private fun showStartupAd(activity: Activity, onDone: () -> Unit) {
        if (startupAbandoned) {
            isStartupAdShowing = false
            clearStartupState()
            return
        }
        if (isShowingAd || activity.isFinishing || activity.isDestroyed) {
            isStartupAdShowing = false
            finishStartup(onDone)
            return
        }
        val ad = appOpenAd ?: run {
            isStartupAdShowing = false
            finishStartup(onDone)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                lastDismissTimeMs = System.currentTimeMillis()
                appOpenAd?.fullScreenContentCallback = null
                appOpenAd = null
                isShowingAd = false
                isStartupAdShowing = false
                loadAd()
                completeStartup(onDone)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Error al mostrar: tipo=$AD_TYPE_STARTUP, code=${adError.code}, message=${adError.message}")
                lastDismissTimeMs = System.currentTimeMillis()
                appOpenAd?.fullScreenContentCallback = null
                appOpenAd = null
                isShowingAd = false
                isStartupAdShowing = false
                loadAd()
                completeStartup(onDone)
            }

        }
        isShowingAd = true
        isStartupAdShowing = true
        Log.d(TAG, "Mostrando anuncio: tipo=$AD_TYPE_STARTUP")
        ad.show(activity)
    }

    private fun finishStartup(onDone: () -> Unit) {
        clearStartupState()
        onDone()
    }

    /** Tras cerrar/fallar el ad de arranque, o si el splash ya abandonó la espera. */
    private fun completeStartup(onDone: () -> Unit) {
        val invokeCallback = !startupAbandoned
        clearStartupState()
        if (invokeCallback) onDone()
    }

    private fun clearStartupState() {
        startupHandledBySplash = false
        startupOnDone = null
        isStartupAdShowing = false
        startupAbandoned = false
    }

    /**
     * Timeout del splash: deja de esperar el App Open de arranque sin invocar [startupOnDone].
     * Si el anuncio ya está visible, al cerrarlo no se navegará de nuevo desde el manager.
     */
    fun abandonStartup() {
        startupAbandoned = true
        startupHandledBySplash = false
        startupOnDone = null
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
        private const val TAG = "mylog_ads"
        private const val AD_TYPE = "app_open"
        private const val AD_TYPE_STARTUP = "app_open_startup"
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
