package com.rinconsur.probadores.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.EstadoTurno
import com.rinconsur.probadores.model.Indicadores
import com.rinconsur.probadores.model.Probador
import com.rinconsur.probadores.model.ReglaNegocioException
import com.rinconsur.probadores.model.Reglas
import com.rinconsur.probadores.model.Turno
import com.rinconsur.probadores.model.UsoActivo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

class ProbadorRepository(private val helper: DatabaseHelper) {

    private val columnasTurno =
        "id, codigo, creado_en, prendas, preferente, estado, probador_id, motivo_abandono"

    private fun hoy(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private inline fun <T> transaccion(bloque: (SQLiteDatabase) -> T): T {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val resultado = bloque(db)
            db.setTransactionSuccessful()
            return resultado
        } finally {
            db.endTransaction()
        }
    }

    // ---------- Lecturas ----------

    fun listarProbadores(): List<Probador> = probadores(helper.readableDatabase)

    fun listarEnEspera(): List<Turno> = enEspera(helper.readableDatabase)

    fun usosActivos(): List<UsoActivo> = usosActivos(helper.readableDatabase)

    fun indicadores(): Indicadores {
        val db = helper.readableDatabase
        val fecha = hoy()
        val turnos = contar(db, "SELECT COUNT(*) FROM turnos WHERE fecha = ?", fecha)
        val abandonos = contar(
            db,
            "SELECT COUNT(*) FROM turnos WHERE fecha = ? AND estado = ?",
            fecha,
            EstadoTurno.ABANDONADO.name
        )
        val cerradosHoy = contar(
            db,
            "SELECT COUNT(*) FROM usos u JOIN turnos t ON t.id = u.turno_id " +
                "WHERE t.fecha = ? AND u.fin IS NOT NULL",
            fecha
        )
        val cerradosTotal = contar(db, "SELECT COUNT(*) FROM usos WHERE fin IS NOT NULL")
        return Indicadores(
            turnos = turnos,
            abandonos = abandonos,
            usosCerradosHoy = cerradosHoy,
            usosCerradosTotal = cerradosTotal,
            minutosPromedioUso = promedioUsoMinutos(db, cerradosTotal)
        )
    }

    /** Minutos de uso + ordenamiento para estimar la espera (referencia: 8 + 1). */
    fun minutosPorCiclo(): Int {
        val db = helper.readableDatabase
        val cerrados = contar(db, "SELECT COUNT(*) FROM usos WHERE fin IS NOT NULL")
        val uso = promedioUsoMinutos(db, cerrados) ?: Reglas.USO_REFERENCIA_MIN
        return uso + Reglas.ORDENAMIENTO_MIN
    }

    // ---------- Acciones (devuelven el mensaje para el usuario) ----------

    fun crearTurno(prendas: Int, preferente: Boolean): String {
        if (prendas !in 1..Reglas.MAX_PRENDAS) {
            throw ReglaNegocioException("Las prendas deben ser entre 1 y ${Reglas.MAX_PRENDAS}.")
        }
        return transaccion { db ->
            val fecha = hoy()
            val numero = contar(db, "SELECT COUNT(*) FROM turnos WHERE fecha = ?", fecha) + 1
            val codigo = String.format(Locale.US, "T-%03d", numero)
            val valores = ContentValues().apply {
                put("codigo", codigo)
                put("fecha", fecha)
                put("creado_en", System.currentTimeMillis())
                put("prendas", prendas)
                put("preferente", if (preferente) 1 else 0)
                put("estado", EstadoTurno.EN_ESPERA.name)
            }
            db.insertOrThrow("turnos", null, valores)
            "Turno $codigo creado."
        }
    }

    fun asignarSiguiente(): String = transaccion { db ->
        val turno = enEspera(db).firstOrNull()
            ?: throw ReglaNegocioException("No hay turnos en espera.")
        val libres = probadores(db).filter { it.estado == EstadoProbador.LIBRE }
        if (libres.isEmpty()) throw ReglaNegocioException("No hay probadores libres.")

        // El probador accesible se reserva para atención preferente cuando es posible.
        val elegido = if (turno.preferente) {
            libres.firstOrNull { it.accesible } ?: libres.first()
        } else {
            libres.firstOrNull { !it.accesible } ?: libres.first()
        }

        val ahora = System.currentTimeMillis()
        val valoresTurno = ContentValues().apply {
            put("estado", EstadoTurno.EN_USO.name)
            put("probador_id", elegido.id)
        }
        db.update("turnos", valoresTurno, "id = ?", arrayOf(turno.id.toString()))
        cambiarEstado(db, elegido.id, EstadoProbador.OCUPADO)
        val valoresUso = ContentValues().apply {
            put("turno_id", turno.id)
            put("probador_id", elegido.id)
            put("inicio", ahora)
            put("prendas_ingresadas", turno.prendas)
        }
        db.insertOrThrow("usos", null, valoresUso)
        "${turno.codigo} asignado a ${elegido.codigo}."
    }

    fun registrarAbandono(turnoId: Long, motivo: String): String {
        val limpio = motivo.trim().take(120)
        if (limpio.isEmpty()) throw ReglaNegocioException("Indica el motivo del abandono.")
        return transaccion { db ->
            val turno = buscarTurno(db, turnoId)
                ?: throw ReglaNegocioException("El turno no existe.")
            if (turno.estado != EstadoTurno.EN_ESPERA) {
                throw ReglaNegocioException("Solo se puede registrar abandono de turnos en espera.")
            }
            val valores = ContentValues().apply {
                put("estado", EstadoTurno.ABANDONADO.name)
                put("motivo_abandono", limpio)
            }
            db.update("turnos", valores, "id = ?", arrayOf(turnoId.toString()))
            "Abandono registrado: ${turno.codigo}."
        }
    }

    fun cerrarUso(probadorId: Int, prendasDevueltas: Int): String = transaccion { db ->
        val probador = probadores(db).firstOrNull { it.id == probadorId }
            ?: throw ReglaNegocioException("El probador no existe.")
        if (probador.estado != EstadoProbador.OCUPADO) {
            throw ReglaNegocioException("${probador.codigo} no está en uso.")
        }
        val uso = usosActivos(db).firstOrNull { it.probadorId == probadorId }
            ?: throw ReglaNegocioException("No hay un uso activo registrado en ${probador.codigo}.")
        if (prendasDevueltas < 0 || prendasDevueltas > uso.prendasIngresadas) {
            throw ReglaNegocioException(
                "Las prendas devueltas deben estar entre 0 y ${uso.prendasIngresadas}."
            )
        }
        val valoresUso = ContentValues().apply {
            put("fin", System.currentTimeMillis())
            put("prendas_devueltas", prendasDevueltas)
        }
        db.update("usos", valoresUso, "id = ?", arrayOf(uso.usoId.toString()))
        val valoresTurno = ContentValues().apply { put("estado", EstadoTurno.ATENDIDO.name) }
        db.update("turnos", valoresTurno, "id = ?", arrayOf(uso.turnoId.toString()))
        cambiarEstado(db, probadorId, EstadoProbador.EN_ORDENAMIENTO)
        "Uso cerrado en ${probador.codigo} (devueltas $prendasDevueltas de ${uso.prendasIngresadas}). " +
            "Ordena el probador y libéralo."
    }

    fun liberarProbador(probadorId: Int): String = transaccion { db ->
        val probador = probadores(db).firstOrNull { it.id == probadorId }
            ?: throw ReglaNegocioException("El probador no existe.")
        if (probador.estado != EstadoProbador.EN_ORDENAMIENTO) {
            throw ReglaNegocioException("${probador.codigo} no está en ordenamiento.")
        }
        cambiarEstado(db, probadorId, EstadoProbador.LIBRE)
        "${probador.codigo} liberado."
    }

    fun alternarFueraDeServicio(probadorId: Int): String = transaccion { db ->
        val probador = probadores(db).firstOrNull { it.id == probadorId }
            ?: throw ReglaNegocioException("El probador no existe.")
        when (probador.estado) {
            EstadoProbador.LIBRE -> {
                cambiarEstado(db, probadorId, EstadoProbador.FUERA_DE_SERVICIO)
                "${probador.codigo} marcado fuera de servicio."
            }
            EstadoProbador.FUERA_DE_SERVICIO -> {
                cambiarEstado(db, probadorId, EstadoProbador.LIBRE)
                "${probador.codigo} habilitado nuevamente."
            }
            else -> throw ReglaNegocioException(
                "Solo se puede cambiar el servicio de un probador libre o fuera de servicio."
            )
        }
    }

    // ---------- Auxiliares ----------

    private fun probadores(db: SQLiteDatabase): List<Probador> {
        val lista = mutableListOf<Probador>()
        db.rawQuery(
            "SELECT id, codigo, accesible, estado, estado_desde FROM probadores ORDER BY id",
            null
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Probador(
                        id = c.getInt(0),
                        codigo = c.getString(1),
                        accesible = c.getInt(2) == 1,
                        estado = EstadoProbador.valueOf(c.getString(3)),
                        estadoDesde = c.getLong(4)
                    )
                )
            }
        }
        return lista
    }

    private fun leerTurno(c: Cursor) = Turno(
        id = c.getLong(0),
        codigo = c.getString(1),
        creadoEn = c.getLong(2),
        prendas = c.getInt(3),
        preferente = c.getInt(4) == 1,
        estado = EstadoTurno.valueOf(c.getString(5)),
        probadorId = if (c.isNull(6)) null else c.getInt(6),
        motivoAbandono = if (c.isNull(7)) null else c.getString(7)
    )

    private fun enEspera(db: SQLiteDatabase): List<Turno> {
        val lista = mutableListOf<Turno>()
        db.rawQuery(
            "SELECT $columnasTurno FROM turnos WHERE fecha = ? AND estado = ? " +
                "ORDER BY preferente DESC, creado_en ASC, id ASC",
            arrayOf(hoy(), EstadoTurno.EN_ESPERA.name)
        ).use { c ->
            while (c.moveToNext()) lista.add(leerTurno(c))
        }
        return lista
    }

    private fun buscarTurno(db: SQLiteDatabase, id: Long): Turno? =
        db.rawQuery(
            "SELECT $columnasTurno FROM turnos WHERE id = ?",
            arrayOf(id.toString())
        ).use { c -> if (c.moveToFirst()) leerTurno(c) else null }

    private fun usosActivos(db: SQLiteDatabase): List<UsoActivo> {
        val lista = mutableListOf<UsoActivo>()
        db.rawQuery(
            "SELECT u.id, u.turno_id, t.codigo, u.probador_id, u.inicio, u.prendas_ingresadas " +
                "FROM usos u JOIN turnos t ON t.id = u.turno_id " +
                "WHERE u.fin IS NULL ORDER BY u.id",
            null
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    UsoActivo(
                        usoId = c.getLong(0),
                        turnoId = c.getLong(1),
                        turnoCodigo = c.getString(2),
                        probadorId = c.getInt(3),
                        inicio = c.getLong(4),
                        prendasIngresadas = c.getInt(5)
                    )
                )
            }
        }
        return lista
    }

    private fun cambiarEstado(db: SQLiteDatabase, probadorId: Int, estado: EstadoProbador) {
        val valores = ContentValues().apply {
            put("estado", estado.name)
            put("estado_desde", System.currentTimeMillis())
        }
        db.update("probadores", valores, "id = ?", arrayOf(probadorId.toString()))
    }

    private fun contar(db: SQLiteDatabase, sql: String, vararg args: String): Int {
        val argumentos: Array<String>? = if (args.isEmpty()) null else arrayOf(*args)
        return db.rawQuery(sql, argumentos).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    private fun promedioUsoMinutos(db: SQLiteDatabase, cerrados: Int): Int? {
        if (cerrados < Reglas.MIN_USOS_PARA_PROMEDIO) return null
        val promedioMs: Double = db.rawQuery(
            "SELECT AVG(fin - inicio) FROM usos WHERE fin IS NOT NULL",
            null
        ).use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getDouble(0) else null } ?: return null
        return ceil(promedioMs / 60000.0).toInt().coerceAtLeast(1)
    }
}
