package com.mggx.laberinto

import com.mggx.laberinto.net.*
import org.junit.Assert.*
import org.junit.Test

class NetworkRegressionTest {
    @Test fun verticalMovementIsTransmittedWithoutHorizontalMovement() {
        val t=TransporteLocal(); val peer=t.companero()
        val link=MatchLink(t,"host","Host","skin_minero",true)
        link.bombear(0.1f,5f,0f,5f,90f,0); peer.recibir()
        link.bombear(0.1f,5f,1f,5f,90f,0)
        val pose=peer.recibir().mapNotNull { NetProtocol.decodificar(it) }.first { it.tipo==NetProtocol.Tipo.POSE }
        assertEquals(1f,pose.num(1),0.001f)
    }

    @Test fun movingHostStillAnnouncesMatchToLateJoiner() {
        val t=TransporteLocal()
        val host=MatchLink(t,"host","Host","skin_minero",true)
        host.arrancar(NetProtocol.Modo.COOPERATIVO,4,1234L)
        val guest=MatchLink(t.companero(),"guest","Guest","skin_minero",false)
        repeat(40) {
            host.bombear(0.1f,it.toFloat(),0f,5f,90f,0)
            guest.latir(0.1f)
        }
        assertTrue(guest.match.arrancada)
        assertEquals(4,guest.match.nivel)
        assertEquals(1234L,guest.match.semilla)
    }

    @Test fun idleHostResendsPoseForLateJoiner() {
        val t=TransporteLocal()
        val host=MatchLink(t,"host","Host","skin_minero",true)
        host.bombear(0.1f,5f,2f,7f,90f,0)
        val peer=t.companero()
        repeat(40) { host.bombear(0.1f,5f,2f,7f,90f,0) }
        assertTrue(peer.recibir().mapNotNull { NetProtocol.decodificar(it) }.any { it.tipo==NetProtocol.Tipo.POSE })
    }

    @Test fun nonFiniteEnemyCoordinatesAreRejected() {
        for (value in listOf("NaN", "Infinity", "-Infinity")) {
            assertNull(NetProtocol.leerCuadroDeBicho("0,$value,1,0"))
            assertNull(NetProtocol.leerCuadroDeBicho("0,1,$value,0"))
        }
    }

    @Test fun nonFiniteNumbersNeverEnterPlayerCoordinates() {
        for (value in listOf("NaN","Infinity","-Infinity")) {
            val message=NetProtocol.decodificar("1|POSE|peer|$value|0|0|0|0")!!
            assertEquals(0f,message.num(0),0f)
        }
    }
}
