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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.net.CodigoSala
import com.mggx.laberinto.net.MatchLink
import com.mggx.laberinto.net.NetProtocol
import com.mggx.laberinto.net.Transporte
import com.mggx.laberinto.ui.CaveBackdrop
import com.mggx.laberinto.ui.CaveButton
import com.mggx.laberinto.ui.CaveButtonStyle
import com.mggx.laberinto.ui.CaveTextField
import com.mggx.laberinto.ui.SectionTitle
import com.mggx.laberinto.ui.StonePanel
import com.mggx.laberinto.ui.icons.CaveIcon
import com.mggx.laberinto.ui.icons.IconId
import com.mggx.laberinto.ui.theme.Cave
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * La sala de multijugador: armar una, entrar a una, y arrancar.
 *
 * El codigo de sala es todo lo que hace falta para juntarse: es el nombre del
 * canal por el que hablan los telefonos, no hay cuentas ni listas de amigos.
 * Uno crea la sala, le dicta las cuatro letras al otro, y ya esta.
 *
 * [abrirTransporte] es lo unico que sabe de red: se le pide un transporte
 * para un codigo de sala. Asi esta pantalla se puede mirar y probar sin
 * Firebase, y el dia que se cambie de servicio no hay que tocarla.
 */
@Composable
fun MultiplayerScreen(
    save: SaveData,
    /** Devuelve el transporte, o null y el motivo por el que no se pudo. */
    abrirTransporte: (String) -> Pair<Transporte?, String?>,
    /** Que ve la app de su config de Firebase. Se muestra si algo falla. */
    diagnostico: () -> String,
    onBack: () -> Unit,
    onArrancarPartida: (MatchLink, Int, Long) -> Unit
) {
    var nombre by remember { mutableStateOf(save.nombreJugador.ifBlank { "Minero" }) }
    var codigoEscrito by remember { mutableStateOf("") }
    var sala by remember { mutableStateOf<String?>(null) }
    var link by remember { mutableStateOf<MatchLink?>(null) }
    var modo by remember { mutableStateOf(NetProtocol.Modo.CARRERA) }
    var error by remember { mutableStateOf<String?>(null) }
    // Sube en cada latido de red para que la lista de la sala se repinte.
    var latido by remember { mutableIntStateOf(0) }

    val nivel = save.currentLevel

    fun entrarA(codigo: String, comoAnfitrion: Boolean) {
        val (t, motivo) = abrirTransporte(codigo)
        if (t == null) {
            // El motivo de verdad, no una suposicion: antes decia siempre
            // "fijate que tengas internet", que mandaba a buscar el problema
            // al lugar equivocado.
            error = motivo ?: "No se pudo abrir la sala."
            return
        }
        error = null
        // Recien aca se guarda el nombre: hacerlo en cada tecla reescribia el
        // perfil entero (un JSON) letra por letra.
        save.setNombreJugador(nombre)
        link = MatchLink(
            transporte = t,
            yo = CodigoSala.idDeJugador(Random),
            nombre = nombre.ifBlank { "Minero" },
            skin = save.cosmeticSkin,
            anfitrion = comoAnfitrion
        )
        sala = codigo
    }

    fun salirDeLaSala() {
        link?.cerrar()
        link = null
        sala = null
    }

    // El reloj de la sala: mientras se espera a los demas, hay que seguir
    // mandando latidos (si no te dan por ido) y recibiendo a los que entran.
    LaunchedEffect(link) {
        val l = link ?: return@LaunchedEffect
        while (true) {
            delay(100)
            l.latir(0.1f)
            l.errorConexion?.let { error = it }
            latido++
            // Si el anfitrion reparte la partida, todos bajan a la cueva.
            if (l.match.arrancada) {
                onArrancarPartida(l, l.match.nivel, l.match.semilla)
                break
            }
        }
    }

    // Si te vas de la pantalla sin salir a mano, igual hay que avisar: si no,
    // los demas te ven como un fantasma en la lista por 12 segundos.
    DisposableEffect(Unit) {
        onDispose { link?.let { if (!it.match.arrancada) it.cerrar() } }
    }

    Box(Modifier.fillMaxSize()) {
        CaveBackdrop(seed = 77)
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {

            // ------------------------------------------------------ encabezado
            Row(verticalAlignment = Alignment.CenterVertically) {
                CaveButton(
                    "Volver",
                    { salirDeLaSala(); onBack() },
                    icon = IconId.ATRAS, style = CaveButtonStyle.FANTASMA
                )
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text("Bajar acompañado", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (sala == null) "Armá una sala o entrá con un código."
                        else "Nivel $nivel. Cuando estén todos, arranca el anfitrión.",
                        style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxSize()) {
                // ------------------------------------------- columna izquierda
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (error != null) {
                        StonePanel(Modifier.fillMaxWidth(), glow = Cave.Bad) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                CaveIcon(IconId.CALAVERA, size = 20.dp, tint = Cave.Bad, accent = Cave.Bad)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        error ?: "", style = MaterialTheme.typography.bodyMedium,
                                        color = Cave.Text
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    // Lo que la app ve de su propia configuracion. Sin
                                    // esto, "no anda" no se puede arreglar: no hay
                                    // forma de saber si le falta la base, si apunta a
                                    // otro proyecto o si el paquete no coincide.
                                    Text(
                                        diagnostico(), fontSize = 11.sp, color = Cave.TextFaint
                                    )
                                }
                            }
                        }
                    }

                    if (sala == null) SalaNueva(
                        nombre = nombre,
                        onNombre = { nombre = it },
                        codigo = codigoEscrito,
                        onCodigo = { codigoEscrito = it },
                        onCrear = { entrarA(CodigoSala.nuevo(Random), true) },
                        onEntrar = { entrarA(CodigoSala.normalizar(codigoEscrito), false) }
                    ) else EnLaSala(
                        codigo = sala ?: "",
                        link = link,
                        latido = latido,
                        onSalir = { salirDeLaSala() }
                    )
                }

                Spacer(Modifier.width(14.dp))

                // ---------------------------------------------- columna derecha
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StonePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("Cómo se juega")
                            Spacer(Modifier.height(12.dp))
                            NetProtocol.Modo.entries.forEach { m ->
                                val elegido = m == modo
                                val puedeElegir = link?.anfitrion != false
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (elegido) Cave.Amber.copy(alpha = 0.16f)
                                            else Cave.Void.copy(alpha = 0.25f)
                                        )
                                        .padding(12.dp)
                                ) {
                                    CaveIcon(
                                        if (elegido) IconId.TILDE else IconId.MULTIJUGADOR,
                                        size = 24.dp,
                                        tint = if (elegido) Cave.Good else Cave.TextFaint,
                                        accent = if (elegido) Cave.Good else Cave.AmberDeep
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(m.etiqueta, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            m.explicacion,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Cave.TextDim
                                        )
                                    }
                                    if (puedeElegir && !elegido) {
                                        Spacer(Modifier.width(8.dp))
                                        CaveButton("Elegir", { modo = m }, style = CaveButtonStyle.FANTASMA)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            if (link?.anfitrion == false) {
                                Text(
                                    "El modo lo elige el que armó la sala.",
                                    style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                                )
                            }
                        }
                    }

                    // El botón de arrancar es solo del anfitrión.
                    val l = link
                    if (l != null && l.anfitrion) {
                        StonePanel(Modifier.fillMaxWidth(), glow = Cave.Amber) {
                            Column(Modifier.padding(16.dp)) {
                                SectionTitle("Bajar a la cueva")
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Van a jugar el nivel $nivel, los dos en la misma cueva. " +
                                        "Se puede arrancar aunque estés solo: los que entren " +
                                        "después de que arranque quedan para la próxima.",
                                    style = MaterialTheme.typography.bodyMedium, color = Cave.TextDim
                                )
                                Spacer(Modifier.height(12.dp))
                                CaveButton(
                                    "Empezar (${l.match.cantidad()} en la sala)",
                                    {
                                        // La semilla la sortea el anfitrion y viaja con
                                        // el arranque: es lo que hace que los dos armen
                                        // exactamente la misma cueva.
                                        l.arrancar(modo, nivel, Random.nextLong())
                                    },
                                    icon = IconId.SALIDA, style = CaveButtonStyle.PRIMARIO,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    StonePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("Por qué anda bien")
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "El laberinto no viaja por internet. Con el número de nivel y " +
                                    "una semilla, los dos teléfonos arman exactamente la misma " +
                                    "cueva: los mismos pasillos, las mismas monedas, las mismas " +
                                    "trampas y el mismo relieve.\n\n" +
                                    "Por la red solo va lo poquito que cambia: dónde está cada " +
                                    "uno, y los hechos que tocan el mundo. Son mensajes de menos " +
                                    "de cien caracteres, así que anda hasta con señal fea.",
                                style = MaterialTheme.typography.bodyMedium, color = Cave.TextDim
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Antes de entrar a ninguna sala: elegir nombre, y crear o entrar. */
@Composable
private fun SalaNueva(
    nombre: String,
    onNombre: (String) -> Unit,
    codigo: String,
    onCodigo: (String) -> Unit,
    onCrear: () -> Unit,
    onEntrar: () -> Unit
) {
    StonePanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle("Tu nombre")
            Spacer(Modifier.height(10.dp))
            CaveTextField(
                value = nombre, onValueChange = onNombre,
                placeholder = "Minero", maxChars = 24,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Así te ven los demás en la cueva.",
                style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }

    StonePanel(Modifier.fillMaxWidth(), glow = Cave.Amber) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle("Armar una sala")
            Spacer(Modifier.height(8.dp))
            Text(
                "Te da un código de cuatro letras para dictarle a tu amigo.",
                style = MaterialTheme.typography.bodyMedium, color = Cave.TextDim
            )
            Spacer(Modifier.height(12.dp))
            CaveButton(
                "Crear sala", onCrear,
                icon = IconId.MULTIJUGADOR, style = CaveButtonStyle.PRIMARIO,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    StonePanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle("Entrar a una sala")
            Spacer(Modifier.height(8.dp))
            Text(
                "Escribí el código que te pasaron.",
                style = MaterialTheme.typography.bodyMedium, color = Cave.TextDim
            )
            Spacer(Modifier.height(12.dp))
            CaveTextField(
                value = codigo, onValueChange = { onCodigo(CodigoSala.normalizar(it)) },
                placeholder = "- - - -", maxChars = CodigoSala.LARGO,
                centrado = true, fontSize = 26.sp, mayusculas = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            CaveButton(
                "Entrar", onEntrar,
                icon = IconId.ATRAS,
                style = if (CodigoSala.valido(codigo)) CaveButtonStyle.PRIMARIO
                else CaveButtonStyle.FANTASMA,
                enabled = CodigoSala.valido(codigo),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Ya adentro de la sala: el código para dictar y quién hay. */
@Composable
private fun EnLaSala(
    codigo: String,
    link: MatchLink?,
    latido: Int,
    onSalir: () -> Unit
) {
    StonePanel(Modifier.fillMaxWidth(), glow = Cave.Amber) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle("El código de la sala")
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Cave.Void.copy(alpha = 0.6f))
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    codigo, fontSize = 44.sp, color = Cave.Amber,
                    letterSpacing = 14.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Dictáselo a tu amigo. Tiene que poner ese código en “Entrar a una sala”.",
                style = MaterialTheme.typography.bodyMedium, color = Cave.TextDim
            )
        }
    }

    StonePanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            // `latido` no se usa para nada mas que esto: nombrarlo obliga a
            // Compose a repintar la lista cada vez que la red trae novedades.
            SectionTitle("En la sala (${link?.match?.cantidad() ?: 0})")
            Spacer(Modifier.height(4.dp))
            Text(
                "actualizado hace un instante",
                style = MaterialTheme.typography.bodyMedium,
                color = if (latido >= 0) Cave.TextFaint else Cave.TextFaint
            )
            Spacer(Modifier.height(8.dp))
            val todos = link?.match?.todos().orEmpty()
            if (todos.isEmpty()) {
                Text(
                    "Todavía no hay nadie más. Esperá a que entren con el código.",
                    style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                )
            }
            for (j in todos) {
                val soyYo = j.id == link?.yo
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CaveIcon(
                        IconId.MULTIJUGADOR, size = 24.dp,
                        tint = if (soyYo) Cave.Good else Cave.AmberSoft,
                        accent = if (soyYo) Cave.Good else Cave.Amber
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        j.nombre, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (soyYo) {
                        Text("vos", fontSize = 12.sp, color = Cave.Good)
                    } else if (link?.anfitrion == true) {
                        Text("invitado", fontSize = 12.sp, color = Cave.TextFaint)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            CaveButton(
                "Salir de la sala", onSalir,
                icon = IconId.ATRAS, style = CaveButtonStyle.FANTASMA,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

