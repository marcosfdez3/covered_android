// network/ApiService.kt
package com.example.factcheckapp.network

import com.example.factcheckapp.models.Noticia
import com.example.factcheckapp.models.VerificationResult
import com.example.factcheckapp.models.HistoryItem
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit


private const val BASE_URL = "http://192.168.1.18:8000/"

interface FactCheckApiService {
    @POST("verificar/movil")
    suspend fun verificarNoticia(@Body noticia: Noticia): VerificationResult

    @GET("historial")
    suspend fun obtenerHistorial(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): HistorialResponse

    @GET("estadisticas")
    suspend fun obtenerEstadisticas(): EstadisticasResponse
}

data class HistorialResponse(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val consultas: List<HistoryItem>
)

data class EstadisticasResponse(
    val total_consultas: Int,
    val usuarios_unicos: Int,
    val longitud_promedio_texto: Double
)

object RetrofitInstance {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    val api: FactCheckApiService by lazy {
        retrofit.create(FactCheckApiService::class.java)
    }
}