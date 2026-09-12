package com.mggx.laberinto

import com.mggx.laberinto.gl.ArmsMesh
import com.mggx.laberinto.gl.EnemyMeshes
import com.mggx.laberinto.gl.PlayerMeshes
import com.mggx.laberinto.gl.PropMeshes
import com.mggx.laberinto.gl.StructureMeshes
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le saca una foto a cada modelo del juego y la deja en `build/modelos/`.
 *
 * No falla casi nunca: no es un test de comportamiento, es una herramienta de
 * trabajo. Hasta ahora los modelos se escribian a mano y se verificaban solo
 * con numeros —que las caras miren para afuera, que las proporciones entren en
 * un rango—, y eso no alcanza para decir si algo se VE bien. Un pico puede
 * tener la geometria impecable y estar puesto como una cola, y eso paso.
 *
 * Correr `./gradlew :app:testDebugUnitTest --tests '*RetratosTest*'` y abrir los
 * PNG es la forma de mirar un modelo sin compilar el APK ni tener el telefono.
 */
class RetratosTest {

    /**
     * Convierte la malla de brazos (que tiene su propio formato de vertice, con
     * la marca de mano en vez de UV) a una [PropMeshes.Geometry] para poder
     * fotografiarla con el mismo visor que todo lo demas.
     *
     * [soloTag] filtra por la marca: sirve para retratar el arma o el objeto de
     * mano por separado, sin los brazos encima tapandolos.
     */
    private fun deBrazos(
        mesh: ArmsMesh.Mesh,
        soloTag: ((Float) -> Boolean)? = null
    ): PropMeshes.Geometry {
        val st = ArmsMesh.STRIDE_FLOATS
        val mapa = HashMap<Int, Int>()
        val vs = ArrayList<Float>()
        val idx = ArrayList<Int>()

        fun tomar(v: Int): Int = mapa.getOrPut(v) {
            val base = v * st
            vs.add(mesh.vertices[base]); vs.add(mesh.vertices[base + 1]); vs.add(mesh.vertices[base + 2])
            vs.add(mesh.vertices[base + 3]); vs.add(mesh.vertices[base + 4]); vs.add(mesh.vertices[base + 5])
            vs.size / 6 - 1
        }

        var i = 0
        while (i + 2 < mesh.indices.size) {
            val a = mesh.indices[i]; val b = mesh.indices[i + 1]; val c = mesh.indices[i + 2]
            i += 3
            if (soloTag != null && !soloTag(mesh.vertices[a * st + 6])) continue
            idx.add(tomar(a)); idx.add(tomar(b)); idx.add(tomar(c))
        }
        return PropMeshes.Geometry(vs.toFloatArray(), idx.toIntArray())
    }

    @Test
    fun retratarLosObjetosDeMano() {
        val fotos = ArmsMesh.Objeto.entries.map { o ->
            o.name to deBrazos(ArmsMesh.build(objeto = o)) { it < -1.5f }
        }
        val f = VisorDeMallas.hoja(fotos, "objetos-de-mano")
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists() && f.length() > 1000)
    }

    @Test
    fun retratarLasArmas() {
        val fotos = ArmsMesh.Arma.entries.map { a ->
            a.name to deBrazos(ArmsMesh.build(arma = a)) { it > 1.5f }
        }
        val f = VisorDeMallas.hoja(fotos, "armas")
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists())
    }

    @Test
    fun retratarLosBrazos() {
        val f = VisorDeMallas.retrato(
            deBrazos(ArmsMesh.build(ArmsMesh.Arma.PICO, ArmsMesh.Objeto.FRASCO)),
            "brazos-con-pico-y-frasco",
            lado = 340
        )
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists())
    }

    @Test
    fun retratarElPersonaje() {
        val f = VisorDeMallas.hoja(
            listOf(
                "cuerpo" to PlayerMeshes.mineroCuerpo(),
                "cabeza" to PlayerMeshes.mineroCabeza(),
                "casco" to PlayerMeshes.mineroCasco()
            ),
            "personaje",
            lado = 300
        )
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists())
    }

    @Test
    fun retratarLosBichos() {
        val f = VisorDeMallas.hoja(
            listOf(
                "murcielago" to EnemyMeshes.murcielagoCuerpo(),
                "ala" to EnemyMeshes.murcielagoAla(),
                "rastrero" to EnemyMeshes.rastreroSegmento(),
                "guardian-torso" to EnemyMeshes.guardianTorso(),
                "guardian-cabeza" to EnemyMeshes.guardianCabeza(),
                "topo" to EnemyMeshes.topoCuerpo(),
                "arana" to EnemyMeshes.aranaCuerpo()
            ),
            "bichos",
            lado = 220
        )
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists())
    }

    @Test
    fun retratarLasEstructuras() {
        val f = VisorDeMallas.hoja(
            listOf(
                "antorcha" to StructureMeshes.antorcha(),
                "cristal" to StructureMeshes.cristal(),
                "cofre" to StructureMeshes.cofre(),
                "hongo" to StructureMeshes.hongo(),
                "pincho" to StructureMeshes.pincho(),
                "estacion" to StructureMeshes.estacionCarburo(),
                "obelisco" to StructureMeshes.obeliscoSalida()
            ),
            "estructuras",
            lado = 220
        )
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists())
    }
}
