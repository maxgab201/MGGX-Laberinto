package com.mggx.laberinto.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.gl.CaveRenderer
import kotlin.math.abs

/**
 * Traduce todo lo que llega de un mando fisico.
 *
 * En el juego: sticks para caminar y mirar, gatillos para correr y botones
 * para usar objetos. En los menus: convierte los botones del mando en las
 * teclas que Compose ya entiende (cruceta y "aceptar"), asi se navega todo
 * sin tocar la pantalla.
 */
class GamepadBridge(
    private val save: SaveData,
    private val input: CaveRenderer.InputState
) {
    /** true mientras se esta jugando (no en menus ni en pausa). */
    @Volatile var inGame: Boolean = false

    var onPause: () -> Unit = {}
    var onUseItem: () -> Unit = {}
    var onChalk: () -> Unit = {}
    var onFlashlight: () -> Unit = {}
    var onAttack: () -> Unit = {}
    var onCycleItem: (Int) -> Unit = {}
    var onBack: () -> Unit = {}
    /** Envia una tecla sintetica a la ventana (para navegar menus). */
    var dispatchSynthetic: (Int) -> Unit = {}

    @Volatile var connected: Boolean = false
        private set

    private var lastNavTime = 0L
    private var lastNavDir = 0

    companion object {
        fun isGamepad(device: InputDevice?): Boolean {
            if (device == null) return false
            val s = device.sources
            return (s and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (s and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        }

        fun anyGamepadConnected(): Boolean {
            for (id in InputDevice.getDeviceIds()) {
                if (isGamepad(InputDevice.getDevice(id))) return true
            }
            return false
        }
    }

    fun refreshConnected() { connected = anyGamepadConnected() }

    // ------------------------------------------------------------- botones

    /** Devuelve true si consumio la tecla. */
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isGamepad(event.device) && !isDpad(keyCode)) return false
        connected = true
        if (event.repeatCount > 0 && !isDpad(keyCode)) return true

        if (inGame) {
            when (keyCode) {
                // A = saltar, que es lo que espera cualquiera que agarre un mando.
                KeyEvent.KEYCODE_BUTTON_A -> { input.jumpPending = true; return true }
                KeyEvent.KEYCODE_BUTTON_X -> { onUseItem(); return true }
                KeyEvent.KEYCODE_BUTTON_L2 -> { onChalk(); return true }
                KeyEvent.KEYCODE_BUTTON_Y -> { onCycleItem(1); return true }
                // R1 golpea. Ciclar objetos ya lo hacen Y y L1, asi que el
                // gatillo de arriba queda libre para lo que mas se usa.
                KeyEvent.KEYCODE_BUTTON_R1 -> { onAttack(); return true }
                KeyEvent.KEYCODE_BUTTON_L1 -> { onCycleItem(-1); return true }
                KeyEvent.KEYCODE_BUTTON_THUMBR -> {
                    // R3 cicla de pie -> agachado -> arrastrandose -> de pie.
                    input.crouchLevel = (input.crouchLevel + 1) % 3
                    return true
                }
                // Cruceta arriba: prende y apaga la linterna de carburo.
                KeyEvent.KEYCODE_DPAD_UP -> { if (event.repeatCount == 0) onFlashlight(); return true }
                KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_MENU -> { onPause(); return true }
                KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK -> { onPause(); return true }
                KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_R2 -> {
                    input.runPad = true; return true
                }
            }
            return false
        }

        // --- en menus: se traduce a teclas que Compose ya maneja
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> { dispatchSynthetic(KeyEvent.KEYCODE_DPAD_CENTER); return true }
            KeyEvent.KEYCODE_BUTTON_B -> { onBack(); return true }
            KeyEvent.KEYCODE_BUTTON_START -> { dispatchSynthetic(KeyEvent.KEYCODE_DPAD_CENTER); return true }
        }
        return false
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!isGamepad(event.device)) return false
        if (keyCode == KeyEvent.KEYCODE_BUTTON_THUMBL || keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
            input.runPad = false
            return true
        }
        return false
    }

    private fun isDpad(keyCode: Int): Boolean = keyCode in intArrayOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER
    )

    // --------------------------------------------------------------- ejes

    fun onMotion(event: MotionEvent): Boolean {
        if (!isGamepad(event.device)) return false
        if (event.action != MotionEvent.ACTION_MOVE) return false
        connected = true

        val dead = save.settings.stickDeadzone
        val lx = axis(event, MotionEvent.AXIS_X, dead)
        val ly = axis(event, MotionEvent.AXIS_Y, dead)
        var rx = axis(event, MotionEvent.AXIS_Z, dead)
        var ry = axis(event, MotionEvent.AXIS_RZ, dead)
        // Algunos mandos usan RX/RY para el stick derecho
        if (rx == 0f && ry == 0f) {
            rx = axis(event, MotionEvent.AXIS_RX, dead)
            ry = axis(event, MotionEvent.AXIS_RY, dead)
        }
        val rt = maxOf(
            rawAxis(event, MotionEvent.AXIS_RTRIGGER),
            rawAxis(event, MotionEvent.AXIS_GAS),
            rawAxis(event, MotionEvent.AXIS_BRAKE)
        )

        if (inGame) {
            input.moveX = lx
            input.moveY = -ly
            input.padLookX = rx
            input.padLookY = if (save.settings.invertY) ry else -ry
            input.runPad = rt > 0.55f
            return true
        }

        // En menus el stick izquierdo mueve el foco como si fuera la cruceta.
        val now = System.currentTimeMillis()
        val dir = when {
            ly < -0.65f -> KeyEvent.KEYCODE_DPAD_UP
            ly > 0.65f -> KeyEvent.KEYCODE_DPAD_DOWN
            lx < -0.65f -> KeyEvent.KEYCODE_DPAD_LEFT
            lx > 0.65f -> KeyEvent.KEYCODE_DPAD_RIGHT
            else -> 0
        }
        if (dir == 0) { lastNavDir = 0; return true }
        val delay = if (dir == lastNavDir) 190L else 0L
        if (now - lastNavTime >= delay) {
            lastNavTime = now
            lastNavDir = dir
            dispatchSynthetic(dir)
        }
        return true
    }

    private fun rawAxis(event: MotionEvent, axis: Int): Float {
        val v = event.getAxisValue(axis)
        return if (v.isNaN()) 0f else v
    }

    /** Valor del eje con zona muerta aplicada y reescalado a 0..1. */
    private fun axis(event: MotionEvent, axis: Int, dead: Float): Float {
        val raw = rawAxis(event, axis)
        val a = abs(raw)
        if (a < dead) return 0f
        val scaled = (a - dead) / (1f - dead)
        return if (raw < 0) -scaled else scaled
    }

    /** Suelta todo (al perder el foco o volver a un menu). */
    fun release() {
        input.moveX = 0f; input.moveY = 0f
        input.padLookX = 0f; input.padLookY = 0f
        input.runPad = false
        input.jumpPending = false
    }
}
