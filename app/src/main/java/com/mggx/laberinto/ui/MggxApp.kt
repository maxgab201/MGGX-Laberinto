package com.mggx.laberinto.ui

import android.opengl.GLSurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mggx.laberinto.core.CaveAudio
import com.mggx.laberinto.core.Haptics
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.gl.CaveRenderer
import com.mggx.laberinto.input.GamepadBridge
import com.mggx.laberinto.net.TransporteFirebase
import com.mggx.laberinto.maze.CaveTheme
import com.mggx.laberinto.ui.screens.GameHud
import com.mggx.laberinto.ui.screens.LobbyScreen
import com.mggx.laberinto.ui.screens.MultiplayerScreen
import com.mggx.laberinto.ui.screens.LoadoutScreen
import com.mggx.laberinto.ui.screens.ResultScreen
import com.mggx.laberinto.ui.screens.SettingsScreen
import com.mggx.laberinto.ui.screens.ShopScreen
import com.mggx.laberinto.ui.screens.StatsScreen
import com.mggx.laberinto.ui.icons.CaveIcon
import com.mggx.laberinto.ui.icons.IconId
import com.mggx.laberinto.ui.theme.Cave
import com.mggx.laberinto.ui.theme.MggxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp

enum class Screen { LOBBY, TIENDA, EQUIPO, AJUSTES, REGISTRO, MULTIJUGADOR, JUEGO, RESULTADO }

/**
 * Raiz de la aplicacion: maneja en que pantalla estamos, arma la partida,
 * conecta el mando y sincroniza el audio con lo que pasa en la cueva.
 */
@Composable
fun MggxApp(
    save: SaveData,
    audio: CaveAudio,
    renderer: CaveRenderer,
    input: CaveRenderer.InputState,
    pad: GamepadBridge,
    haptics: Haptics,
    onExit: () -> Unit
) {
    var screen by remember { mutableStateOf(Screen.LOBBY) }
    var refresh by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var session by remember { mutableStateOf<GameSession?>(null) }
    var paused by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ResultData?>(null) }
    var selectedSlotCycle by remember { mutableIntStateOf(0) }
    var showTutorial by remember { mutableStateOf(!save.settings.tutorialDone) }
    /** Sala de multijugador de la partida en curso, o null si se juega solo. */
    var salaEnCurso by remember { mutableStateOf<com.mggx.laberinto.net.MatchLink?>(null) }
    /** El codigo de la sala en curso, para poder volver a mostrarlo. */
    var codigoDeSala by remember { mutableStateOf("") }
    var semillaDeSala by remember { mutableStateOf(0L) }
    /** Nivel que reparte el anfitrion. Es el de la sala, no el desbloqueado. */
    var nivelDeSala by remember { mutableIntStateOf(1) }

    val context = LocalContext.current

    // -------------------------------------------------------- superficie GL
    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(8, 8, 8, 0, 24, 0)
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }
    // El hilo de OpenGL corre mientras se juega y tambien en el lobby, que es
    // una vitrina 3D de la cueva que te espera. En las demas pantallas se
    // apaga para no gastar bateria al pedo.
    DisposableEffect(screen) {
        val vivo = screen == Screen.JUEGO || screen == Screen.LOBBY
        renderer.vitrina = screen == Screen.LOBBY
        if (vivo) glView.onResume() else glView.onPause()
        onDispose { }
    }

    // Vitrina del lobby: se arma una sesion aparte, solo para mirar, del nivel
    // al que vas a bajar. Se rehace solo cuando cambia el nivel elegido.
    var vitrinaLevel by remember { mutableIntStateOf(0) }
    LaunchedEffect(screen, refresh) {
        if (screen != Screen.LOBBY) return@LaunchedEffect
        val lvl = save.currentLevel
        if (vitrinaLevel == lvl) return@LaunchedEffect
        val s = withContext(Dispatchers.Default) { GameSession(save, lvl) }
        s.pause()
        renderer.setSession(s)
        vitrinaLevel = lvl
    }

    // Resolucion interna: dibujar a menos pixeles y estirar es la forma mas
    // barata de ganar cuadros por segundo en telefonos justos.
    LaunchedEffect(refresh, screen) {
        val f = save.settings.renderScale.coerceIn(0.5f, 1f)
        glView.post {
            val w = glView.width
            val h = glView.height
            if (w > 0 && h > 0) {
                if (f < 0.995f) {
                    glView.holder.setFixedSize(
                        (w * f).toInt().coerceAtLeast(320),
                        (h * f).toInt().coerceAtLeast(180)
                    )
                } else {
                    glView.holder.setSizeFromLayout()
                }
            }
        }
    }

    // ------------------------------------------------------------- acciones
    fun startLevel(level: Int) {
        if (loading) return
        // El nivel a jugar queda fijado aca y no en cada lugar que llama:
        // antes se ignoraba el parametro y se leia save.currentLevel, asi que
        // cualquier llamada que se olvidara de fijarlo arrancaba otro nivel.
        save.setCurrentLevel(level)
        // Esta es una partida solitaria: si venias de una sala, se cierra.
        // Sin esto, "Reintentar" despues de una carrera dejaria la sala
        // abierta mandando poses de una partida que ya no existe.
        salaEnCurso?.cerrar()
        salaEnCurso = null
        codigoDeSala = ""
        loading = true
        paused = false
        input.paused = true
        result = null
        screen = Screen.JUEGO
    }

    /**
     * Arranca una partida de a varios: el mismo nivel y la MISMA semilla que
     * repartio el anfitrion, que es lo que hace que los dos telefonos armen
     * exactamente la misma cueva.
     */
    fun startNetLevel(codigo: String, link: com.mggx.laberinto.net.MatchLink, nivel: Int, semilla: Long) {
        if (loading) return
        // OJO: el nivel de la sala NO pasa por save.setCurrentLevel, que lo
        // recorta al maximo que uno tenga desbloqueado. Si el anfitrion baja
        // al 20 y el invitado tiene hasta el 5, el recorte le armaria otra
        // cueva y se cae toda la idea de "misma semilla, mismo laberinto".
        salaEnCurso = link
        codigoDeSala = codigo
        semillaDeSala = semilla
        nivelDeSala = nivel
        loading = true
        paused = false
        input.paused = true
        result = null
        screen = Screen.JUEGO
    }

    LaunchedEffect(screen, loading) {
        if (screen == Screen.JUEGO && loading) {
            val sala = salaEnCurso
            val lvl = if (sala != null) nivelDeSala else save.currentLevel
            val s = withContext(Dispatchers.Default) {
                if (sala != null) GameSession(save, lvl, semillaDeSala).also { it.red = sala }
                else GameSession(save, lvl)
            }
            session = s
            renderer.setSession(s)
            audio.setTrack(CaveAudio.Track.CUEVA, s.theme)
            // Espera a que el renderer levante el nivel y arme su malla.
            // El tope de 8 segundos es solo por las dudas: si el hilo de GL no
            // arranca, es mejor entrar igual que quedarse colgado en la carga.
            var guard = 0
            while (!renderer.ready && guard < 500) { delay(16); guard++ }
            delay(120)
            input.paused = false
            pad.inGame = true
            loading = false
        }
    }

    fun leaveGame(toScreen: Screen) {
        // Al volver al lobby se corta la sala: la partida en red termino.
        salaEnCurso?.cerrar()
        salaEnCurso = null
        // La vitrina del lobby tiene que rearmarse: la sesion que quedaba es la
        // que se acaba de jugar, con las monedas ya levantadas y las trampas
        // descubiertas. Poner el nivel en 0 fuerza una cueva nueva.
        vitrinaLevel = 0
        input.paused = true
        pad.inGame = false
        pad.release()
        paused = false
        session = null
        screen = toScreen
        audio.setTrack(CaveAudio.Track.MENU)
        audio.setIntensity(0f)
        refresh++
    }

    fun finishRun(s: GameSession) {
        val reward = s.settle() ?: return
        // La sala se deja viva si seguia conectada: asi se puede volver a
        // Multijugador y bajar el proximo nivel con el mismo companero, sin
        // tener que armar la sala de nuevo. Se cierra explicitamente si el
        // usuario elige jugar el siguiente nivel solo (startLevel ya lo
        // hace) o volver al lobby/tienda (ver Screen.RESULTADO mas abajo).
        val salaSigueViva = salaEnCurso?.let { !it.cerrado } ?: false
        result = ResultData(
            won = s.phase == GameSession.Phase.GANADO,
            level = s.level,
            reward = reward,
            timeMs = s.elapsedMs,
            steps = s.steps,
            nextLevel = save.currentLevel,
            // El nivel de una sala lo elige el anfitrion y puede estar por
            // encima del que uno tiene desbloqueado. "Reintentar" carga el
            // mas alto que uno SI pueda jugar solo: si no, la pantalla decia
            // "nivel 30" y arrancaba otro completamente distinto.
            reintentar = s.level.coerceAtMost(save.maxLevel),
            salaActiva = salaSigueViva
        )
        input.paused = true
        pad.inGame = false
        pad.release()
        session = null
        screen = Screen.RESULTADO
        audio.setTrack(CaveAudio.Track.MENU)
        audio.setIntensity(0f)
        refresh++
    }

    // --------------------------------------------------- fin de partida
    DisposableEffect(renderer) {
        renderer.onPhaseChanged = { phase ->
            if (phase == GameSession.Phase.GANADO || phase == GameSession.Phase.PERDIDO) {
                haptics.pulse(if (phase == GameSession.Phase.GANADO) 90 else 220)
            }
        }
        onDispose { renderer.onPhaseChanged = null }
    }

    LaunchedEffect(session) {
        val s = session ?: return@LaunchedEffect
        while (true) {
            delay(60)
            // Sonidos que produjo la partida
            while (true) {
                val sfx = renderer.soundOut.poll() ?: break
                audio.play(sfx)
                when (sfx) {
                    GameSession.Sfx.DANO, GameSession.Sfx.TRAMPA -> haptics.pulse(45)
                    GameSession.Sfx.ECO, GameSession.Sfx.ECO_GRANDE ->
                        if (save.settings.vibrateOnPickup) haptics.tick()
                    GameSession.Sfx.VETAGRIS, GameSession.Sfx.COFRE -> haptics.pulse(28)
                    else -> {}
                }
            }
            // La musica se pone tensa cuando queda poca vida
            audio.setIntensity(((0.45f - s.healthFraction()) / 0.45f).coerceIn(0f, 1f))
            if (s.phase == GameSession.Phase.GANADO || s.phase == GameSession.Phase.PERDIDO) {
                delay(650)
                finishRun(s)
                break
            }
        }
    }

    // Sincroniza ajustes que el motor necesita saber
    LaunchedEffect(refresh, screen) {
        input.autoRun = save.settings.autoRun
        input.padSensitivity = save.settings.gamepadSensitivity
    }

    // ------------------------------------------------------------- mando
    DisposableEffect(session, screen) {
        pad.onPause = { if (screen == Screen.JUEGO) { paused = !paused; input.paused = paused } }
        pad.onUseItem = {
            val s = session
            if (s != null) {
                val lo = save.bolsaDeMano()
                lo.getOrNull(selectedSlotCycle % lo.size.coerceAtLeast(1))?.let { id -> renderer.dispatch(s) { s.useItem(id) } }
            }
        }
        pad.onFlashlight = { session?.let { s -> renderer.dispatch(s) { s.toggleLinterna() } } }
        pad.onAttack = { session?.let { s -> renderer.dispatch(s) { s.golpear() } } }
        pad.onCycleItem = { dir ->
            val n = save.bolsaDeMano().size
            if (n > 0) selectedSlotCycle = ((selectedSlotCycle + dir) % n + n) % n
        }
        pad.onBack = {
            when (screen) {
                Screen.LOBBY -> onExit()
                Screen.JUEGO -> { paused = true; input.paused = true }
                Screen.RESULTADO -> { screen = Screen.LOBBY; refresh++ }
                else -> { screen = Screen.LOBBY; refresh++ }
            }
        }
        onDispose { }
    }

    // ------------------------------------------------------------ pintura
    MggxTheme(textScale = save.settings.uiScale) {
        Box(Modifier.fillMaxSize().background(Cave.Void)) {

            // La superficie 3D vive mientras se juega y detras del lobby.
            if (screen == Screen.JUEGO || screen == Screen.LOBBY) {
                AndroidView(
                    factory = { glView },
                    modifier = Modifier.fillMaxSize()
                )
            }

            when (screen) {
                Screen.LOBBY -> LobbyScreen(
                    save = save, refreshKey = refresh,
                    onPlay = { lvl -> startLevel(lvl) },
                    onShop = { screen = Screen.TIENDA },
                    onLoadout = { screen = Screen.EQUIPO },
                    onSettings = { screen = Screen.AJUSTES },
                    onStats = { screen = Screen.REGISTRO },
                    onMultiplayer = { screen = Screen.MULTIJUGADOR }
                )
                Screen.MULTIJUGADOR -> MultiplayerScreen(
                    save = save,
                    abrirTransporte = { codigo -> abrirSala(context, codigo) },
                    diagnostico = { diagnosticoFirebase(context) },
                    onBack = { screen = Screen.LOBBY; refresh++ },
                    onArrancarPartida = { codigo, link, nivel, semilla ->
                        startNetLevel(codigo, link, nivel, semilla)
                    },
                    // Si venimos de terminar un nivel de sala (ver
                    // onContinuarSala en Screen.RESULTADO), la sala sigue
                    // conectada: no hay que crear ni entrar de nuevo.
                    salaExistente = salaEnCurso?.let { l -> codigoDeSala to l },
                    ultimoNivelJugado = salaEnCurso?.let { nivelDeSala to semillaDeSala }
                )
                Screen.TIENDA -> ShopScreen(
                    save = save, refreshKey = refresh,
                    onBack = { screen = Screen.LOBBY; refresh++ },
                    onChanged = { refresh++ },
                    onMessage = { toast = it }
                )
                Screen.EQUIPO -> LoadoutScreen(
                    save = save, refreshKey = refresh,
                    onBack = { screen = Screen.LOBBY; refresh++ },
                    onChanged = { refresh++ },
                    onMessage = { toast = it }
                )
                Screen.AJUSTES -> SettingsScreen(
                    save = save,
                    onBack = { screen = Screen.LOBBY; refresh++ },
                    onChanged = {
                        refresh++
                        input.autoRun = save.settings.autoRun
                        input.padSensitivity = save.settings.gamepadSensitivity
                    },
                    onResetProgress = {
                        save.resetProgress()
                        toast = "Progreso borrado. Arrancas de cero."
                        refresh++
                    }
                )
                Screen.REGISTRO -> StatsScreen(save) { screen = Screen.LOBBY; refresh++ }
                Screen.JUEGO -> {
                    val s = session
                    if (s != null && !loading) {
                        GameHud(
                            dispatch = { action -> renderer.dispatch(s, action) },
                            save = save, session = s, input = input,
                            fps = renderer.fps,
                            onPause = { paused = true; input.paused = true; pad.inGame = false },
                            onQuit = {
                                s.forceLose()
                                finishRun(s)
                            },
                            paused = paused,
                            onResume = { paused = false; input.paused = false; pad.inGame = true }
                        )
                    }
                }
                Screen.RESULTADO -> {
                    val r = result
                    if (r != null) {
                        // Volver al lobby o a la tienda sin elegir seguir
                        // con la sala es abandonarla: si no, quedaria viva
                        // sin que nadie la use hasta que el companero de por
                        // muerto el silencio a los 12 segundos.
                        fun cerrarSalaYSeguirA(destino: Screen) {
                            salaEnCurso?.cerrar()
                            salaEnCurso = null
                            codigoDeSala = ""
                            screen = destino
                            refresh++
                        }
                        ResultScreen(
                            won = r.won, level = r.level,
                            colorBlindMode = save.settings.colorBlindMode,
                            reward = r.reward,
                            timeMs = r.timeMs, steps = r.steps, nextLevel = r.nextLevel,
                            salaActiva = r.salaActiva,
                            onNext = { startLevel(r.nextLevel) },
                            onRetry = { startLevel(r.reintentar) },
                            // Vuelve directo a la sala, que sigue conectada:
                            // MultiplayerScreen la reconoce por salaExistente
                            // y no pide crear ni entrar de nuevo.
                            onContinuarSala = { screen = Screen.MULTIJUGADOR; refresh++ },
                            onLobby = { cerrarSalaYSeguirA(Screen.LOBBY) },
                            onShop = { cerrarSalaYSeguirA(Screen.TIENDA) }
                        )
                    }
                }
            }

            // --------------------------------------------------- primeros pasos
            if (screen == Screen.JUEGO && !loading && showTutorial) {
                TutorialOverlay {
                    save.settings.tutorialDone = true
                    save.save()
                    showTutorial = false
                    input.paused = false
                    pad.inGame = true
                }
                LaunchedEffect(Unit) { input.paused = true; pad.inGame = false }
            }

            // ------------------------------------------------------ carga
            AnimatedVisibility(loading, enter = fadeIn(), exit = fadeOut()) {
                LoadingOverlay(save.currentLevel)
            }

            // ------------------------------------------------------ avisos
            AnimatedVisibility(
                toast != null,
                enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp)
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Cave.Void.copy(alpha = 0.88f))
                        .padding(horizontal = 20.dp, vertical = 11.dp)
                ) {
                    Text(
                        toast ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = Cave.AmberSoft
                    )
                }
            }
            LaunchedEffect(toast) {
                if (toast != null) { delay(2200); toast = null }
            }
        }
    }
}

private class ResultData(
    val won: Boolean,
    val level: Int,
    /** Nivel que carga el boton "Reintentar" (puede no ser el jugado). */
    val reintentar: Int,
    val reward: GameSession.Reward,
    val timeMs: Long,
    val steps: Int,
    val nextLevel: Int,
    /** Si la sala de multijugador sigue conectada, para ofrecer seguir con ella. */
    val salaActiva: Boolean
)

@Composable
private fun LoadingOverlay(level: Int) {
    val theme = CaveTheme.forLevel(level)
    Box(
        Modifier.fillMaxSize().background(Cave.Void),
        contentAlignment = Alignment.Center
    ) {
        CaveBackdrop(seed = level * 13 + 3)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DESCENDIENDO", style = MaterialTheme.typography.headlineLarge, color = Cave.Amber)
            Spacer(Modifier.height(8.dp))
            Text("Nivel $level - ${theme.displayName}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                theme.description,
                style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
            )
            Spacer(Modifier.height(22.dp))
            Row {
                Text("Excavando la roca...", style = MaterialTheme.typography.bodyMedium, color = Cave.TextDim)
            }
            Spacer(Modifier.height(10.dp))
            // Barra indeterminada: va y viene mientras se arma el nivel.
            val t = rememberInfiniteTransition(label = "carga")
            val p by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
                label = "avance"
            )
            Box(
                Modifier
                    .width(240.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Cave.Void)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.32f)
                        .offset(x = (208 * p).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Cave.Amber)
                )
            }
        }
    }
}

/** Cartel de bienvenida la primera vez que se baja a la cueva. */
@Composable
private fun TutorialOverlay(onClose: () -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Cave.Void.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        // El cartel crecio a siete puntos y en una pantalla chica (el juego va
        // apaisado) no entraba entero: el boton de cerrar quedaba fuera de
        // vista y parecia que no se podia sacar. Ahora el texto scrollea y el
        // boton de cerrar queda siempre fijo abajo, visible.
        val altoMax = maxHeight * 0.86f
        StonePanel(Modifier.width(560.dp).heightIn(max = altoMax), glow = Cave.Amber) {
            Column(Modifier.padding(24.dp)) {
                Text("Primeros pasos", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Siete cosas y arrancamos.",
                    style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                ) {
                    TutorialLine(IconId.DEDO, "Caminar",
                        "Arrastra en la mitad izquierda de la pantalla. Empuja el joystick a fondo para correr.")
                    TutorialLine(IconId.OJO, "Mirar",
                        "Arrastra en la mitad derecha para girar la cabeza.")
                    TutorialLine(IconId.AGACHARSE, "Agacharte y saltar",
                        "El boton de agacharse cicla de pie, agachado y bien raso. En los tramos " +
                            "bajos te agachas solo, y agacharte no gasta aguante.")
                    TutorialLine(IconId.CALAVERA, "Cuidado",
                        "Hay trampas y bichos. Los pinches y los pozos se ven; los bichos hacen " +
                            "ruido antes de encontrarte.")
                    TutorialLine(IconId.PUNO, "Pelear",
                        "Al bicho que se te viene encima le pegas con el boton del puno. Sin arma " +
                            "pegas flojo pero pegas; en la Tienda hay palos, picos y hachas.")
                    TutorialLine(IconId.SALIDA, "Salir",
                        "Busca la columna de cristal que brilla. Esa es la salida del nivel.")
                    TutorialLine(IconId.MOCHILA, "Objetos",
                        "Los que cargaste en Equipo aparecen abajo a la derecha. Un toque y se usan.")
                }
                Spacer(Modifier.height(14.dp))
                CaveButton(
                    "Entendido, a bajar", onClose,
                    icon = IconId.TILDE, style = CaveButtonStyle.PRIMARIO,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Esto se muestra una sola vez. Si usas un mando, en Ajustes esta la lista completa de botones.",
                    style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                )
            }
        }
    }
}

@Composable
private fun TutorialLine(icon: IconId, title: String, text: String) {
    Row(Modifier.padding(vertical = 7.dp)) {
        CaveIcon(icon, size = 26.dp, tint = Cave.AmberSoft, accent = Cave.Amber)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Cave.TextDim)
        }
    }
}

/**
 * Abre la sala de multijugador, o devuelve el motivo por el que no pudo.
 *
 * Antes esto era un `runCatching { ... }.getOrNull()` y la pantalla siempre
 * decia "fijate que tengas internet", que es una conclusion apurada: la
 * conexion todavia no se intento siquiera. Si algo falla aca es porque la app
 * no encuentra la configuracion de Firebase, y eso no tiene nada que ver con
 * la senal del telefono. Ahora vuelve el motivo de verdad, que es lo unico
 * que sirve para arreglarlo.
 */
private fun abrirSala(
    context: android.content.Context,
    codigo: String
): Pair<com.mggx.laberinto.net.Transporte?, String?> {
    // Normalmente de esto se encarga solo un ContentProvider que Firebase
    // mete en el manifest, pero hay telefonos (y arranques raros) donde no
    // llega a correr. Llamarlo a mano es inofensivo: si ya estaba iniciado
    // no hace nada, y si faltara la configuracion devuelve null en vez de
    // reventar.
    val app = runCatching {
        com.google.firebase.FirebaseApp.getInstance()
    }.getOrElse {
        runCatching { com.google.firebase.FirebaseApp.initializeApp(context) }.getOrNull()
    }
    if (app == null) {
        // Sin esto el cartel decia siempre "se compilo sin el
        // google-services.json", que es una conclusion y no un dato: son dos
        // problemas distintos con arreglos distintos. Si el APK viene sin la
        // configuracion hay que recompilarlo; si la trae y aun asi Firebase no
        // arranca, recompilar no cambia nada y el laburo esta en otro lado.
        return null to if (appIdDeFirebase(context) == null) {
            "Este APK se compilo sin la configuracion de Firebase " +
                "(falta google-services.json). Hay que compilarlo de nuevo con " +
                "el archivo puesto: el multijugador no puede andar."
        } else {
            "La configuracion de Firebase esta en el APK, pero el SDK no " +
                "arranco igual. Probá cerrar la app del todo y volver a abrirla."
        }
    }
    return try {
        com.mggx.laberinto.net.TransporteFirebase(codigo) to null
    } catch (t: Throwable) {
        // El mensaje crudo del SDK: feo de leer, pero dice exactamente que
        // falta (la URL de la base, el permiso, la app sin registrar).
        null to "No se pudo abrir la sala: ${t.javaClass.simpleName}: ${t.message}"
    }
}

/**
 * Que ve la app de su propia configuracion de Firebase.
 *
 * Se muestra en la pantalla de multijugador cuando algo falla. Es la
 * diferencia entre "no anda" y saber en un vistazo si el problema es que la
 * app no encuentra la base, si apunta a otro proyecto, o si el paquete no es
 * el que esta registrado en la consola.
 */
private fun diagnosticoFirebase(context: android.content.Context): String {
    val app = runCatching { com.google.firebase.FirebaseApp.getInstance() }.getOrNull()
        ?: return "Firebase: NO iniciado | " +
            "Config en el APK: ${if (appIdDeFirebase(context) != null) "SI" else "NO"} | " +
            "Paquete: ${context.packageName}"
    val o = app.options
    return "Base: ${o.databaseUrl ?: "(ninguna)"} | " +
        "Proyecto: ${o.projectId ?: "(ninguno)"} | " +
        "Paquete: ${context.packageName}"
}

/**
 * El id de app de Firebase tal como quedo adentro del APK, o null si no esta.
 *
 * Es la misma pregunta que se hace el SDK al arrancar, hecha aparte: el plugin
 * de Google convierte el google-services.json en <string> comunes, y el SDK los
 * busca de nombre. Preguntarlo por separado es lo que permite distinguir "el
 * APK salio sin la configuracion" de "la configuracion esta y el problema es
 * otro", que desde afuera se ven igual: multijugador que no abre.
 */
private fun appIdDeFirebase(context: android.content.Context): String? {
    val id = context.resources.getIdentifier("google_app_id", "string", context.packageName)
    if (id == 0) return null
    return runCatching { context.getString(id) }.getOrNull()?.takeIf { it.isNotBlank() }
}

