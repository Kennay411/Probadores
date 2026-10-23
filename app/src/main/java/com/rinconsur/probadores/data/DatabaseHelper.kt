package com.rinconsur.probadores.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.Reglas
import java.util.Locale

/** Base local SQLite. No guarda nombre, RUT ni datos personales de clientes. */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, NOMBRE, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE probadores (
                id INTEGER PRIMARY KEY,
                codigo TEXT NOT NULL,
                accesible INTEGER NOT NULL,
                estado TEXT NOT NULL,
                estado_desde INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE turnos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                codigo TEXT NOT NULL,
                fecha TEXT NOT NULL,
                creado_en INTEGER NOT NULL,
                prendas INTEGER NOT NULL,
                preferente INTEGER NOT NULL,
                estado TEXT NOT NULL,
                probador_id INTEGER,
                motivo_abandono TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE usos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                turno_id INTEGER NOT NULL,
                probador_id INTEGER NOT NULL,
                inicio INTEGER NOT NULL,
                fin INTEGER,
                prendas_ingresadas INTEGER NOT NULL,
                prendas_devueltas INTEGER
            )
            """.trimIndent()
        )

        // Datos iniciales ficticios: P-01 a P-08 (P-08 accesible).
        val ahora = System.currentTimeMillis()
        for (i in 1..Reglas.TOTAL_PROBADORES) {
            val valores = ContentValues().apply {
                put("id", i)
                put("codigo", String.format(Locale.US, "P-%02d", i))
                put("accesible", if (i == Reglas.TOTAL_PROBADORES) 1 else 0)
                put("estado", EstadoProbador.LIBRE.name)
                put("estado_desde", ahora)
            }
            db.insert("probadores", null, valores)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS usos")
        db.execSQL("DROP TABLE IF EXISTS turnos")
        db.execSQL("DROP TABLE IF EXISTS probadores")
        onCreate(db)
    }

    companion object {
        const val NOMBRE = "rinconsur.db"
        const val VERSION = 1
    }
}
