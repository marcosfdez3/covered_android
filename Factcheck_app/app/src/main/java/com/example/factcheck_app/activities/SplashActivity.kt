package com.example.factcheckapp.activities

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.factcheckapp.MainActivity
import com.example.factcheckapp.R
import com.example.factcheckapp.TutorialActivity

class SplashActivity : AppCompatActivity() {

    // Duración de la splash screen en milisegundos
    private val SPLASH_DELAY: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar la status bar transparente

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true  // Iconos oscuros
        }

        setContentView(R.layout.activity_splash)

        // Ocultar Action Bar
        supportActionBar?.hide()

        // Navegar a la siguiente actividad después del delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkTutorialStatusAndNavigate()
        }, SPLASH_DELAY)
    }

    /**
     * Verifica si el usuario ya completó el tutorial
     * y navega a la actividad correspondiente
     */
    private fun checkTutorialStatusAndNavigate() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val tutorialCompleted = prefs.getBoolean("tutorial_completed", false)

        val intent = if (tutorialCompleted) {
            // Si ya completó el tutorial, ir directamente a MainActivity
            Intent(this, MainActivity::class.java)
        } else {
            // Si es la primera vez, mostrar el tutorial
            Intent(this, TutorialActivity::class.java)
        }

        startActivity(intent)
        finish() // Cerrar SplashActivity para que no vuelva atrás
    }
}