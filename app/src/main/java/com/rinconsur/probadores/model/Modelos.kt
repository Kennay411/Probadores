package com.rinconsur.probadores.model

enum class EstadoProbador(val etiqueta: String) {
    LIBRE("Libre"),
    OCUPADO("Ocupado"),
    EN_ORDENAMIENTO("En ordenamiento"),
    FUERA_DE_SERVICIO("Fuera de servicio")
}

enum class EstadoTurno(val etiqueta: String) {
    EN_ESPERA("En espera"),
    EN_USO("En uso"),
    ATENDIDO("Atendido"),
    ABANDONADO("Abandonado")
}

data class Probador(
    val id: Int,
    val codigo: String,
    val accesible: Boolean,
    val estado: EstadoProbador,
    val estadoDesde: Long
)

data class Turno(
    val id: Long,
    val codigo: String,
    val creadoEn: Long,
    val prendas: Int,
    val preferente: Boolean,
    val estado: EstadoTurno,
    val probadorId: Int?,
    val motivoAbandono: String?
)

data class UsoActivo(
    val usoId: Long,
    val turnoId: Long,
    val turnoCodigo: String,
    val probadorId: Int,
    val inicio: Long,
    val prendasIngresadas: Int
)

data class Indicadores(
    val turnos: Int = 0,
    val abandonos: Int = 0,
    val usosCerradosHoy: Int = 0,
    val usosCerradosTotal: Int = 0,
    val minutosPromedioUso: Int? = null
) {
    val porcentajeAbandono: Int
        get() = if (turnos == 0) 0 else Math.round(abandonos * 100.0 / turnos).toInt()
}
