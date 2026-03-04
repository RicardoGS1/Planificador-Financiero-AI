package com.virtualworld.easyexpensecontrol

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.virtualworld.easyexpensecontrol.ui.navigation.Navigation
import com.virtualworld.easyexpensecontrol.ui.theme.EasyExpenseControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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