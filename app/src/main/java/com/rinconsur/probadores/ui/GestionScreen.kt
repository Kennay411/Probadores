package com.rinconsur.probadores.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rinconsur.probadores.model.EstadoProbador
import com.rinconsur.probadores.model.Probador
import com.rinconsur.probadores.model.Reglas
import com.rinconsur.probadores.model.Turno
import com.rinconsur.probadores.model.UsoActivo
import com.rinconsur.probadores.viewmodel.UiState

/** Pantalla de asistente/vendedor y jefatura (demo, sin autenticación en este MVP). */
@Composable
fun GestionScreen(
    estado: UiState,
    onCrearTurno: (Int, Boolean) -> Unit,
    onAsignarSiguiente: () -> Unit,
    onAbandono: (Long, String) -> Unit,
    onCerrarUso: (Int, Int) -> Unit,
    onLiberar: (Int) -> Unit,
    onAlternarServicio: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var prendas by rememberSaveable { mutableStateOf(1) }
    var preferente by rememberSaveable { mutableStateOf(false) }
    var turnoAbandono by remember { mutableStateOf<Turno?>(null) }
    var probadorCierre by remember { mutableStateOf<Probador?>(null) }
    val ahora by rememberAhora()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Gestión de probadores", style = MaterialTheme.typography.headlineSmall)
        }

        // --- Nuevo turno ---
        item {
            Tarjeta(modifier = Modifier.fillMaxWidth()) {
                Text("Nuevo turno", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Prendas (máximo ${Reglas.MAX_PRENDAS})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(onClick = { if (prendas > 1) { prendas -= 1 } }) { Text("-") }
                    Text(prendas.toString(), style = MaterialTheme.typography.headlineSmall)
                    OutlinedButton(onClick = { if (prendas < Reglas.MAX_PRENDAS) { prendas += 1 } }) {
                        Text("+")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = preferente, onCheckedChange = { preferente = it })
                    Text("Atención preferente", style = MaterialTheme.typography.bodyLarge)
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onCrearTurno(prendas, preferente)
                        prendas = 1
                        preferente = false
                    }
                ) { Text("Crear turno") }
            }
        }

        // --- Fila de espera ---
        item {
            Tarjeta(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Fila de espera (${estado.enEspera.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = estado.enEspera.isNotEmpty() && estado.libres > 0,
                    onClick = onAsignarSiguiente
                ) { Text("Asignar siguiente a probador libre") }
                if (estado.enEspera.isEmpty()) {
                    Text("No hay personas en fila.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        itemsIndexed(estado.enEspera, key = { _, turno -> turno.id }) { indice, turno ->
            Tarjeta(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${indice + 1}. ${turno.codigo}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val detalle = buildString {
                            append("${turno.prendas} prenda(s)")
                            if (turno.preferente) append(" · Preferente")
                        }
                        Text(detalle, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { turnoAbandono = turno }) { Text("Abandonó") }
                }
            }
        }

        // --- Probadores ---
        item {
            Text("Probadores", style = MaterialTheme.typography.titleLarge)
        }
        items(estado.probadores, key = { it.id }) { probador ->
            TarjetaProbadorGestion(
                probador = probador,
                uso = estado.usosActivos[probador.id],
                ahora = ahora,
                onCerrar = { probadorCierre = probador },
                onLiberar = { onLiberar(probador.id) },
                onAlternarServicio = { onAlternarServicio(probador.id) }
            )
        }
    }

    turnoAbandono?.let { turno ->
        DialogoAbandono(
            turno = turno,
            onCancelar = { turnoAbandono = null },
            onConfirmar = { motivo ->
                onAbandono(turno.id, motivo)
                turnoAbandono = null
            }
        )
    }

    probadorCierre?.let { probador ->
        val ingresadas = estado.usosActivos[probador.id]?.prendasIngresadas
        if (ingresadas == null) {
            probadorCierre = null
        } else {
            DialogoCierre(
                probador = probador,
                ingresadas = ingresadas,
                onCancelar = { probadorCierre = null },
                onConfirmar = { devueltas ->
                    onCerrarUso(probador.id, devueltas)
                    probadorCierre = null
                }
            )
        }
    }
}

@Composable
private fun TarjetaProbadorGestion(
    probador: Probador,
    uso: UsoActivo?,
    ahora: Long,
    onCerrar: () -> Unit,
    onLiberar: () -> Unit,
    onAlternarServicio: () -> Unit
) {
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

        if (probador.estado == EstadoProbador.OCUPADO && uso != null) {
            val minutos = ((ahora - uso.inicio) / 60_000L).toInt().coerceAtLeast(0)
            Text(
                "${uso.turnoCodigo} · ${uso.prendasIngresadas} prenda(s) · $minutos min en uso",
                style = MaterialTheme.typography.bodyMedium
            )
            if (minutos >= Reglas.USO_PROLONGADO_MIN) {
                Text(
                    "Uso prolongado: revisar al cliente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        when (probador.estado) {
            EstadoProbador.OCUPADO -> Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCerrar
            ) { Text("Cerrar uso") }

            EstadoProbador.EN_ORDENAMIENTO -> Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onLiberar
            ) { Text("Ordenado: liberar probador") }

            EstadoProbador.LIBRE -> OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAlternarServicio
            ) { Text("Marcar fuera de servicio") }

            EstadoProbador.FUERA_DE_SERVICIO -> OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAlternarServicio
            ) { Text("Volver a habilitar") }
        }
    }
}

@Composable
private fun DialogoAbandono(
    turno: Turno,
    onCancelar: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Abandono de ${turno.codigo}") },
        text = {
            OutlinedTextField(
                value = motivo,
                onValueChange = { motivo = it.take(120) },
                label = { Text("Motivo") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(enabled = motivo.isNotBlank(), onClick = { onConfirmar(motivo) }) {
                Text("Registrar")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoCierre(
    probador: Probador,
    ingresadas: Int,
    onCancelar: () -> Unit,
    onConfirmar: (Int) -> Unit
) {
    var texto by remember { mutableStateOf(ingresadas.toString()) }
    val valor = texto.toIntOrNull()
    val valido = valor != null && valor in 0..ingresadas
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Cerrar uso de ${probador.codigo}") },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("Prendas devueltas") },
                supportingText = { Text("Prendas ingresadas: $ingresadas") },
                isError = !valido,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = { if (valor != null) onConfirmar(valor) }
            ) { Text("Cerrar uso") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
