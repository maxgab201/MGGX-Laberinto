package com.mggx.laberinto

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mggx.laberinto.core.CaveAudio
import com.mggx.laberinto.core.Haptics
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.gl.CaveRenderer
import com.mggx.laberinto.input.GamepadBridge
import com.mggx.laberinto.ui.MggxApp

class MainActivity : ComponentActivity() {

    private lateinit var save: SaveData
    private lateinit var audio: CaveAudio
    private lateinit var renderer: CaveRenderer
    private lateinit var input: CaveRenderer.InputState
    private lateinit var pad: GamepadBridge
    private lateinit var haptics: Haptics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // La bandera de pantalla siempre prendida NO se pone aca: la maneja
        // MggxApp segun donde este el jugador (ver AhorroDeEnergia). Puesta
        // aca se prendia al arrancar y no se apagaba nunca, ni en la tienda ni
        // en los ajustes.
        hideSystemBars()

        save = SaveData.get(this)
        audio = CaveAudio(save)
        haptics = Haptics(this, save)
        input = CaveRenderer.InputState()
        input.autoRun = save.settings.autoRun
        input.padSensitivity = save.settings.gamepadSensitivity
        renderer = CaveRenderer(save, input)
        pad = GamepadBridge(save, input)
        pad.refreshConnected()
        pad.dispatchSynthetic = { keyCode ->
            val now = android.os.SystemClock.uptimeMillis()
            val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
            val up = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)
            // Va directo a super para no volver a entrar en el puente.
            super.dispatchKeyEvent(down)
            super.dispatchKeyEvent(up)
        }

        setContent {
            MggxApp(
                save = save,
                audio = audio,
                renderer = renderer,
                input = input,
                pad = pad,
                haptics = haptics,
                onExit = { finish() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        pad.refreshConnected()
        audio.start()
        audio.setTrack(CaveAudio.Track.MENU)
    }

    override fun onPause() {
        super.onPause()
        input.paused = true
        input.releaseAll()
        audio.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        audio.stop()
        save.save()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars() else input.releaseAll()
    }

    // ----------------------------------------------------------- mando

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = when (event.action) {
            KeyEvent.ACTION_DOWN -> pad.onKeyDown(event.keyCode, event)
            KeyEvent.ACTION_UP -> pad.onKeyUp(event.keyCode, event)
            else -> false
        }
        if (handled) return true
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (pad.onMotion(event)) return true
        return super.dispatchGenericMotionEvent(event)
    }

    // ------------------------------------------------------- pantalla

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }
}
