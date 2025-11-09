package com.example.factcheckapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.factcheckapp.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sharedPreferences: SharedPreferences
    private val auth = Firebase.auth

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val PREFS_NAME = "CoveredPrefs"
        private const val KEY_SKIPPED_LOGIN = "skipped_login"
        private const val KEY_USER_LOGGED_IN = "user_logged_in"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar SI ya pasó por login
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (shouldSkipLogin()) {
            startMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureGoogleSignIn()
        setupClickListeners()
    }

    private fun shouldSkipLogin(): Boolean {
        return sharedPreferences.getBoolean(KEY_SKIPPED_LOGIN, false) ||
                sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false) ||
                auth.currentUser != null
    }

    private fun configureGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()  // ← IMPORTANTE: Solicitar el perfil para obtener el nombre
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            showMessage("Error configurando Google Sign-In")
        }
    }

    private fun setupClickListeners() {
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnSkip.setOnClickListener {
            skipLogin()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)

                // ✅ OBTENER Y GUARDAR LA INFORMACIÓN DEL USUARIO
                val userName = account?.displayName ?: "Usuario"
                val userEmail = account?.email ?: ""

                // Guardar en SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putBoolean(KEY_USER_LOGGED_IN, true)
                editor.putString("user_name", userName)  // ← GUARDAR EL NOMBRE
                editor.putString("user_email", userEmail)
                editor.apply()

                showMessage("¡Bienvenido, $userName!")
                startMainActivity()

            } catch (e: ApiException) {
                showMessage("Error en login: ${e.message}")
            }
        }
    }

    private fun skipLogin() {
        // Guardar que el usuario eligió saltar el login
        sharedPreferences.edit().putBoolean(KEY_SKIPPED_LOGIN, true).apply()
        showMessage("Modo invitado activado")
        startMainActivity()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // IMPORTANTE: cerrar LoginActivity
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}