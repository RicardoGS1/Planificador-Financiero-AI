package com.virtualworld.easyexpensecontrol.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.virtualworld.easyexpensecontrol.BuildConfig

/**
 * Gestiona la actualización forzada mediante Google Play In-App Updates (tipo IMMEDIATE).
 *
 * Flujo:
 * 1. Remote Config indica que la versión instalada es inferior a [min_version_code].
 * 2. Se intenta lanzar el flujo IMMEDIATE de Play (pantalla a pantalla completa de Play).
 * 3. Si no está disponible (p. ej. APK sideload), se muestra la pantalla de bloqueo con enlace a Play Store.
 */
object PlayUpdateManager {

    private const val TAG = "PlayUpdateManager"
    private const val PLAY_STORE_PACKAGE = "com.android.vending"

    private var appUpdateManager: AppUpdateManager? = null
    private var updateLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var onUpdateFlowFinished: ((Boolean) -> Unit)? = null

    fun register(activity: ComponentActivity) {
        if (updateLauncher != null) return

        appUpdateManager = AppUpdateManagerFactory.create(activity)
        updateLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            val success = result.resultCode == Activity.RESULT_OK
            Log.d(TAG, "In-app update flow finished, success=$success")
            onUpdateFlowFinished?.invoke(success)
            onUpdateFlowFinished = null
        }
    }

    /**
     * Reanuda un IMMEDIATE update interrumpido (p. ej. usuario volvió a la app tras minimizar).
     */
    fun resumeStalledImmediateUpdate(activity: Activity) {
        val manager = appUpdateManager ?: AppUpdateManagerFactory.create(activity)
        appUpdateManager = manager

        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startImmediateUpdateFlow(activity, info) { /* sin acción extra */ }
            }
        }
    }

    /**
     * Comprueba si hace falta forzar actualización y lanza el flujo de Play cuando sea posible.
     *
     * @param onUpdateStarted Play ha tomado el control con IMMEDIATE (bloquear navegación normal).
     * @param onFallbackRequired No se pudo usar In-App Update; mostrar pantalla con enlace a Play Store.
     * @param onNotRequired La versión instalada cumple el mínimo remoto.
     */
    fun checkAndStartForceUpdate(
        activity: Activity,
        isForceUpdateRequired: Boolean,
        onUpdateStarted: () -> Unit,
        onFallbackRequired: () -> Unit,
        onNotRequired: () -> Unit,
    ) {
        if (!isForceUpdateRequired) {
            onNotRequired()
            return
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Force update requerido (DEBUG: se omite In-App Update)")
            onNotRequired()
            return
        }

        val manager = appUpdateManager ?: AppUpdateManagerFactory.create(activity)
        appUpdateManager = manager

        manager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                    onUpdateStarted()
                    startImmediateUpdateFlow(activity, info) { success ->
                        if (!success) onFallbackRequired()
                    }
                }

                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    onUpdateStarted()
                    startImmediateUpdateFlow(activity, info) { success ->
                        if (!success) onFallbackRequired()
                    }
                }

                else -> {
                    Log.w(
                        TAG,
                        "Force update requerido pero In-App Update no disponible " +
                            "(availability=${info.updateAvailability()})"
                    )
                    onFallbackRequired()
                }
            }
        }.addOnFailureListener { error ->
            Log.w(TAG, "Error comprobando In-App Update", error)
            onFallbackRequired()
        }
    }

    fun openPlayStoreListing(activity: Activity) {
        val packageName = activity.packageName
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage(PLAY_STORE_PACKAGE)
        }

        try {
            activity.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        }
    }

    private fun startImmediateUpdateFlow(
        activity: Activity,
        appUpdateInfo: AppUpdateInfo,
        onFinished: (Boolean) -> Unit,
    ) {
        val launcher = updateLauncher
        if (launcher == null) {
            Log.w(TAG, "updateLauncher no registrado; abriendo Play Store")
            onFinished(false)
            return
        }

        onUpdateFlowFinished = onFinished
        val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        val manager = appUpdateManager ?: return

        manager.startUpdateFlowForResult(
            appUpdateInfo,
            launcher,
            options
        ).addOnFailureListener { error ->
            Log.w(TAG, "No se pudo iniciar In-App Update IMMEDIATE", error)
            onUpdateFlowFinished = null
            onFinished(false)
        }
    }
}
