package com.rinconsur.probadores.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rinconsur.probadores.model.Probador
import com.rinconsur.probadores.viewmodel.UiState

/** Consulta anónima: no pide ni muestra datos personales. */
@Composable
fun PanelScreen(estado: UiState, modifier: Modifier = Modifier) {
    val textoEspera = when (val m = estado.esperaMin) {
        null -> "Sin probadores habilitados"
        0 -> "Sin espera"
        else -> "Aprox. $m min"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Probadores Rincón Sur", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Consulta anónima: no se solicita nombre ni RUT.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TarjetaDato("Probadores libres", estado.libres.toString(), Modifier.weight(1f))
                TarjetaDato("Personas en fila", estado.enEspera.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            TarjetaDato("Espera estimada para un turno nuevo", textoEspera, Modifier.fillMaxWidth())
        }
        items(estado.probadores, key = { it.id }) { probador ->
            FilaProbador(probador)
        }
    }
}

@Composable
private fun FilaProbador(probador: Probador) {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(probador.codigo, style = MaterialTheme.typography.titleMedium)
                if (probador.accesible) {
                    Text("Probador accesible", style = MaterialTheme.typography.bodyMedium)
                }
            }
            EtiquetaEstado(probador.estado)
        }
    }
}
