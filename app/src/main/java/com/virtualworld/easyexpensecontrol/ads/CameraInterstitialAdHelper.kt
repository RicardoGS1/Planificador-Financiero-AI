package com.virtualworld.easyexpensecontrol.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.virtualworld.easyexpensecontrol.R

/**
 * Muestra un intersticial antes de abrir la cámara.
 * Si el anuncio no carga o no se puede mostrar, se llama a [onContinue] de inmediato.
 *
 * Los callbacks de AdMob pueden venir fuera del hilo principal; [ActivityResultLauncher.launch]
 * debe ejecutarse en el UI thread y tras un frame tras cerrar el anuncio para que la cámara abra bien.
 */
object CameraInterstitialAdHelper {

    private const val TAG = "CameraInterstitialAd"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun showThenContinue(activity: Activity, onContinue: () -> Unit) {
        Log.d(TAG, "Loading interstitial ad...")
        InterstitialAd.load(
            activity,
            activity.getString(R.string.admob_interstitial_camera),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded, showing...")
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad dismissed")
                            ad.fullScreenContentCallback = null
                            continueOnUiAfterAd(activity, onContinue)
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.e(TAG, "Interstitial ad failed to show: code=${error.code}, message=${error.message}")
                            ad.fullScreenContentCallback = null
                            continueOnUiAfterAd(activity, onContinue)
                        }
                    }
                    ad.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: code=${error.code}, message=${error.message}, domain=${error.domain}")
                    continueOnUiAfterAd(activity, onContinue)
                }
            }
        )
    }

    private fun continueOnUiAfterAd(activity: Activity, onContinue: () -> Unit) {
        val run = Runnable {
            if (activity.isFinishing || activity.isDestroyed) return@Runnable
            onContinue()
        }
        // Siempre al hilo principal; decorView.post deja pasar un frame tras cerrar el anuncio.
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
