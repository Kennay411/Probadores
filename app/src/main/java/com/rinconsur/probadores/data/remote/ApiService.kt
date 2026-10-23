package com.rinconsur.probadores.data.remote

import retrofit2.http.GET

data class ProbadorDto(
    val id: Int? = null,
    val codigo: String? = null,
    val estado: String? = null,
    val accesible: Boolean? = null
)

data class ResumenDto(
    val libres: Int? = null,
    val enFila: Int? = null,
    val esperaEstimadaMin: Int? = null
)

/** Rutas de ejemplo: ajustarlas a los endpoints reales del backend. */
interface ApiService {
    @GET("api/probadores")
    suspend fun obtenerProbadores(): List<ProbadorDto>

    @GET("api/resumen")
    suspend fun obtenerResumen(): ResumenDto
}
