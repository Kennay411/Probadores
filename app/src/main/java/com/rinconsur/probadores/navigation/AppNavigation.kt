package com.rinconsur.probadores.navigation

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rinconsur.probadores.ui.GestionScreen
import com.rinconsur.probadores.ui.IndicadoresScreen
import com.rinconsur.probadores.ui.PanelScreen
import com.rinconsur.probadores.viewmodel.ProbadoresViewModel

sealed class Ruta(val ruta: String, val titulo: String, val icono: ImageVector) {
    object Panel : Ruta("panel", "Panel", Icons.Filled.Home)
    object Gestion : Ruta("gestion", "Gestión", Icons.Filled.Settings)
    object Metricas : Ruta("indicadores", "Indicadores", Icons.Filled.Info)
}

private val destinos = listOf(Ruta.Panel, Ruta.Gestion, Ruta.Metricas)

@Composable
fun AppNavigation(viewModel: ProbadoresViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.mensaje) {
        val mensaje = estado.mensaje
        if (mensaje != null) {
            snackbar.showSnackbar(mensaje)
            viewModel.consumirMensaje()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                val entrada by navController.currentBackStackEntryAsState()
                val actual = entrada?.destination?.route
                destinos.forEach { destino ->
                    NavigationBarItem(
                        selected = actual == destino.ruta,
                        onClick = {
                            navController.navigate(destino.ruta) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destino.icono, contentDescription = null) },
                        label = { Text(destino.titulo) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Ruta.Panel.ruta,
            modifier = Modifier.padding(padding)
        ) {
            composable(Ruta.Panel.ruta) {
                PanelScreen(estado)
            }
            composable(Ruta.Gestion.ruta) {
                GestionScreen(
                    estado = estado,
                    onCrearTurno = viewModel::crearTurno,
                    onAsignarSiguiente = viewModel::asignarSiguiente,
                    onAbandono = viewModel::registrarAbandono,
                    onCerrarUso = viewModel::cerrarUso,
                    onLiberar = viewModel::liberarProbador,
                    onAlternarServicio = viewModel::alternarFueraDeServicio
                )
            }
            composable(Ruta.Metricas.ruta) {
                IndicadoresScreen(estado)
            }
        }
    }
}
