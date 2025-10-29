package com.example.factcheck_app.models

data class HistorialItem(
    val id: Int,
    val texto: String,
    val resultado: String,
    val fecha: String,
    val url: String? = null
)