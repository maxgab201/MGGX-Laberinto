package com.mggx.laberinto

import com.mggx.laberinto.net.PuedeJugarEnRed
import com.mggx.laberinto.net.PuedeJugarEnRed.Motivo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El corte entre lo que anda sin internet (todo) y lo que no (el multijugador).
 */
class PuedeJugarEnRedTest {

    @Test
    fun conTodoEnOrdenSePuede() {
        assertEquals(
            Motivo.OK,
            PuedeJugarEnRed.evaluar(hayInternet = true, configEnElApk = true, firebaseArranco = true)
        )
    }

    @Test
    fun sinInternetNoSePuedeYEsLoPrimeroQueSeDice() {
        // El caso que faltaba y que era el peor de todos: Firebase sin red NO
        // falla, encola lo que le pediste y promete mandarlo despues. La sala
        // se quedaba esperando para siempre, sin cartel y sin error.
        assertEquals(
            Motivo.SIN_INTERNET,
            PuedeJugarEnRed.evaluar(hayInternet = false, configEnElApk = true, firebaseArranco = true)
        )
    }

    @Test
    fun sinRedNoSeCulpaAOtraCosa() {
        // Aunque ademas falte todo lo demas, a alguien sin datos hay que
        // decirle que prenda los datos, no mandarlo a recompilar el APK: es lo
        // unico de los tres que puede arreglar solo.
        assertEquals(
            Motivo.SIN_INTERNET,
            PuedeJugarEnRed.evaluar(hayInternet = false, configEnElApk = false, firebaseArranco = false)
        )
    }

    @Test
    fun elApkSinConfiguracionSeDistingueDeFirebaseQueNoArranco() {
        // Son dos problemas con arreglos distintos: uno se arregla
        // recompilando y el otro no, asi que un solo cartel para los dos
        // manda a buscar el problema al lugar equivocado.
        assertEquals(
            Motivo.SIN_CONFIGURACION,
            PuedeJugarEnRed.evaluar(hayInternet = true, configEnElApk = false, firebaseArranco = false)
        )
        assertEquals(
            Motivo.FIREBASE_NO_ARRANCO,
            PuedeJugarEnRed.evaluar(hayInternet = true, configEnElApk = true, firebaseArranco = false)
        )
    }

    @Test
    fun cadaMotivoTieneSuPropioCartel() {
        val carteles = Motivo.entries.filter { it != Motivo.OK }.map { PuedeJugarEnRed.mensaje(it) }
        assertEquals("hay carteles repetidos", carteles.size, carteles.toSet().size)
        for (c in carteles) assertTrue("hay un cartel vacio", c.length > 25)
        assertEquals("con todo en orden no se muestra nada", "", PuedeJugarEnRed.mensaje(Motivo.OK))
    }

    @Test
    fun elCartelDeSinInternetAclaraQueElRestoDelJuegoAnda() {
        // Que no haya red no puede leerse como "el juego no anda": el juego
        // entero es offline, lo unico que necesita internet es la sala.
        val texto = PuedeJugarEnRed.mensaje(Motivo.SIN_INTERNET).lowercase()
        assertTrue("no dice que el resto del juego anda igual", "sin conexion" in texto)
        assertTrue("no dice que hacer", "wifi" in texto || "datos" in texto)
    }
}
