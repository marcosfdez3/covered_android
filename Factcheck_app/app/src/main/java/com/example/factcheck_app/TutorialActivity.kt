package com.example.factcheckapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.factcheckapp.R

class TutorialActivity : AppCompatActivity() {

    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var btnFinish: Button
    private lateinit var btnSkip: TextView
    private lateinit var indicatorLayout: LinearLayout
    private lateinit var imgTutorialBackground: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var imgIcon: ImageView

    private val tutorialPages = listOf(
        TutorialPage(
            R.drawable.erizo_tutorial,
            "Bienvenido a Covered",
            "La aplicación que te ayuda a verificar la veracidad de noticias e información"
        ),
        TutorialPage(
            R.drawable.erizo_leyendo,
            "Verificación Rápida",
            "Ingresa cualquier texto o URL y obtén resultados instantáneos basados en fuentes confiables"
        ),
        TutorialPage(
            R.drawable.erizo_volando,
            "Historial de Consultas",
            "Revisa tus verificaciones anteriores en cualquier momento desde el menú de navegación"
        )
    )

    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si ya se completó el tutorial
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val tutorialCompleted = prefs.getBoolean("tutorial_completed", false)

        if (tutorialCompleted) {
            startMainActivity()
            return
        }

        setContentView(R.layout.activity_tutorial)
        initViews()
        setupButtons()
        showPage(currentPage)
    }

    private fun initViews() {
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNextPage)
        btnFinish = findViewById(R.id.btnFinish)
        btnSkip = findViewById(R.id.btnSkip)
        indicatorLayout = findViewById(R.id.indicatorLayout)
        imgTutorialBackground = findViewById(R.id.imgTutorialBackground)
        tvTitle = findViewById(R.id.tvTitle)
        tvDescription = findViewById(R.id.tvDescription)
        imgIcon = findViewById(R.id.imgIcon)
    }

    private fun setupButtons() {

        btnPrevious.width= 200
        btnNext.width= 200
        btnFinish.width=200
        btnNext.setOnClickListener {
            if (currentPage < tutorialPages.size - 1) {
                currentPage++
                showPage(currentPage)
            }
        }

        btnPrevious.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                showPage(currentPage)
            }
        }

        btnFinish.setOnClickListener {
            finishTutorial()
        }

        btnSkip.setOnClickListener {
            finishTutorial()
        }
    }

    private fun showPage(pageIndex: Int) {
        val page = tutorialPages[pageIndex]

        // Actualizar background
        imgTutorialBackground.setImageResource(page.imageRes)

        // Actualizar textos
        tvTitle.text = page.title
        tvDescription.text = page.description

        // Actualizar UI
        updateUI(pageIndex)


        // Animación suave
        animateContentChange()
    }

    private fun animateContentChange() {
        tvTitle.alpha = 0f
        tvDescription.alpha = 0f

        tvTitle.animate()
            .alpha(1f)
            .setDuration(400)
            .start()

        tvDescription.animate()
            .alpha(1f)
            .setDuration(400)
            .setStartDelay(200)
            .start()
    }





    private fun updateUI(position: Int) {
        when {
            position == 0 -> {
                btnPrevious.visibility = View.GONE
                btnNext.visibility = View.VISIBLE
                btnFinish.visibility = View.GONE
            }
            position == tutorialPages.size - 1 -> {
                btnPrevious.visibility = View.VISIBLE
                btnNext.visibility = View.GONE
                btnFinish.visibility = View.VISIBLE
            }
            else -> {
                btnPrevious.visibility = View.VISIBLE
                btnNext.visibility = View.VISIBLE
                btnFinish.visibility = View.GONE
            }
        }
    }

    private fun finishTutorial() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("tutorial_completed", true).apply()
        startMainActivity()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

// Clase de datos para las páginas del tutorial
data class TutorialPage(
    val imageRes: Int,
    val title: String,
    val description: String
)