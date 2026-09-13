package com.mggx.laberinto.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.maze.CaveTheme
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Musica y efectos generados por sintesis en tiempo real. No hay ni un archivo
 * de audio en el APK: todo sale de osciladores, ruido filtrado y un eco largo
 * que le da el aire de caverna.
 *
 * La musica es ambiental: un drone grave, un pad que respira, gotas de agua
 * y una melodia lenta en escala pentatonica menor. Cada tema de cueva cambia
 * la tonica y el color del pad.
 */
class CaveAudio(private val save: SaveData) {

    companion object {
        const val RATE = 44100
        private const val BLOCK = 1024
        /** Semitonos de la pentatonica menor. */
        private val PENTA = intArrayOf(0, 3, 5, 7, 10, 12, 15)
    }

    enum class Track { MENU, CUEVA }

    private var audio: AudioTrack? = null
    private var thread: Thread? = null

    /**
     * Una corrida del motor de audio: su AudioTrack y su propia bandera de
     * "segui andando".
     *
     * Existe para que cada hilo tenga la SUYA. Con una bandera compartida, un
     * hilo viejo que todavia no murio revive en cuanto alguien vuelve a
     * arrancar el audio (ver [stop]).
     */
    private class Sesion(val at: AudioTrack) {
        @Volatile var viva = true
    }

    private var sesion: Sesion? = null
    private val running = AtomicBoolean(false)
    private val sfxQueue = ConcurrentLinkedQueue<GameSession.Sfx>()
    private val rnd = Random(0xC0FFEE)

    @Volatile private var track: Track = Track.MENU
    @Volatile private var themeIndex: Int = 0
    @Volatile private var musicGain = 0f
    @Volatile private var targetMusicGain = 0f
    @Volatile private var intensity = 0f          // 0 calmo .. 1 tenso

    // --- estado del sintetizador
    private var t = 0.0
    private var dronePhase = 0.0
    private var dronePhase2 = 0.0
    private var padPhase = DoubleArray(4)
    private var melodyPhase = 0.0
    private var melodyEnv = 0.0
    private var melodyFreq = 0.0
    private var nextMelodyAt = 2.0
    private var nextDropAt = 4.0
    private var dropEnv = 0.0
    private var dropPhase = 0.0
    private var dropFreq = 900.0
    private var lp = 0f
    private var lpNoise = 0f

    // --- eco de caverna (delay estereo con realimentacion)
    private val delayL = FloatArray((RATE * 0.37f).toInt())
    private val delayR = FloatArray((RATE * 0.52f).toInt())
    private var dIdxL = 0
    private var dIdxR = 0

    // --- voces de efectos
    private class Voice {
        var active = false
        var kind = 0
        var age = 0.0
        var dur = 0.0
        var phase = 0.0
        var phase2 = 0.0
        var freq = 440.0
        var freq2 = 660.0
        var amp = 0.5
        var noise = 0f
        var pan = 0.5f
    }
    private val voices = Array(10) { Voice() }

    private val out = ShortArray(BLOCK * 2)

    // ------------------------------------------------------------ control

    fun start() {
        // Se mira la sesion y no solo `running`: si quedo una corrida viva, dos
        // motores escribiendo en paralelo suenan a ruido.
        if (running.get() || sesion != null) return
        val minBuf = AudioTrack.getMinBufferSize(
            RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = max(minBuf, BLOCK * 8)
        val at = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audio = at
        at.play()
        running.set(true)
        val s = Sesion(at)
        sesion = s
        thread = Thread({ loop(s) }, "MggxAudio").apply {
            priority = Thread.NORM_PRIORITY + 1
            start()
        }
    }

    /**
     * Corta el audio.
     *
     * Dos cosas que parecen detalles y no lo son:
     *
     * 1. **El hilo mira SU PROPIA bandera, no la compartida.** Antes miraba
     *    `running`, que es una sola para toda la clase. Si `join` vencia
     *    (escribir en un AudioTrack BLOQUEA hasta que haya lugar en el buffer,
     *    asi que medio segundo no siempre alcanza) el hilo viejo quedaba vivo;
     *    y en cuanto alguien llamaba a `start()` —volver de una pausa, nada
     *    mas— `running` volvia a true y **el hilo zombi revivia**. Cada
     *    pausa/reanudacion podia dejar uno colgado, todos escribiendo audio a
     *    la vez.
     *
     * 2. **El AudioTrack lo suelta el hilo que lo usa, no el que corta.** Antes
     *    `stop()` lo liberaba apenas vencia el `join`, con el hilo todavia
     *    escribiendo adentro: uso despues de liberar. Ahora el que escribe es
     *    el que cierra, en su `finally`, asi que no hay forma de que se
     *    escriba en uno ya soltado, venza el `join` o no.
     */
    fun stop() {
        running.set(false)
        val s = sesion
        sesion = null
        audio = null
        // Esto es lo que de verdad para el hilo, incluso si despues alguien
        // llama a start() y prende `running` de nuevo.
        s?.viva = false
        thread?.join(500)
        thread = null
    }

    fun setTrack(newTrack: Track, theme: CaveTheme? = null) {
        track = newTrack
        theme?.let { themeIndex = it.ordinal }
        targetMusicGain = 1f
    }

    fun setIntensity(v: Float) { intensity = v.coerceIn(0f, 1f) }

    fun play(sfx: GameSession.Sfx) {
        if (sfxQueue.size < 24) sfxQueue.add(sfx)
    }

    // ------------------------------------------------------------ motor

    private fun loop(s: Sesion) {
        try {
            while (s.viva) {
                try {
                    render()
                    s.at.write(out, 0, out.size)
                } catch (e: Throwable) {
                    // Nunca tirar la app por un problema de audio.
                    try { Thread.sleep(20) } catch (_: InterruptedException) { break }
                }
            }
        } finally {
            // Lo suelta el que lo usa. Ver el comentario largo en [stop].
            try { s.at.pause(); s.at.flush(); s.at.release() } catch (_: Throwable) {}
        }
    }

    private fun allocVoice(): Voice? {
        for (v in voices) if (!v.active) return v
        // Reemplaza la mas vieja
        var oldest: Voice? = null
        for (v in voices) if (oldest == null || v.age > oldest!!.age) oldest = v
        return oldest
    }

    private fun spawnSfx(s: GameSession.Sfx) {
        val v = allocVoice() ?: return
        v.active = true; v.age = 0.0; v.phase = 0.0; v.phase2 = 0.0; v.noise = 0f
        v.pan = 0.5f
        when (s) {
            GameSession.Sfx.PASO -> { v.kind = 0; v.dur = 0.16; v.amp = 0.30; v.freq = 120.0 + rnd.nextDouble() * 50 }
            GameSession.Sfx.ECO -> { v.kind = 1; v.dur = 0.34; v.amp = 0.28; v.freq = 880.0; v.freq2 = 1320.0 }
            GameSession.Sfx.ECO_GRANDE -> { v.kind = 1; v.dur = 0.62; v.amp = 0.38; v.freq = 660.0; v.freq2 = 990.0 }
            GameSession.Sfx.VETAGRIS -> { v.kind = 2; v.dur = 1.30; v.amp = 0.40; v.freq = 523.25; v.freq2 = 783.99 }
            GameSession.Sfx.COFRE -> { v.kind = 3; v.dur = 0.85; v.amp = 0.42; v.freq = 196.0 }
            GameSession.Sfx.TRAMPA -> { v.kind = 4; v.dur = 0.55; v.amp = 0.50; v.freq = 90.0 }
            GameSession.Sfx.DANO -> { v.kind = 5; v.dur = 0.40; v.amp = 0.55; v.freq = 70.0 }
            GameSession.Sfx.USAR -> { v.kind = 6; v.dur = 0.30; v.amp = 0.32; v.freq = 440.0; v.freq2 = 880.0 }
            GameSession.Sfx.ROMPER -> { v.kind = 7; v.dur = 0.70; v.amp = 0.55; v.freq = 110.0 }
            GameSession.Sfx.GANAR -> { v.kind = 8; v.dur = 1.90; v.amp = 0.45; v.freq = 392.0 }
            GameSession.Sfx.PERDER -> { v.kind = 9; v.dur = 2.10; v.amp = 0.45; v.freq = 220.0 }
            GameSession.Sfx.ZUMBIDO -> { v.kind = 11; v.dur = 0.50; v.amp = 0.18; v.freq = 58.0 }
            GameSession.Sfx.BICHO -> { v.kind = 12; v.dur = 0.62; v.amp = 0.42; v.freq = 168.0 }
            GameSession.Sfx.GOLPE -> { v.kind = 13; v.dur = 0.22; v.amp = 0.30; v.freq = 620.0 }
            GameSession.Sfx.IMPACTO -> { v.kind = 14; v.dur = 0.34; v.amp = 0.52; v.freq = 96.0 }
        }
    }

    private fun noise(): Float = rnd.nextFloat() * 2f - 1f

    private fun voiceSample(v: Voice, dt: Double): Float {
        val x = v.age / v.dur
        if (x >= 1.0) { v.active = false; return 0f }
        val env: Double
        var sample: Double
        when (v.kind) {
            0 -> { // paso: golpe seco de tierra
                env = exp(-x * 14.0) * (1.0 - x)
                v.noise = v.noise * 0.72f + noise() * 0.28f
                sample = v.noise.toDouble() * 0.8 + sin(v.phase) * 0.5
                v.phase += 2 * PI * v.freq * dt
            }
            1 -> { // eco: campanita de dos tonos
                env = exp(-x * 6.5)
                sample = sin(v.phase) * 0.6 + sin(v.phase2) * 0.4
                v.phase += 2 * PI * v.freq * dt
                v.phase2 += 2 * PI * v.freq2 * dt
            }
            2 -> { // vetagris: acorde cristalino largo
                env = exp(-x * 2.4) * (1.0 - exp(-x * 40.0))
                sample = sin(v.phase) * 0.45 + sin(v.phase2) * 0.35 + sin(v.phase * 2.0) * 0.2
                v.phase += 2 * PI * v.freq * dt
                v.phase2 += 2 * PI * v.freq2 * dt
            }
            3 -> { // cofre: madera + monedas
                env = exp(-x * 3.6)
                val coins = if (x > 0.12) sin(v.phase2) * 0.35 * exp(-(x - 0.12) * 8.0) else 0.0
                v.noise = v.noise * 0.6f + noise() * 0.4f
                sample = sin(v.phase) * 0.5 + coins + v.noise * 0.25
                v.phase += 2 * PI * v.freq * dt
                v.phase2 += 2 * PI * (1400.0 + sin(v.age * 40.0) * 300.0) * dt
            }
            4 -> { // trampa: golpe grave con barrido
                env = exp(-x * 5.0)
                val f = v.freq * (1.0 + 3.0 * exp(-x * 9.0))
                v.noise = v.noise * 0.55f + noise() * 0.45f
                sample = sin(v.phase) * 0.7 + v.noise * 0.5
                v.phase += 2 * PI * f * dt
            }
            5 -> { // dano: thump sordo
                env = exp(-x * 8.0)
                v.noise = v.noise * 0.80f + noise() * 0.20f
                sample = sin(v.phase) * 0.85 + v.noise * 0.35
                v.phase += 2 * PI * (v.freq * (1.0 + 1.6 * exp(-x * 12.0))) * dt
            }
            6 -> { // usar objeto: subida corta
                env = exp(-x * 7.0)
                val f = v.freq + (v.freq2 - v.freq) * x
                sample = sin(v.phase) * 0.7
                v.phase += 2 * PI * f * dt
            }
            7 -> { // romper roca
                env = exp(-x * 4.2)
                v.noise = v.noise * 0.45f + noise() * 0.55f
                sample = v.noise.toDouble() * 0.9 + sin(v.phase) * 0.5
                v.phase += 2 * PI * (v.freq * (1.0 + 2.2 * exp(-x * 14.0))) * dt
            }
            8 -> { // ganar: arpegio ascendente
                env = if (x < 0.05) x / 0.05 else exp(-(x - 0.05) * 1.6)
                val step = (x * 5.0).toInt().coerceAtMost(4)
                val f = v.freq * 2.0.pow(PENTA[step] / 12.0)
                sample = sin(v.phase) * 0.55 + sin(v.phase * 2.0) * 0.18
                v.phase += 2 * PI * f * dt
            }
            9 -> { // perder: descenso grave
                env = exp(-x * 1.5)
                val f = v.freq * (1.0 - 0.55 * x)
                sample = sin(v.phase) * 0.6 + sin(v.phase * 0.5) * 0.3
                v.phase += 2 * PI * f * dt
            }
            10 -> { // marca de tiza
                env = exp(-x * 12.0)
                sample = sin(v.phase) * 0.5
                v.phase += 2 * PI * (v.freq * (1.0 - 0.3 * x)) * dt
            }
            12 -> { // bicho: chillido raspado que baja de golpe
                env = exp(-x * 5.5) * (1.0 - x * 0.35)
                v.noise = v.noise * 0.55f + noise() * 0.45f
                val f = v.freq * (1.0 + 0.9 * exp(-x * 9.0)) * (1.0 - 0.45 * x)
                sample = sin(v.phase) * 0.42 + sin(v.phase * 2.51) * 0.22 + v.noise * 0.30
                v.phase += 2 * PI * f * dt
            }
            13 -> { // swing: el aire cortado, corto y agudo
                env = exp(-x * 20.0) * sin(PI * x)
                v.noise = v.noise * 0.35f + noise() * 0.65f
                sample = v.noise * 0.85 + sin(v.phase) * 0.15
                v.phase += 2 * PI * (v.freq * (1.0 - 0.55 * x)) * dt
            }
            14 -> { // impacto: golpe carnoso con cuerpo grave
                env = exp(-x * 11.0)
                v.noise = v.noise * 0.60f + noise() * 0.40f
                val f = v.freq * (1.0 + 1.6 * exp(-x * 26.0))
                sample = sin(v.phase) * 0.55 + v.noise * 0.45
                v.phase += 2 * PI * f * dt
            }
            else -> { // zumbido de peligro
                env = sin(PI * x) * 0.9
                sample = sin(v.phase) * 0.6 + sin(v.phase * 1.5) * 0.2
                v.phase += 2 * PI * v.freq * dt
            }
        }
        v.age += dt
        return (sample * env * v.amp).toFloat()
    }

    private fun render() {
        val dt = 1.0 / RATE
        val s = save.settings
        val master = s.masterVolume.coerceIn(0f, 1f)
        val musicVol = s.musicVolume.coerceIn(0f, 1f) * master
        val sfxVol = s.sfxVolume.coerceIn(0f, 1f) * master

        while (true) {
            val q = sfxQueue.poll() ?: break
            spawnSfx(q)
        }

        val theme = CaveTheme.entries[themeIndex.coerceIn(0, CaveTheme.entries.size - 1)]
        // Tonica por tema: baja y oscura, mas grave en las zonas profundas.
        val root = 55.0 * 2.0.pow((theme.ordinal % 5) / 12.0) * (if (track == Track.MENU) 1.5 else 1.0)
        val tense = intensity

        for (i in 0 until BLOCK) {
            musicGain += (targetMusicGain - musicGain) * 0.00004f

            // --- drone: dos osciladores desafinados
            val droneF = root
            dronePhase += 2 * PI * droneF * dt
            dronePhase2 += 2 * PI * (droneF * 1.0072) * dt
            var music = (sin(dronePhase) * 0.30 + sin(dronePhase2) * 0.24).toFloat()
            music += (sin(dronePhase * 2.0) * 0.09).toFloat()

            // --- pad: cuarta y quinta que respiran
            val breath = (0.55 + 0.45 * sin(t * 0.11)).toFloat()
            val padNotes = doubleArrayOf(root * 2, root * 2 * 2.0.pow(3 / 12.0), root * 3, root * 4)
            var pad = 0.0
            for (k in padNotes.indices) {
                padPhase[k] += 2 * PI * padNotes[k] * dt
                pad += sin(padPhase[k]) * (0.075 - k * 0.012)
            }
            music += (pad * breath).toFloat()

            // --- melodia lenta y espaciada
            if (t >= nextMelodyAt) {
                val degree = PENTA[rnd.nextInt(PENTA.size)]
                val octave = if (rnd.nextFloat() < 0.35f) 3 else 2
                melodyFreq = root * octave * 2.0.pow(degree / 12.0)
                melodyEnv = 1.0
                melodyPhase = 0.0
                nextMelodyAt = t + 2.6 + rnd.nextDouble() * 4.2 - tense * 1.1
            }
            if (melodyEnv > 0.0001) {
                melodyPhase += 2 * PI * melodyFreq * dt
                val tone = sin(melodyPhase) * 0.55 + sin(melodyPhase * 2.0) * 0.12
                music += (tone * melodyEnv * 0.20).toFloat()
                melodyEnv *= 0.99993
            }

            // --- gota de agua de la cueva
            if (t >= nextDropAt) {
                dropEnv = 1.0
                dropPhase = 0.0
                dropFreq = 700.0 + rnd.nextDouble() * 900.0
                nextDropAt = t + 3.0 + rnd.nextDouble() * 7.0
            }
            if (dropEnv > 0.0001) {
                dropPhase += 2 * PI * (dropFreq * (1.0 + 1.4 * dropEnv * dropEnv)) * dt
                music += (sin(dropPhase) * dropEnv * dropEnv * 0.16).toFloat()
                dropEnv *= 0.99975
            }

            // --- viento subterraneo (ruido rosa aproximado)
            lpNoise = lpNoise * 0.997f + noise() * 0.003f
            music += lpNoise * (0.55f + tense * 0.9f)

            // --- pulso de tension: late mas rapido cuando la vida esta baja
            if (tense > 0.02f) {
                val beat = (sin(t * (2.2 + tense * 3.4)) * 0.5 + 0.5).pow(6.0)
                music += (beat * 0.10 * tense).toFloat()
            }

            // Filtro pasabajos suave para que no suene aspero
            lp += (music - lp) * 0.28f
            var mus = lp * 0.55f * musicGain * musicVol

            // --- efectos
            var sl = 0f
            var sr = 0f
            for (v in voices) {
                if (!v.active) continue
                val sv = voiceSample(v, dt) * sfxVol
                sl += sv * (1f - v.pan)
                sr += sv * v.pan
            }

            // --- eco de caverna sobre la mezcla completa
            val inL = mus + sl
            val inR = mus + sr
            val echoL = delayL[dIdxL]
            val echoR = delayR[dIdxR]
            val feedback = 0.34f + 0.10f * (if (track == Track.CUEVA) 1f else 0f)
            delayL[dIdxL] = inL + echoR * feedback
            delayR[dIdxR] = inR + echoL * feedback
            dIdxL = (dIdxL + 1) % delayL.size
            dIdxR = (dIdxR + 1) % delayR.size

            var outL = inL + echoL * 0.30f
            var outR = inR + echoR * 0.30f

            // Limitador blando: nunca satura feo
            outL = softClip(outL)
            outR = softClip(outR)

            out[i * 2] = (outL * 30000f).toInt().coerceIn(-32768, 32767).toShort()
            out[i * 2 + 1] = (outR * 30000f).toInt().coerceIn(-32768, 32767).toShort()

            t += dt
        }
    }

    private fun softClip(x: Float): Float {
        val a = abs(x)
        if (a <= 0.7f) return x
        val sign = if (x < 0) -1f else 1f
        return sign * min(1f, 0.7f + (a - 0.7f) / (1f + (a - 0.7f) * 2.2f))
    }
}
