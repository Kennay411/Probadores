package com.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rinconsur.probadores.data.DatabaseHelper
import com.rinconsur.probadores.data.ProbadorRepository
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.Indicadores
import com.rinconsur.probadores.model.Probador
import com.rinconsur.probadores.model.ReglaNegocioException
import com.rinconsur.probadores.model.Turno
import com.rinconsur.probadores.model.UsoActivo
import com.rinconsur.probadores.model.estimarEsperaMinutos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val probadores: List<Probador> = emptyList(),
    val enEspera: List<Turno> = emptyList(),
    val usosActivos: Map<Int, UsoActivo> = emptyMap(),
    val indicadores: Indicadores = Indicadores(),
    val esperaMin: Int? = 0,
    val cargando: Boolean = true,
    val mensaje: String? = null
) {
    val libres: Int get() = probadores.count { it.estado == EstadoProbador.LIBRE }
}

class ProbadoresViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ProbadorRepository(DatabaseHelper(application))

    private val _estado = MutableStateFlow(UiState())
    val estado: StateFlow<UiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            _estado.value = withContext(Dispatchers.IO) { cargar() }
        }
    }

    fun crearTurno(prendas: Int, preferente: Boolean) =
        ejecutar { repo.crearTurno(prendas, preferente) }

    fun asignarSiguiente() = ejecutar { repo.asignarSiguiente() }

    fun registrarAbandono(turnoId: Long, motivo: String) =
        ejecutar { repo.registrarAbandono(turnoId, motivo) }

    fun cerrarUso(probadorId: Int, prendasDevueltas: Int) =
        ejecutar { repo.cerrarUso(probadorId, prendasDevueltas) }

    fun liberarProbador(probadorId: Int) = ejecutar { repo.liberarProbador(probadorId) }

    fun alternarFueraDeServicio(probadorId: Int) =
        ejecutar { repo.alternarFueraDeServicio(probadorId) }

    fun consumirMensaje() {
        _estado.value = _estado.value.copy(mensaje = null)
    }

    private fun ejecutar(accion: () -> String) {
        viewModelScope.launch {
            val mensaje = withContext(Dispatchers.IO) {
                try {
                    accion()
                } catch (e: ReglaNegocioException) {
                    e.message ?: "No se pudo completar la acción."
                } catch (e: Exception) {
                    "Error inesperado: ${e.message}"
                }
            }
            val nuevo = withContext(Dispatchers.IO) { cargar() }
            _estado.value = nuevo.copy(mensaje = mensaje)
        }
    }

    private fun cargar(): UiState {
        val probadores = repo.listarProbadores()
        val fila = repo.listarEnEspera()
        val libres = probadores.count { it.estado == EstadoProbador.LIBRE }
        val habilitados = probadores.count { it.estado != EstadoProbador.FUERA_DE_SERVICIO }
        return UiState(
            probadores = probadores,
            enEspera = fila,
            usosActivos = repo.usosActivos().associateBy { it.probadorId },
            indicadores = repo.indicadores(),
            esperaMin = estimarEsperaMinutos(fila.size, libres, habilitados, repo.minutosPorCiclo()),
            cargando = false
        )
    }
}
