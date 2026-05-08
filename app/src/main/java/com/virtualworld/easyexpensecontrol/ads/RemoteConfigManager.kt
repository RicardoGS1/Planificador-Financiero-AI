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
 */
object RemoteConfigManager {

    private const val TAG = "RemoteConfigManager"

    /** Clave en Firebase Remote Config para activar/desactivar el App Open Ad. */
    const val KEY_APP_OPEN_AD_ENABLED = "app_open_ad_enabled"

    /**
     * En DEBUG refrescamos cada vez (0s) para poder probar cambios al instante.
     * En RELEASE usamos 1h, suficiente para reaccionar sin abusar de la cuota de Firebase.
     */
    private val MIN_FETCH_INTERVAL_SECONDS: Long = if (BuildConfig.DEBUG) 0L else 3_600L

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
            minimumFetchIntervalInSeconds = MIN_FETCH_INTERVAL_SECONDS
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Remote Config fetchAndActivate OK, updated=${task.result}")
                } else {
                    Log.w(TAG, "Remote Config fetchAndActivate failed", task.exception)
                }
            }
    }

    /**
     * @return true si el App Open Ad está habilitado remotamente. Si Remote Config aún
     * no se ha inicializado o no hay valor cacheado, devuelve el default `true`.
     */
    fun isAppOpenAdEnabled(): Boolean {
        return try {
            Firebase.remoteConfig.getBoolean(KEY_APP_OPEN_AD_ENABLED)
        } catch (t: Throwable) {
            Log.w(TAG, "Error leyendo $KEY_APP_OPEN_AD_ENABLED, usando default true", t)
            true
        }
    }
}
