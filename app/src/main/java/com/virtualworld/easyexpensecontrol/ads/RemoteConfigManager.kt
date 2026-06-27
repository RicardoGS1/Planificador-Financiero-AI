package com.virtualworld.easyexpensecontrol.ads

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.virtualworld.easyexpensecontrol.BuildConfig
import com.virtualworld.easyexpensecontrol.R

/**
 * Wrapper centralizado de Firebase Remote Config.
 *
 * Carga los valores por defecto desde res/xml/remote_config_defaults.xml y dispara un
 * fetchAndActivate al inicializar la app. Las consultas (p. ej. [isAppOpenAdEnabled])
 * son síncronas y devuelven el último valor activado, o el default si aún no se ha
 * recibido respuesta del servidor.
 *
 * Claves expuestas:
 *  - [KEY_APP_OPEN_AD_ENABLED]: activa/desactiva el App Open Ad de forma remota.
 *  - [KEY_INTERSTITIAL_AD_ENABLED]: activa/desactiva el intersticial de forma remota.
 *  - [KEY_INTERSTITIAL_AD_ADD_TRANSACTION_ENABLED]: activa/desactiva el intersticial al
 *    guardar transacciones y volver a la pantalla anterior.
 *  - [KEY_INTERSTITIAL_AD_FREQUENCY]: cada cuántas solicitudes de [InterstitialAdHelper.show]
 *    se muestra realmente el intersticial (p. ej. 4 = mostrar 1 de cada 4 solicitudes).
 *  - [KEY_REWARDED_AD_ENABLED]: activa/desactiva el rewarded ad de forma remota.
 *  - [KEY_SPLASH_APP_OPEN_MAX_WAIT_SECONDS]: segundos máximos que el splash espera a que
 *    cargue el App Open antes de ir a la home (no cuenta mientras el anuncio está visible).
 *  - [KEY_MIN_VERSION_CODE]: versionCode mínimo obligatorio. Si la app instalada es inferior,
 *    se fuerza actualización mediante Google Play In-App Updates (IMMEDIATE).
 */
object RemoteConfigManager {

    private const val TAG = "mylog_ads"

    const val KEY_APP_OPEN_AD_ENABLED = "app_open_ad_enabled"
    const val KEY_INTERSTITIAL_AD_ENABLED = "interstitial_ad_enabled"
    const val KEY_INTERSTITIAL_AD_ADD_TRANSACTION_ENABLED = "interstitial_ad_add_transaction_enabled"
    const val KEY_INTERSTITIAL_AD_FREQUENCY = "interstitial_ad_frequency"
    const val KEY_REWARDED_AD_ENABLED = "rewarded_ad_enabled"
    const val KEY_SPLASH_APP_OPEN_MAX_WAIT_SECONDS = "splash_app_open_max_wait_seconds"
    const val KEY_MIN_VERSION_CODE = "min_version_code"

    private const val DEFAULT_SPLASH_APP_OPEN_MAX_WAIT_SECONDS = 10L
    private const val MIN_SPLASH_APP_OPEN_MAX_WAIT_SECONDS = 3L
    private const val MAX_SPLASH_APP_OPEN_MAX_WAIT_SECONDS = 60L

    /**
     * En DEBUG refrescamos al instante para poder probar parámetros como
     * [KEY_INTERSTITIAL_AD_FREQUENCY]. En RELEASE usamos 1 h.
     */
    private val minFetchIntervalSeconds: Long = if (BuildConfig.DEBUG) 0L else 3_600L

    @Volatile
    private var initialized = false

    /**
     * Inicializa Remote Config: aplica defaults, ajusta el intervalo mínimo de fetch
     * y dispara un `fetchAndActivate`. Idempotente.
     *
     * Llamar una sola vez al arrancar la app (Application.onCreate).
     */
    fun initialize() {
        if (initialized) return
        initialized = true

        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = minFetchIntervalSeconds
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
            .addOnCompleteListener {
                remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
                    .addOnCompleteListener {
                        remoteConfig.fetchAndActivate()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d(
                                        TAG,
                                        "Remote Config fetchAndActivate OK, updated=${task.result}, " +
                                            "interstitial_ad_frequency=${getInterstitialAdFrequency()}"
                                    )
                                } else {
                                    Log.w(TAG, "Remote Config fetchAndActivate failed", task.exception)
                                }
                            }
                    }
            }
    }

    fun isAppOpenAdEnabled(): Boolean = getBoolean(KEY_APP_OPEN_AD_ENABLED)

    fun isInterstitialAdEnabled(): Boolean = getBoolean(KEY_INTERSTITIAL_AD_ENABLED)

    fun isInterstitialAdOnAddTransactionEnabled(): Boolean =
        getBoolean(KEY_INTERSTITIAL_AD_ADD_TRANSACTION_ENABLED)

    /**
     * Número de solicitudes de intersticial entre cada visualización real.
     * Valor 1 = comportamiento habitual (mostrar en cada solicitud elegible).
     * Valor 4 = mostrar solo en la 4.ª solicitud y reiniciar el contador.
     */
    fun getInterstitialAdFrequency(): Long {
        val remoteConfig = Firebase.remoteConfig
        val value = remoteConfig.getValue(KEY_INTERSTITIAL_AD_FREQUENCY)
        val fromLong = value.asLong()
        val fromString = value.asString().trim().toLongOrNull()
        val resolved = when {
            fromLong > 0L -> fromLong
            fromString != null && fromString > 0L -> fromString
            else -> 1L
        }.coerceAtLeast(1L)
        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "interstitial_ad_frequency=$resolved (long=$fromLong, string='${value.asString()}', " +
                    "source=${value.source})"
            )
        }
        return resolved
    }

    fun isRewardedAdEnabled(): Boolean = getBoolean(KEY_REWARDED_AD_ENABLED)

    /**
     * Tiempo máximo de espera del splash al App Open de arranque (en ms).
     * Configurable en Firebase como entero en segundos ([KEY_SPLASH_APP_OPEN_MAX_WAIT_SECONDS]).
     */
    fun getSplashAppOpenMaxWaitMs(): Long {
        val seconds = getPositiveLong(
            key = KEY_SPLASH_APP_OPEN_MAX_WAIT_SECONDS,
            default = DEFAULT_SPLASH_APP_OPEN_MAX_WAIT_SECONDS,
        ).coerceIn(MIN_SPLASH_APP_OPEN_MAX_WAIT_SECONDS, MAX_SPLASH_APP_OPEN_MAX_WAIT_SECONDS)
        return seconds * 1_000L
    }

    /**
     * versionCode mínimo exigido por Remote Config. 0 = sin restricción.
     */
    fun getMinVersionCode(): Long = getNonNegativeLong(KEY_MIN_VERSION_CODE, default = 0L)

    /**
     * true si la versión instalada es inferior al mínimo remoto.
     */
    fun isForceUpdateRequired(): Boolean {
        val minVersion = getMinVersionCode()
        if (minVersion <= 0L) return false
        return BuildConfig.VERSION_CODE.toLong() < minVersion
    }

    private fun getNonNegativeLong(key: String, default: Long): Long {
        val remoteConfig = Firebase.remoteConfig
        val value = remoteConfig.getValue(key)
        val fromLong = value.asLong()
        val fromString = value.asString().trim().toLongOrNull()
        return when {
            fromLong >= 0L -> fromLong
            fromString != null && fromString >= 0L -> fromString
            else -> default
        }
    }

    private fun getPositiveLong(key: String, default: Long): Long {
        val remoteConfig = Firebase.remoteConfig
        val value = remoteConfig.getValue(key)
        val fromLong = value.asLong()
        val fromString = value.asString().trim().toLongOrNull()
        return when {
            fromLong > 0L -> fromLong
            fromString != null && fromString > 0L -> fromString
            else -> default
        }
    }

    private fun getBoolean(key: String, default: Boolean = true): Boolean {
        return try {
            Firebase.remoteConfig.getBoolean(key)
        } catch (t: Throwable) {
            Log.w(TAG, "Error leyendo $key, usando default $default", t)
            default
        }
    }
}
