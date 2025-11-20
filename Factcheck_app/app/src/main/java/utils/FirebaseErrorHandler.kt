package com.example.factcheckapp.utils

import com.google.firebase.auth.FirebaseAuthException

object FirebaseErrorHandler {

    fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "El formato del email no es válido"
                    "ERROR_WRONG_PASSWORD" -> "La contraseña es incorrecta"
                    "ERROR_USER_NOT_FOUND" -> "No existe una cuenta con este email"
                    "ERROR_USER_DISABLED" -> "Esta cuenta ha sido deshabilitada"
                    "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Intenta más tarde"
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Ya existe una cuenta con este email"
                    "ERROR_WEAK_PASSWORD" -> "La contraseña es demasiado débil"
                    else -> "Error de autenticación: ${exception.message}"
                }
            }
            else -> "Error: ${exception.message}"
        }
    }
}