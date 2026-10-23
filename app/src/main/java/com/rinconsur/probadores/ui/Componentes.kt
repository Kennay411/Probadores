package com.rinconsur.probadores.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.ui.theme.FueraServicioFondo
import com.rinconsur.probadores.ui.theme.FueraServicioTexto
import com.rinconsur.probadores.ui.theme.LibreFondo
import com.rinconsur.probadores.ui.theme.LibreTexto
import com.rinconsur.probadores.ui.theme.OcupadoFondo
import com.rinconsur.probadores.ui.theme.OcupadoTexto
import com.rinconsur.probadores.ui.theme.OrdenamientoFondo
import com.rinconsur.probadores.ui.theme.OrdenamientoTexto
import kotlinx.coroutines.delay

@Composable
fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = contenido
        )
    }
}

@Composable
fun TarjetaDato(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Tarjeta(modifier = modifier) {
        Text(titulo, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.titleLarge)
    }
}

/** El estado se comunica con texto además de color (accesibilidad). */
@Composable
fun EtiquetaEstado(estado: EstadoProbador) {
    val (fondo, texto) = when (estado) {
        EstadoProbador.LIBRE -> LibreFondo to LibreTexto
        EstadoProbador.OCUPADO -> OcupadoFondo to OcupadoTexto
        EstadoProbador.EN_ORDENAMIENTO -> OrdenamientoFondo to OrdenamientoTexto
        EstadoProbador.FUERA_DE_SERVICIO -> FueraServicioFondo to FueraServicioTexto
    }
    Surface(color = fondo, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = estado.etiqueta,
            color = texto,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Hora actual que se refresca cada 30 s para mostrar tiempos de uso. */
@Composable
fun rememberAhora(): State<Long> = produceState(initialValue = System.currentTimeMillis()) {
    while (true) {
        delay(30_000)
        value = System.currentTimeMillis()
    }
}
