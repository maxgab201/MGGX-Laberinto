package com.mggx.laberinto

import com.mggx.laberinto.gl.ArmsMesh
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El pulgar tiene que quedar del mismo lado que el dedo indice (el mas
 * cercano al centro del cuerpo, "hacia adentro"), no del lado del menique.
 *
 * Bug real que motiva este test: `buildArm()` se dibuja una sola vez
 * (mirando hacia -Z) y se espeja con `side` para armar el par de manos, asi
 * que un pulgar mal puesto en el modelo base queda mal puesto en las DOS
 * manos por igual — no es algo que el espejado por si solo pueda arreglar.
 */
class ArmsMeshTest {

    @Test
    fun elPulgarQuedaDelLadoDelIndice() {
        assertTrue(
            "el pulgar (${ArmsMesh.PULGAR_X}) tiene que tener el mismo signo " +
                "que el indice (${ArmsMesh.INDICE_X}): los dos del lado de adentro",
            ArmsMesh.PULGAR_X * ArmsMesh.INDICE_X > 0f
        )
    }

    @Test
    fun laMallaSigueSiendoValida() {
        // No repite el chequeo de orientacion de caras (eso ya lo cubre
        // MeshWindingTest); solo confirma que el cambio no rompio el build.
        val mesh = ArmsMesh.build()
        assertTrue(mesh.vertices.isNotEmpty())
        assertTrue(mesh.indices.isNotEmpty())
        assertTrue(mesh.indices.size % 3 == 0)
    }
}
