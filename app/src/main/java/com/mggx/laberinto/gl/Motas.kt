package com.mggx.laberinto.gl

import kotlin.math.sin

/**
 * El polvo que flota en el aire de la cueva.
 *
 * Es lo unico que le falta a una cueva bien iluminada para dejar de parecer un
 * decorado: el aire vacio no existe bajo tierra. Unas motas que cruzan el haz
 * de la antorcha hacen que el espacio se sienta ocupado, y encima son lo que
 * deja VER el haz, que hasta ahora solo se notaba cuando pegaba en una pared.
 *
 * La cuenta esta separada del dibujo, como BrazoPose o MinimapMath: es
 * aritmetica pura y se verifica en un test de JVM.
 *
 * El truco que hace que esto sea barato: las motas NO se guardan ni se
 * simulan. Hay una cantidad fija, cada una tiene una posicion que es funcion
 * del numero de mota y del tiempo, y esa posicion se envuelve en una caja
 * centrada en el jugador. Camines lo que camines, siempre tenes las mismas
 * doscientas motas alrededor, sin crear ni destruir ninguna y sin que haya que
 * llevar ningun estado de un cuadro al otro.
 */
object Motas {

    /** Lado de la caja de polvo que viaja con el jugador, en metros. */
    const val LADO = 14f

    /** Cuantas motas hay segun la calidad elegida. */
    fun cuantas(calidad: Int): Int = when (calidad) {
        0 -> 0          // en calidad baja el problema es el telefono, no el ambiente
        1 -> 70
        2 -> 160
        else -> 260
    }

    /** Un numero estable entre 0 y 1 para la mota [i], con [sal] para variarlo. */
    private fun azar(i: Int, sal: Int): Float {
        var h = i * 374761393 + sal * 668265263
        h = (h xor (h shr 13)) * 1274126177
        h = h xor (h shr 16)
        return (h and 0x7FFFFFFF) / 2147483647.0f
    }

    /**
     * Envuelve [v] dentro de una caja de lado [LADO] centrada en [centro].
     *
     * Es el corazon del asunto: una mota que se aleja por un lado reaparece
     * por el otro, asi que la nube de polvo sigue al jugador sin que nadie la
     * mueva. Sin esto habria que crear motas nuevas adelante y borrar las de
     * atras, que es el triple de trabajo y ademas se nota (aparecen de la nada).
     */
    fun envolver(v: Float, centro: Float): Float {
        val d = v - (centro - LADO * 0.5f)
        // `mod` de Kotlin da siempre un resultado positivo, que es justo lo que
        // hace falta: con el resto de `%` una mota detras del origen saltaria
        // al otro extremo de la caja.
        return (centro - LADO * 0.5f) + d.mod(LADO)
    }

    /**
     * Posicion de la mota [i] en el instante [t], ya envuelta alrededor del
     * jugador. Escribe x, y, z en [out].
     *
     * El movimiento es una deriva lenta (el aire de una cueva se mueve, pero
     * despacio) mas un bamboleo con senos de periodos distintos por eje: si
     * fueran iguales, todas las motas dibujarian la misma figura a la vez y se
     * veria el patron.
     */
    fun posicion(i: Int, t: Float, px: Float, py: Float, pz: Float, out: FloatArray) {
        val bx = azar(i, 1) * LADO
        val by = azar(i, 2) * LADO
        val bz = azar(i, 3) * LADO
        val fase = azar(i, 4) * 6.2831f
        val ritmo = 0.25f + azar(i, 5) * 0.55f

        // Deriva: todas caen muy despacio y van para el mismo lado, como el
        // aire de una galeria. La velocidad es distinta por mota para que no se
        // muevan en bloque.
        val deriva = 0.05f + azar(i, 6) * 0.07f

        val x = bx + t * deriva * 0.6f + sin((t * ritmo + fase).toDouble()).toFloat() * 0.35f
        val y = by - t * deriva * 0.28f + sin((t * ritmo * 0.7f + fase * 1.7f).toDouble()).toFloat() * 0.22f
        val z = bz + t * deriva * 0.4f + sin((t * ritmo * 1.3f + fase * 0.6f).toDouble()).toFloat() * 0.35f

        out[0] = envolver(x, px)
        out[1] = envolver(y, py)
        out[2] = envolver(z, pz)
    }

    /** Tamano de la mota [i], en metros. Van de casi invisibles a apenas visibles. */
    fun tamano(i: Int): Float = 0.008f + azar(i, 7) * 0.016f

    /**
     * Cuanto brilla la mota [i] ahora mismo, de 0 a 1.
     *
     * Titilan: una mota de polvo real gira sobre si misma y cambia de cara. Sin
     * el titileo se ven como puntos pegados en el aire.
     */
    fun brillo(i: Int, t: Float): Float {
        val fase = azar(i, 8) * 6.2831f
        val ritmo = 1.1f + azar(i, 9) * 2.4f
        return 0.45f + 0.55f * (0.5f + 0.5f * sin((t * ritmo + fase).toDouble()).toFloat())
    }
}
