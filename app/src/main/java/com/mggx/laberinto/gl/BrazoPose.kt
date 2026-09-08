package com.mggx.laberinto.gl

/**
 * Los numeros de la pose del brazo, separados de las llamadas a
 * `android.opengl.Matrix`.
 *
 * Esto existe a proposito: `unitTests.isReturnDefaultValues = true`
 * (ver app/build.gradle.kts) hace que cualquier llamada a Matrix.* en un
 * test JVM no calcule nada real. Con la matematica separada aca, se puede
 * testear con aritmetica pura sin tocar OpenGL.
 */
data class BrazoPose(
    val tx: Float, val ty: Float, val tz: Float,
    val rotYReposo: Float, val rotXReposo: Float, val rotZReposo: Float,
    val rotYSwing: Float, val rotXSwing: Float, val rotZSwing: Float,
    val escala: Float
)

object BrazoAnim {
    /**
     * Punto (en Z local) del extremo lejano del antebrazo, cerca del codo.
     * Coincide con `taperedTube(..., 0.62f, ...)` en ArmsMesh.buildArm(). El
     * giro EXTRA del golpe pivotea ahi, no en el origen (que esta cerca de
     * la muneca): asi es el puno el que sale disparado y el codo casi no se
     * mueve, como un golpe de verdad.
     */
    const val PIVOTE_CODO_Z = 0.62f

    /**
     * [mio] va de 0 (reposo) a 1 (pico del golpe). [side] es -1 (brazo
     * izquierdo) o +1 (derecho).
     */
    fun pose(side: Float, mio: Float, sway: Float, bob: Float, breathe: Float): BrazoPose = BrazoPose(
        tx = side * (0.258f + sway * side * 0.5f) - side * mio * 0.16f,
        ty = -0.196f + bob + breathe + mio * 0.10f,
        // El puno se ALEJA de la camara hacia el objetivo en el pico del
        // golpe (mas negativo en Z, espacio-camara mira hacia -Z). Antes
        // decia "+ mio*0.20f", que lo acercaba a la camara en vez de
        // alejarlo: se sentia como que el golpe se retraia, no que pegaba.
        tz = -0.48f - kotlin.math.abs(sway) * 0.4f - mio * 0.20f,
        rotYReposo = side * -27f,
        rotXReposo = 3f + bob * 80f,
        rotZReposo = side * 14f,
        rotYSwing = side * mio * 22f,
        rotXSwing = -mio * 46f,
        rotZSwing = -side * mio * 30f,
        escala = 0.95f + mio * 0.10f
    )
}
