// models/Noticia.kt (actualizado)
package com.example.factcheckapp.models

import com.google.gson.annotations.SerializedName
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

data class Noticia(
    @SerializedName("texto")
    val texto: String,

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("usuario_id")
    val usuarioId: String = Firebase.auth.currentUser?.uid ?: "anonimo",

    @SerializedName("dispositivo_id")
    val dispositivoId: String? = null,


)

data class VerificationResult(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("resultado")
    val resultado: String,

    @SerializedName("razonamiento")  // ✅ NUEVO
    val razonamiento: String? = null,

    @SerializedName("detalle")
    val detalle: Map<String, String>? = null,

    @SerializedName("consulta_id")
    val consultaId: Int? = null,

    @SerializedName("fecha_procesamiento")
    val fechaProcesamiento: String
)

data class HistoryItem(
    @SerializedName("id")
    val id: Int,

    @SerializedName("texto")
    val texto: String,

    @SerializedName("resultado")
    val resultado: String,

    @SerializedName("url")
    val url: String?,

    @SerializedName("fecha")
    val fecha: String
)



// models/Noticia.kt (agrega estas clases al final)
data class HistorialResponse(
    @SerializedName("total")
    val total: Int,

    @SerializedName("limit")
    val limit: Int,

    @SerializedName("offset")
    val offset: Int,

    @SerializedName("consultas")
    val consultas: List<HistoryItem>
)

// Data class para el historial local (puedes eliminar la anterior)
data class HistorialItem(
    val id: Int,
    val texto: String,
    val resultado: String,
    val url: String?,
    val fecha: String,
    val usuarioId: String?
)