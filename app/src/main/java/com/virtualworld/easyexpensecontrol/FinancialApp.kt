package com.virtualworld.easyexpensecontrol

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.virtualworld.easyexpensecontrol.ads.AppOpenAdManager
import com.virtualworld.easyexpensecontrol.core.util.LocaleHelper
import com.virtualworld.easyexpensecontrol.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class FinancialApp : Application() {

    private var appOpenAdManager: AppOpenAdManager? = null
    private val mobileAdsInitialized = AtomicBoolean(false)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(base))
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            // Para probar en un móvil físico con build debug sin riesgo de "invalid traffic":
            //  1. Lanza la app y haz que pida un anuncio.
            //  2. Filtra logcat por `tag:Ads` y busca un mensaje tipo:
            //       "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList(\"<HASH>\"))"
            //  3. Pega el HASH en TEST_DEVICE_HASHES (uno por dispositivo).
            // Los anuncios mostrarán la etiqueta "Test Ad" cuando esté bien configurado.
            val testDeviceIds = buildList {
                add(AdRequest.DEVICE_ID_EMULATOR)
                addAll(TEST_DEVICE_HASHES)
            }
            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
        }

        startKoin {
            androidContext(this@FinancialApp)
            modules(appModule)
        }

        // MobileAds.initialize NO se llama aquí: hay que esperar al consentimiento
        // (UMP) que se solicita desde MainActivity. Una vez resuelto, MainActivity
        // invoca [initializeMobileAdsIfNeeded].
    }

    /**
     * Inicializa MobileAds y arranca el [AppOpenAdManager]. Idempotente: sólo se ejecuta una vez.
     * Debe llamarse después de resolver el consentimiento UMP (cuando `canRequestAds()` es true).
     *
     * @param initialActivity la Activity en primer plano cuando se llama. Es necesaria para que el
     * App Open Ad pueda mostrarse en arranque en frío (cold start), porque en ese momento ya se
     * ejecutó `onActivityStarted` antes de registrar los callbacks del manager y de otra forma
     * `currentActivity` quedaría null durante la primera ventana de oportunidad de mostrar el ad.
     */
    fun initializeMobileAdsIfNeeded(initialActivity: Activity? = null) {
        val activityRef = initialActivity?.let { WeakReference(it) }
        if (!mobileAdsInitialized.compareAndSet(false, true)) return
        MobileAds.initialize(this) { initializationStatus ->
            Log.d(TAG, "AdMob SDK initialized: $initializationStatus")
            appOpenAdManager = AppOpenAdManager(this, activityRef?.get())
        }
    }

    companion object {
        private const val TAG = "FinancialApp"

        /**
         * Hashes de dispositivos físicos donde queremos forzar anuncios de test en builds DEBUG.
         * Solo aplica en DEBUG (no se usa en release). Añade aquí el hash que verás en logcat
         * (filtra por `tag:Ads`) la primera vez que un dispositivo pida un anuncio.
         *
         * Ejemplo:
         *   "33BE2250B43518CCDA7DE426D04EE231",
         */
        private val TEST_DEVICE_HASHES: List<String> = listOf(
           "2BB32931CE2CA1B41D596D003D898757"
        )
    }
}
