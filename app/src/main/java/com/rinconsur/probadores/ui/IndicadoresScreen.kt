package com.rinconsur.probadores.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rinconsur.probadores.model.Reglas
import com.rinconsur.probadores.viewmodel.UiState

@Composable
fun IndicadoresScreen(estado: UiState, modifier: Modifier = Modifier) {
    val ind = estado.indicadores
    val textoUso = ind.minutosPromedioUso?.let { "$it min (promedio real de ${ind.usosCerradosTotal} usos)" }
        ?: "${Reglas.USO_REFERENCIA_MIN} min (referencia ficticia; se usa el promedio real desde " +
        "${Reglas.MIN_USOS_PARA_PROMEDIO} usos cerrados)"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Indicadores del día", style = MaterialTheme.typography.headlineSmall)
        TarjetaDato("Turnos emitidos", ind.turnos.toString(), Modifier.fillMaxWidth())
        TarjetaDato("Abandonos", ind.abandonos.toString(), Modifier.fillMaxWidth())
        TarjetaDato("Porcentaje de abandono", "${ind.porcentajeAbandono}%", Modifier.fillMaxWidth())
        TarjetaDato("Usos cerrados hoy", ind.usosCerradosHoy.toString(), Modifier.fillMaxWidth())
        TarjetaDato("Tiempo de uso usado en la estimación", textoUso, Modifier.fillMaxWidth())
        Text(
            "El cálculo de venta potencial perdida queda pendiente de la integración de métricas.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
