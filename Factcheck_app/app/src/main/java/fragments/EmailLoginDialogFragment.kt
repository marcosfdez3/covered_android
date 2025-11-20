// EmailLoginDialogFragment.kt (ACTUALIZADO)
package com.example.factcheckapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.factcheckapp.utils.FirebaseErrorHandler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class EmailLoginDialogFragment : DialogFragment() {

    private lateinit var auth: FirebaseAuth
    private var loginSuccessListener: ((FirebaseUser) -> Unit)? = null
    private var isLoginMode = true // true = Login, false = Register

    fun setOnLoginSuccessListener(listener: (FirebaseUser) -> Unit) {
        this.loginSuccessListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        auth = Firebase.auth

        val builder = AlertDialog.Builder(requireActivity())
        val inflater = LayoutInflater.from(requireActivity())
        val view = inflater.inflate(R.layout.fragment_email_login_dialog, null)

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        builder.setView(view)
            .setTitle("Iniciar Sesión con Email")
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

        // Configurar botones según el modo
        updateButtonTexts(btnLogin, btnRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInputs(email, password)) {
                if (isLoginMode) {
                    loginUser(email, password, progressBar)
                } else {
                    registerUser(email, password, progressBar)
                }
            }
        }

        btnRegister.setOnClickListener {
            // Cambiar entre modos Login/Registro
            isLoginMode = !isLoginMode
            updateButtonTexts(btnLogin, btnRegister)
            updateDialogTitle(builder)
            clearInputs(etEmail, etPassword)
        }

        return builder.create()
    }

    private fun updateButtonTexts(btnLogin: Button, btnRegister: Button) {
        if (isLoginMode) {
            btnLogin.text = "Iniciar Sesión"
            btnRegister.text = "Crear Cuenta"
        } else {
            btnLogin.text = "Registrarse"
            btnRegister.text = "Ya tengo cuenta"
        }
    }

    private fun updateDialogTitle(builder: AlertDialog.Builder) {
        val title = if (isLoginMode) "Iniciar Sesión" else "Crear Cuenta"
        builder.setTitle(title)
    }

    private fun clearInputs(etEmail: EditText, etPassword: EditText) {
        etEmail.text?.clear()
        etPassword.text?.clear()
        etEmail.error = null
        etPassword.error = null
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (TextUtils.isEmpty(email)) {
            showMessage("Por favor ingresa tu email")
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Por favor ingresa un email válido")
            return false
        }

        if (TextUtils.isEmpty(password)) {
            showMessage("Por favor ingresa tu contraseña")
            return false
        }

        if (password.length < 6) {
            showMessage("La contraseña debe tener al menos 6 caracteres")
            return false
        }

        return true
    }

    // En EmailLoginDialogFragment.kt - actualizar los métodos de login/registro
    private fun loginUser(email: String, password: String, progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE
        disableButtons()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                progressBar.visibility = View.GONE
                enableButtons()

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        if (user.isEmailVerified) {
                            showMessage("¡Bienvenido de nuevo!")
                            loginSuccessListener?.invoke(user)
                            dismiss()
                        } else {
                            showMessage("Por favor verifica tu email antes de iniciar sesión")
                            // Opcional: reenviar email de verificación
                            user.sendEmailVerification()
                        }
                    }
                } else {
                    val errorMessage = FirebaseErrorHandler.getErrorMessage(task.exception!!)
                    showMessage(errorMessage)
                }
            }
    }

    private fun registerUser(email: String, password: String, progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE
        disableButtons()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                progressBar.visibility = View.GONE
                enableButtons()

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Actualizar el perfil del usuario con el email como nombre
                        updateUserProfile(user, email)
                    }
                } else {
                    showMessage("Error al registrar: ${task.exception?.message}")
                }
            }
    }

    private fun updateUserProfile(user: FirebaseUser, email: String) {
        val displayName = email.substringBefore("@")

        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showMessage("¡Cuenta creada exitosamente!")
                    loginSuccessListener?.invoke(user)
                    dismiss()
                } else {
                    // Si falla la actualización del perfil, igualmente proceder
                    showMessage("¡Cuenta creada exitosamente!")
                    loginSuccessListener?.invoke(user)
                    dismiss()
                }
            }
    }

    private fun disableButtons() {
        view?.findViewById<Button>(R.id.btnLogin)?.isEnabled = false
        view?.findViewById<Button>(R.id.btnRegister)?.isEnabled = false
    }

    private fun enableButtons() {
        view?.findViewById<Button>(R.id.btnLogin)?.isEnabled = true
        view?.findViewById<Button>(R.id.btnRegister)?.isEnabled = true
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}