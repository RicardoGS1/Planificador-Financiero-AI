package com.virtualworld.easyexpensecontrol.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.virtualworld.easyexpensecontrol.R
import java.util.Date

/**
 * App Open al volver al primer plano (incluye arranque en frío cuando el anuncio ya cargó).
 */
class AppOpenAdManager(private val application: Application) : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var currentActivity: Activity? = null
    private var loadTime: Long = 0

    /** Solo mostrar al terminar de cargar si antes pedimos mostrar (p. ej. app al primer plano sin caché). */
    private var pendingShowWhenLoaded = false

    /** Tras cerrar el anuncio, ProcessLifecycleOwner vuelve a disparar onStart; ignoramos ese lapso. */
    private var lastDismissTimeMs: Long = 0

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (System.currentTimeMillis() - lastDismissTimeMs < DISMISS_COOLDOWN_MS) return
                currentActivity?.let { showAdIfAvailable(it) }
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
        AppOpenAd.load(
            application,
            application.getString(R.string.admob_app_open),
            AdRequest.Builder().build(),
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoadingAd = false
                    appOpenAd = ad
                    loadTime = Date().time
                    if (pendingShowWhenLoaded) {
                        pendingShowWhenLoaded = false
                        currentActivity?.let { showAdIfAvailable(it) }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                }
            }
        )
    }

    private fun showAdIfAvailable(activity: Activity) {
        if (isShowingAd) return
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
                lastDismissTimeMs = System.currentTimeMillis()
                pendingShowWhenLoaded = false
                appOpenAd?.fullScreenContentCallback = null
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
            }
        }
        isShowingAd = true
        ad.show(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (activity.javaClass.name.startsWith("com.google.android.gms.ads")) return
        currentActivity = activity
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
        private const val DISMISS_COOLDOWN_MS = 1_000L
    }
}
