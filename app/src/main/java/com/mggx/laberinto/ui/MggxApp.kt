package com.mggx.laberinto.ui

import android.opengl.GLSurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.mggx.laberinto.maze.CaveTheme
import com.mggx.laberinto.ui.screens.GameHud
import com.mggx.laberinto.ui.screens.LobbyScreen
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

enum class Screen { LOBBY, TIENDA, EQUIPO, AJUSTES, REGISTRO, JUEGO, RESULTADO }

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
    DisposableEffect(Unit) {
        onDispose { glView.onPause() }
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
        loading = true
        paused = false
        input.paused = true
        result = null
        screen = Screen.JUEGO
    }

    LaunchedEffect(screen, loading) {
        if (screen == Screen.JUEGO && loading) {
            val lvl = save.currentLevel
            val s = withContext(Dispatchers.Default) { GameSession(save, lvl) }
            session = s
            renderer.setSession(s)
            audio.setTrack(CaveAudio.Track.CUEVA, s.theme)
            // Espera a que el renderer termine de armar la geometria del nivel.
            var guard = 0
            while (!renderer.ready && guard < 400) { delay(16); guard++ }
            delay(120)
            input.paused = false
            pad.inGame = true
            loading = false
        }
    }

    fun leaveGame(toScreen: Screen) {
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
        result = ResultData(
            won = s.phase == GameSession.Phase.GANADO,
            level = s.level,
            reward = reward,
            timeMs = s.elapsedMs,
            steps = s.steps,
            nextLevel = save.currentLevel
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
                val lo = save.loadoutList()
                lo.getOrNull(selectedSlotCycle % lo.size.coerceAtLeast(1))?.let { s.useItem(it) }
            }
        }
        pad.onChalk = { session?.dropChalk() }
        pad.onCycleItem = { dir ->
            val n = save.loadoutList().size
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

            // La superficie 3D solo existe mientras se juega.
            if (screen == Screen.JUEGO) {
                AndroidView(
                    factory = { glView.apply { onResume() } },
                    modifier = Modifier.fillMaxSize()
                )
            }

            when (screen) {
                Screen.LOBBY -> LobbyScreen(
                    save = save, refreshKey = refresh,
                    onPlay = { lvl -> save.setCurrentLevel(lvl); startLevel(lvl) },
                    onShop = { screen = Screen.TIENDA },
                    onLoadout = { screen = Screen.EQUIPO },
                    onSettings = { screen = Screen.AJUSTES },
                    onStats = { screen = Screen.REGISTRO }
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
                        ResultScreen(
                            won = r.won, level = r.level,
                            colorBlindMode = save.settings.colorBlindMode,
                            reward = r.reward,
                            timeMs = r.timeMs, steps = r.steps, nextLevel = save.currentLevel,
                            onNext = { startLevel(save.currentLevel) },
                            onRetry = { save.setCurrentLevel(r.level); startLevel(r.level) },
                            onLobby = { screen = Screen.LOBBY; refresh++ },
                            onShop = { screen = Screen.TIENDA; refresh++ }
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
    val reward: GameSession.Reward,
    val timeMs: Long,
    val steps: Int,
    val nextLevel: Int
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
            Box(Modifier.width(240.dp)) {
                CaveBar(1f, Cave.AmberDeep, Modifier.fillMaxSize(), height = 4.dp)
            }
        }
    }
}

/** Cartel de bienvenida la primera vez que se baja a la cueva. */
@Composable
private fun TutorialOverlay(onClose: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Cave.Void.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        StonePanel(Modifier.width(560.dp), glow = Cave.Amber) {
            Column(Modifier.padding(24.dp)) {
                Text("Primeros pasos", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cuatro cosas y arrancamos.",
                    style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                )
                Spacer(Modifier.height(18.dp))
                TutorialLine(IconId.DEDO, "Caminar",
                    "Arrastra en la mitad izquierda de la pantalla. Empuja el joystick a fondo para correr.")
                TutorialLine(IconId.OJO, "Mirar",
                    "Arrastra en la mitad derecha para girar la cabeza.")
                TutorialLine(IconId.SALIDA, "Salir",
                    "Busca la columna de cristal que brilla. Esa es la salida del nivel.")
                TutorialLine(IconId.MOCHILA, "Objetos",
                    "Los que cargaste en Equipo aparecen abajo a la derecha. Un toque y se usan.")
                Spacer(Modifier.height(18.dp))
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
