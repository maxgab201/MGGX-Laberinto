package com.mggx.laberinto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mggx.laberinto.net.NetProtocol
import com.mggx.laberinto.ui.CaveBackdrop
import com.mggx.laberinto.ui.CaveButton
import com.mggx.laberinto.ui.CaveButtonStyle
import com.mggx.laberinto.ui.SectionTitle
import com.mggx.laberinto.ui.StonePanel
import com.mggx.laberinto.ui.icons.CaveIcon
import com.mggx.laberinto.ui.icons.IconId
import com.mggx.laberinto.ui.theme.Cave

/**
 * Pantalla del multijugador que todavia no esta.
 *
 * No tiene ningun boton que no haga nada: lo unico que se puede hacer aca es
 * leer como va a funcionar y volver. Cuando el modo este listo, esta misma
 * pantalla pasa a ser la sala.
 */
@Composable
fun MultiplayerScreen(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        CaveBackdrop(seed = 77)
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CaveButton("Volver", onBack, icon = IconId.ATRAS, style = CaveButtonStyle.FANTASMA)
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text("Bajar acompañado", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Dos o mas mineros en la misma cueva.",
                        style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Cave.Amber.copy(alpha = 0.18f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CaveIcon(IconId.RELOJ, size = 16.dp, tint = Cave.Amber, accent = Cave.AmberSoft)
                        Spacer(Modifier.width(8.dp))
                        Text("PROXIMAMENTE", fontSize = 12.sp, color = Cave.Amber)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StonePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("Las dos formas de jugar")
                            Spacer(Modifier.height(12.dp))
                            NetProtocol.Modo.entries.forEach { modo ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    CaveIcon(
                                        IconId.MULTIJUGADOR, size = 26.dp,
                                        tint = Cave.AmberSoft, accent = Cave.Amber
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(modo.etiqueta, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            modo.explicacion,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Cave.TextDim
                                        )
                                    }
                                }
                            }
                        }
                    }
                    StonePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("Por que va a andar bien")
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "El laberinto no viaja por internet. Con el numero de nivel y " +
                                    "una semilla, los dos telefonos arman exactamente la misma " +
                                    "cueva: los mismos pasillos, las mismas monedas, las mismas " +
                                    "trampas y el mismo relieve.\n\n" +
                                    "Por la red solo va lo poquito que cambia: donde esta cada " +
                                    "uno, y los hechos que tocan el mundo (agarre esta moneda, " +
                                    "rompi esta pared, pise esta trampa). Son mensajes de menos " +
                                    "de cien caracteres, asi que anda hasta con senal fea.",
                                style = MaterialTheme.typography.bodyMedium, color = Cave.TextDim
                            )
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StonePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("Como viene la mano")
                            Spacer(Modifier.height(12.dp))
                            Paso(true, "El protocolo de mensajes", "Listo y probado")
                            Paso(true, "El estado de la sala", "Listo y probado")
                            Paso(true, "Cuevas identicas en los dos telefonos", "Listo y probado")
                            Paso(true, "La conexion en si (el relay)", "Listo, con Firebase")
                            Paso(false, "La pantalla de sala y de invitar", "Falta")
                            Paso(false, "Dibujar al otro minero adentro de la cueva", "Falta")
                        }
                    }
                    StonePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("Para armarlo")
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "En el repositorio hay una guia paso a paso, en criollo, con " +
                                    "todo lo que falta hacer y en que orden: el archivo " +
                                    "docs/MULTIJUGADOR.md. Incluye que servicio conviene, cuanto " +
                                    "sale (nada, para empezar) y que archivo hay que tocar en " +
                                    "cada paso.",
                                style = MaterialTheme.typography.bodyMedium, color = Cave.TextDim
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Paso(hecho: Boolean, que: String, estado: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        CaveIcon(
            if (hecho) IconId.TILDE else IconId.RELOJ,
            size = 18.dp,
            tint = if (hecho) Cave.Good else Cave.TextFaint,
            accent = if (hecho) Cave.Good else Cave.AmberDeep
        )
        Spacer(Modifier.width(11.dp))
        Text(
            que, style = MaterialTheme.typography.bodyLarge,
            color = if (hecho) Cave.Text else Cave.TextDim,
            modifier = Modifier.weight(1f)
        )
        Text(estado, fontSize = 11.sp, color = if (hecho) Cave.Good else Cave.TextFaint)
    }
}
