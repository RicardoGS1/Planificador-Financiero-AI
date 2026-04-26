package com.virtualworld.easyexpensecontrol

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.virtualworld.easyexpensecontrol.ads.ConsentManager
import com.virtualworld.easyexpensecontrol.core.util.LocaleHelper
import com.virtualworld.easyexpensecontrol.ui.navigation.Navigation
import com.virtualworld.easyexpensecontrol.ui.theme.EasyExpenseControlTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Pantalla completa: ocultar barra de estado y barra de navegación
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
        )
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Si en lanzamientos previos ya tenemos consentimiento (o no es necesario en esta
        // región), inicializa MobileAds de inmediato para que el App Open en frío esté listo.
        if (ConsentManager.canRequestAds(this)) {
            (application as FinancialApp).initializeMobileAdsIfNeeded(this)
        }

        // Solicita / actualiza el consentimiento UMP. Cuando el usuario decida (o el SDK
        // resuelva sin formulario), si se pueden pedir anuncios, arranca MobileAds.
        ConsentManager.gatherConsent(this) { canRequestAds ->
            if (canRequestAds) {
                (application as FinancialApp).initializeMobileAdsIfNeeded(this)
            }
        }

        setContent {
            val navController = rememberNavController()
            EasyExpenseControlTheme {
                Navigation(navController = navController,
                    // Sonido de borrado
                    onPlaySound = { soundRes ->
                        val mediaPlayer = MediaPlayer.create(this, soundRes)
                        mediaPlayer.setOnCompletionListener { it.release() }
                        mediaPlayer.start()
                    }
                )
            }
        }
    }
}
