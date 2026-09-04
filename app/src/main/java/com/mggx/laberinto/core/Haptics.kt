package com.mggx.laberinto.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Vibracion corta y prolija, respetando el ajuste del jugador. */
class Haptics(context: Context, private val save: SaveData) {

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (t: Throwable) { null }

    private val available = vibrator?.hasVibrator() == true

    fun pulse(ms: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        if (!available || !save.settings.haptics) return
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, amplitude))
        } catch (_: Throwable) { }
    }

    fun tick() = pulse(12, 70)
}
