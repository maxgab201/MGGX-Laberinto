package com.mggx.laberinto.gl

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * El golpe se veia raro: en el pico de la animacion el puno se acercaba a
 * la camara en vez de alejarse hacia el objetivo, y el pivote de rotacion
 * (en la muneca) hacia que todo el antebrazo describiera un arco enorme.
 *
 * `app/build.gradle.kts` tiene `unitTests.isReturnDefaultValues = true`, asi
 * que `android.opengl.Matrix` no calcula nada real en un test JVM. Por eso
 * aca se reimplementa, con trigonometria pura, EXACTAMENTE la misma
 * composicion que arma `CaveRenderer.armMatrix()` a partir de los numeros
 * de [BrazoAnim.pose], para poder verificar el arreglo con numeros.
 */
class BrazoAnimTest {

    private data class Pt(val x: Float, val y: Float, val z: Float)

    private fun rotY(p: Pt, deg: Float): Pt {
        val r = Math.toRadians(deg.toDouble())
        val c = cos(r).toFloat(); val s = sin(r).toFloat()
        return Pt(p.x * c + p.z * s, p.y, -p.x * s + p.z * c)
    }

    private fun rotX(p: Pt, deg: Float): Pt {
        val r = Math.toRadians(deg.toDouble())
        val c = cos(r).toFloat(); val s = sin(r).toFloat()
        return Pt(p.x, p.y * c - p.z * s, p.y * s + p.z * c)
    }

    private fun rotZ(p: Pt, deg: Float): Pt {
        val r = Math.toRadians(deg.toDouble())
        val c = cos(r).toFloat(); val s = sin(r).toFloat()
        return Pt(p.x * c - p.y * s, p.x * s + p.y * c, p.z)
    }

    private fun trasladar(p: Pt, dx: Float, dy: Float, dz: Float) = Pt(p.x + dx, p.y + dy, p.z + dz)
    private fun escalar(p: Pt, e: Float) = Pt(p.x * e, p.y * e, p.z * e)

    /**
     * Transforma un punto local con la composicion NUEVA (la que arma
     * armMatrix() ahora): reposo pivotando en el origen, swing extra
     * pivotando en el codo. Reproduce, en el mismo orden, la cadena de
     * translateM/rotateM/scaleM de CaveRenderer.
     */
    private fun transformarNuevo(local: Pt, pose: BrazoPose, mio: Float): Pt {
        var p = escalar(local, pose.escala)
        if (mio != 0f) {
            p = trasladar(p, 0f, 0f, -BrazoAnim.PIVOTE_CODO_Z)
            p = rotZ(p, pose.rotZSwing)
            p = rotX(p, pose.rotXSwing)
            p = rotY(p, pose.rotYSwing)
            p = trasladar(p, 0f, 0f, BrazoAnim.PIVOTE_CODO_Z)
        }
        p = rotZ(p, pose.rotZReposo)
        p = rotX(p, pose.rotXReposo)
        p = rotY(p, pose.rotYReposo)
        p = trasladar(p, pose.tx, pose.ty, pose.tz)
        return p
    }

    /**
     * La composicion VIEJA (antes del arreglo): reposo y swing sumados en un
     * solo angulo por eje, todo pivotando desde el origen (cerca de la
     * muneca). Sirve para confirmar, con el mismo punto, cuanto mejora el
     * arreglo del pivote.
     */
    private fun transformarViejo(local: Pt, pose: BrazoPose): Pt {
        var p = escalar(local, pose.escala)
        p = rotZ(p, pose.rotZReposo + pose.rotZSwing)
        p = rotX(p, pose.rotXReposo + pose.rotXSwing)
        p = rotY(p, pose.rotYReposo + pose.rotYSwing)
        p = trasladar(p, pose.tx, pose.ty, pose.tz)
        return p
    }

    private fun distancia(a: Pt, b: Pt): Float =
        hypot(hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat(), (a.z - b.z))

    // Puntos locales de referencia (ver ArmsMesh.buildArm): el antebrazo va
    // de z=0.62 (codo) a z=0.05, y los dedos llegan hasta z~=-0.20.
    private val CODO = Pt(0f, 0f, 0.62f)
    private val DEDOS = Pt(0f, 0f, -0.20f)

    @Test
    fun elPunoSeAlejaDeLaCamaraEnElPicoDelGolpe() {
        val reposo = BrazoAnim.pose(side = 1f, mio = 0f, sway = 0f, bob = 0f, breathe = 0f)
        val pico = BrazoAnim.pose(side = 1f, mio = 1f, sway = 0f, bob = 0f, breathe = 0f)
        assertTrue(
            "el termino Z tiene que alejarse de la camara (mas negativo) en el pico",
            pico.tz < reposo.tz
        )
    }

    @Test
    fun laPuntaDeLosDedosQuedaMasLejosDeLaCamaraEnElPico() {
        val reposo = BrazoAnim.pose(side = 1f, mio = 0f, sway = 0f, bob = 0f, breathe = 0f)
        val pico = BrazoAnim.pose(side = 1f, mio = 1f, sway = 0f, bob = 0f, breathe = 0f)
        val zReposo = transformarNuevo(DEDOS, reposo, 0f).z
        val zPico = transformarNuevo(DEDOS, pico, 1f).z
        assertTrue(
            "la punta de los dedos tiene que alejarse de la camara al pegar (zReposo=$zReposo, zPico=$zPico)",
            zPico < zReposo
        )
    }

    @Test
    fun elCodoCasiNoGiraConElPivoteNuevo() {
        // El brazo entero se corre un poco hacia adelante al pegar (eso es a
        // proposito: tx/ty/tz cambian con mio, y ESO le pasa por igual a
        // cualquier punto del brazo, codo incluido). Lo que el pivote tiene
        // que evitar es que, ADEMAS de ese corrimiento comun, el codo
        // describa un arco de rotacion grande. Por eso se compara el
        // desplazamiento real del codo contra la magnitud del delta de
        // traslacion (tx,ty,tz) solo -el piso de movimiento que le toca a
        // CUALQUIER punto por el solo hecho de que el brazo entero se
        // adelanta- en vez de contra un numero fijo inventado.
        val reposo = BrazoAnim.pose(side = 1f, mio = 0f, sway = 0f, bob = 0f, breathe = 0f)
        val pico = BrazoAnim.pose(side = 1f, mio = 1f, sway = 0f, bob = 0f, breathe = 0f)
        val soloTraslacion = distancia(
            Pt(reposo.tx, reposo.ty, reposo.tz),
            Pt(pico.tx, pico.ty, pico.tz)
        )
        val codoReposo = transformarNuevo(CODO, reposo, 0f)
        val codoPico = transformarNuevo(CODO, pico, 1f)
        val desplazamientoCodo = distancia(codoReposo, codoPico)
        assertTrue(
            "el codo gira de mas: se movio $desplazamientoCodo m, y solo por la " +
                "traslacion compartida ya se mueve $soloTraslacion m",
            desplazamientoCodo < soloTraslacion + 0.08f
        )
    }

    @Test
    fun sinElPivoteElCodoDescribiaUnArcoEnorme() {
        // Confirma, con el MISMO punto, que el bug del pivote era real: sin
        // separar el swing del reposo, el codo se movia mucho mas.
        val reposo = BrazoAnim.pose(side = 1f, mio = 0f, sway = 0f, bob = 0f, breathe = 0f)
        val pico = BrazoAnim.pose(side = 1f, mio = 1f, sway = 0f, bob = 0f, breathe = 0f)
        val codoReposo = transformarViejo(CODO, reposo)
        val codoPico = transformarViejo(CODO, pico)
        val desplazamiento = distancia(codoReposo, codoPico)
        assertTrue(
            "el bug del pivote no era tan grave como se penso: $desplazamiento m",
            desplazamiento > 0.5f
        )
    }

    @Test
    fun conMioCeroLaComposicionNuevaEsIdenticaALaDeReposo() {
        // El bloque del swing extra no puede correr ni cambiar nada si
        // mio=0: si no, el brazo en reposo se veria distinto al de antes.
        val reposo = BrazoAnim.pose(side = -1f, mio = 0f, sway = 0.01f, bob = 0.005f, breathe = 0.002f)
        assertTrue(reposo.rotYSwing == 0f && reposo.rotXSwing == 0f && reposo.rotZSwing == 0f)
        val a = transformarNuevo(DEDOS, reposo, 0f)
        val b = transformarViejo(DEDOS, reposo)
        assertTrue(
            "con mio=0 la composicion nueva no coincide con la de reposo de siempre",
            distancia(a, b) < 1e-4f
        )
    }
}
