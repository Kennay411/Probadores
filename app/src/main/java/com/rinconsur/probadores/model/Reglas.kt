package com.rinconsur.probadores.model

object Reglas {
    const val TOTAL_PROBADORES = 8
    const val MAX_PRENDAS = 6
    const val USO_REFERENCIA_MIN = 8
    const val ORDENAMIENTO_MIN = 1
    const val MIN_USOS_PARA_PROMEDIO = 10
    const val USO_PROLONGADO_MIN = 15
}

/** Error de regla de negocio: su mensaje se muestra tal cual al usuario. */
class ReglaNegocioException(mensaje: String) : Exception(mensaje)

/**
 * Espera estimada (minutos) para un turno nuevo.
 * Devuelve null si no hay probadores habilitados.
 */
fun estimarEsperaMinutos(enFila: Int, libres: Int, habilitados: Int, minutosCiclo: Int): Int? {
    if (habilitados <= 0) return null
    val pendientes = enFila + 1 - libres
    if (pendientes <= 0) return 0
    val rondas = (pendientes + habilitados - 1) / habilitados
    return rondas * minutosCiclo
}
