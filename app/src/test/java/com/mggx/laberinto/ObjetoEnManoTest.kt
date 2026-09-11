package com.mggx.laberinto

import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import com.mggx.laberinto.gl.ArmsMesh
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El objeto que llevas en la mano izquierda: el pan, el mapa, el frasco.
 *
 * Viaja en la MISMA malla que los brazos, marcado con [ArmsMesh.TAG_OBJETO],
 * que es negativo. El signo hace dos cosas de una: el vertex shader elige la
 * matriz del brazo con `aSide < 0.0`, asi que ya va con la mano izquierda; y el
 * fragment shader lo reconoce por ser menor que -1.5 para darle su material.
 */
class ObjetoEnManoTest {

    /** Vertices marcados como objeto de mano. */
    private fun verticesDelObjeto(objeto: ArmsMesh.Objeto): List<FloatArray> {
        val mesh = ArmsMesh.build(objeto = objeto)
        val out = ArrayList<FloatArray>()
        var i = 0
        while (i < mesh.vertices.size) {
            if (mesh.vertices[i + 6] < -1.5f) {
                out.add(floatArrayOf(mesh.vertices[i], mesh.vertices[i + 1], mesh.vertices[i + 2]))
            }
            i += ArmsMesh.STRIDE_FLOATS
        }
        return out
    }

    @Test
    fun todosLosConsumiblesDeLaTiendaTienenAlgoQueMostrar() {
        // Un consumible sin modelo se veria como una mano vacia, y eso se lee
        // como que el juego perdio el objeto. Ninguno puede quedar afuera.
        val consumibles = ItemCatalog.ofKind(ItemKind.CONSUMIBLE)
        assertTrue("no hay consumibles en el catalogo", consumibles.size > 15)
        for (item in consumibles) {
            assertNotNull(
                "${item.id} (${item.name}) no tiene forma para la mano",
                ArmsMesh.Objeto.por(item.id)
            )
        }
    }

    @Test
    fun sinObjetoElegidoNoSeDibujaNada() {
        assertNull("la cadena vacia no es un objeto", ArmsMesh.Objeto.por(""))
        val sin = ArmsMesh.build()
        var marcados = 0
        var i = 0
        while (i < sin.vertices.size) {
            if (sin.vertices[i + 6] < -1.5f) marcados++
            i += ArmsMesh.STRIDE_FLOATS
        }
        assertEquals("aparecio un objeto sin que haya ninguno elegido", 0, marcados)
    }

    @Test
    fun cadaFamiliaAgregaGeometriaDeVerdad() {
        val sinNada = ArmsMesh.build().vertices.size
        for (objeto in ArmsMesh.Objeto.entries) {
            val con = ArmsMesh.build(objeto = objeto)
            assertTrue("$objeto no agrego nada", con.vertices.size > sinNada)
            assertTrue("$objeto tiene muy pocos vertices", verticesDelObjeto(objeto).size > 40)
        }
    }

    @Test
    fun elObjetoVaEnLaManoIzquierdaYNoEnLaDerecha() {
        // El tag NEGATIVO es lo que lo manda a la matriz del brazo izquierdo.
        // Si fuera positivo, el pan te apareceria en la mano del pico.
        assertTrue(
            "el tag del objeto tiene que ser negativo para ir a la izquierda",
            ArmsMesh.TAG_OBJETO < 0f
        )
        assertTrue(
            "y menor que -1.5, que es como lo reconoce el shader",
            ArmsMesh.TAG_OBJETO < -1.5f
        )
        // Y no puede confundirse con el arma, que va en la derecha.
        assertTrue("el arma y el objeto comparten marca", ArmsMesh.TAG_ARMA > 0f)
    }

    @Test
    fun elObjetoQuedaAgarradoYNoFlotandoAlLado() {
        // Tiene que cruzar el hueco del puno izquierdo, igual que el arma cruza
        // el derecho: si no, se veria una mano abierta con algo al lado.
        for (objeto in ArmsMesh.Objeto.entries) {
            val v = verticesDelObjeto(objeto)
            val x = v.map { kotlin.math.abs(it[0]) }.average().toFloat()
            assertTrue("$objeto queda corrido del eje de la mano (x=$x)", x < 0.09f)
            val maxZ = v.maxOf { it[2] }
            assertTrue("$objeto asoma por detras de la mano (z=$maxZ)", maxZ < 0.06f)
        }
    }

    @Test
    fun elObjetoSeVeSinTaparLaPantalla() {
        // Se lleva para MIRARLO, asi que tiene que tener tamano suficiente para
        // reconocerse de reojo; pero no puede taparte la mira ni el camino, y
        // por eso se levanta menos que el arma.
        //
        // No se pide que TODOS se levanten lo mismo: un frasco o una antorcha
        // asoman por encima del puno, pero un ovillo o una piedra van apoyados
        // adentro de la mano, que es como se llevan de verdad. Lo que se cuida
        // es que se vean y que no estorben.
        for (objeto in ArmsMesh.Objeto.entries) {
            val v = verticesDelObjeto(objeto)
            val ancho = v.maxOf { it[0] } - v.minOf { it[0] }
            val alto = v.maxOf { it[1] } - v.minOf { it[1] }
            val largo = v.maxOf { it[2] } - v.minOf { it[2] }
            val tamano = kotlin.math.sqrt(ancho * ancho + alto * alto + largo * largo)
            assertTrue("$objeto es tan chico que no se ve (mide $tamano)", tamano > 0.07f)
            assertTrue("$objeto es enorme y tapa la pantalla (mide $tamano)", tamano < 0.45f)
            assertTrue(
                "$objeto se levanta demasiado y tapa la vista (maxY=${v.maxOf { it[1] }})",
                v.maxOf { it[1] } < 0.40f
            )
        }
    }

    @Test
    fun losObjetosLargosAsomanPorArribaDelPuno() {
        // Un frasco, una antorcha y un mapa se agarran por el medio y sobresalen:
        // si quedaran enteros adentro del puno no se leerian como lo que son.
        for (objeto in listOf(
            ArmsMesh.Objeto.FRASCO, ArmsMesh.Objeto.ANTORCHA, ArmsMesh.Objeto.MAPA
        )) {
            val maxY = verticesDelObjeto(objeto).maxOf { it[1] }
            assertTrue("$objeto no asoma por arriba del puno (maxY=$maxY)", maxY > 0.07f)
        }
    }

    @Test
    fun ningunaFamiliaSeVeIgualQueOtra() {
        // Si dos familias tuvieran la misma silueta, elegirlas por separado no
        // serviria de nada: no se distinguirian de reojo.
        val siluetas = ArmsMesh.Objeto.entries.map { o ->
            val v = verticesDelObjeto(o)
            Triple(
                Math.round((v.maxOf { it[0] } - v.minOf { it[0] }) * 1000f),
                Math.round((v.maxOf { it[1] } - v.minOf { it[1] }) * 1000f),
                Math.round((v.maxOf { it[2] } - v.minOf { it[2] }) * 1000f)
            )
        }
        assertEquals("hay familias con la misma silueta", siluetas.size, siluetas.toSet().size)
    }

    @Test
    fun elArmaYElObjetoConvivenEnLaMismaMalla() {
        // Van los dos en el mismo buffer, con sus propias marcas: si uno
        // pisara al otro, llevar pico y pan a la vez rompería uno de los dos.
        val mesh = ArmsMesh.build(ArmsMesh.Arma.PICO, ArmsMesh.Objeto.PAN)
        var armas = 0
        var objetos = 0
        var brazos = 0
        var i = 0
        while (i < mesh.vertices.size) {
            val tag = mesh.vertices[i + 6]
            when {
                tag > 1.5f -> armas++
                tag < -1.5f -> objetos++
                else -> brazos++
            }
            i += ArmsMesh.STRIDE_FLOATS
        }
        assertTrue("no se dibujo el arma", armas > 50)
        assertTrue("no se dibujo el objeto", objetos > 40)
        assertTrue("se perdieron los brazos", brazos > 300)
    }

    @Test
    fun unObjetoDesconocidoNoDejaLaManoVacia() {
        // El dia que se agregue un consumible nuevo y nadie toque este mapeo,
        // tiene que salir algo generico y no nada.
        assertNotNull(ArmsMesh.Objeto.por("consumible_que_todavia_no_existe"))
    }

    @Test
    fun lasCarasDelObjetoMiranParaAfuera() {
        // Mismo cuidado que con el resto de los modelos: una cara dada vuelta
        // no da ningun error, simplemente se ve transparente en el telefono.
        for (objeto in ArmsMesh.Objeto.entries) {
            val mesh = ArmsMesh.build(objeto = objeto)
            var malas = 0
            var i = 0
            while (i + 2 < mesh.indices.size) {
                val i0 = mesh.indices[i] * ArmsMesh.STRIDE_FLOATS
                val i1 = mesh.indices[i + 1] * ArmsMesh.STRIDE_FLOATS
                val i2 = mesh.indices[i + 2] * ArmsMesh.STRIDE_FLOATS
                if (mesh.vertices[i0 + 6] >= -1.5f) { i += 3; continue }

                val ax = mesh.vertices[i1] - mesh.vertices[i0]
                val ay = mesh.vertices[i1 + 1] - mesh.vertices[i0 + 1]
                val az = mesh.vertices[i1 + 2] - mesh.vertices[i0 + 2]
                val bx = mesh.vertices[i2] - mesh.vertices[i0]
                val by = mesh.vertices[i2 + 1] - mesh.vertices[i0 + 1]
                val bz = mesh.vertices[i2 + 2] - mesh.vertices[i0 + 2]
                val gx = ay * bz - az * by
                val gy = az * bx - ax * bz
                val gz = ax * by - ay * bx
                val len = kotlin.math.sqrt(gx * gx + gy * gy + gz * gz)
                if (len < 1e-9f) { i += 3; continue }

                var nx = 0f; var ny = 0f; var nz = 0f
                for (k in intArrayOf(i0, i1, i2)) {
                    nx += mesh.vertices[k + 3]; ny += mesh.vertices[k + 4]; nz += mesh.vertices[k + 5]
                }
                val nlen = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)
                if (nlen < 1e-6f) { i += 3; continue }
                val dot = (gx / len) * (nx / nlen) + (gy / len) * (ny / nlen) + (gz / len) * (nz / nlen)
                if (dot < 0.05f) malas++
                i += 3
            }
            assertEquals("$objeto tiene triangulos dados vuelta", 0, malas)
        }
    }
}
