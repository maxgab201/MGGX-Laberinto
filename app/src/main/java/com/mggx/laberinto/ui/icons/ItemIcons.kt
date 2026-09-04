package com.mggx.laberinto.ui.icons

import androidx.compose.ui.graphics.Color

/** Dibuja los iconos de objetos. Devuelve false si el id no pertenece a este grupo. */
fun drawItemIcon(p: Pen, id: IconId): Boolean {
    val m = p.main
    val a = p.accent
    val dim = m.copy(alpha = 0.45f)
    when (id) {

        // ------------------------------------------------ consumibles
        IconId.ANTORCHA -> {
            p.fill(m) { m(10.6f, 11f); l(13.4f, 11f); l(12.9f, 22f); l(11.1f, 22f); z() }
            p.fill(dim) { rect(9.6f, 10f, 4.8f, 1.8f) }
            p.fill(a) {
                m(12f, 1.5f); c(15.5f, 4.5f, 16f, 6.5f, 15.2f, 8.4f)
                c(14.6f, 9.9f, 13.4f, 10.6f, 12f, 10.6f)
                c(10.6f, 10.6f, 9.4f, 9.9f, 8.8f, 8.4f)
                c(8f, 6.5f, 8.5f, 4.5f, 12f, 1.5f); z()
            }
            p.fill(Color(0xFFFFF2C4)) {
                m(12f, 4.6f); c(13.6f, 6.4f, 13.8f, 7.6f, 13.2f, 8.6f)
                c(12.8f, 9.3f, 12.4f, 9.6f, 12f, 9.6f)
                c(11.6f, 9.6f, 11.2f, 9.3f, 10.8f, 8.6f)
                c(10.2f, 7.6f, 10.4f, 6.4f, 12f, 4.6f); z()
            }
        }
        IconId.LLAMA_AZUL -> {
            p.fill(m) { m(10.8f, 12f); l(13.2f, 12f); l(12.8f, 22f); l(11.2f, 22f); z() }
            p.fill(Color(0xFF3C8CE8)) {
                m(12f, 1.8f); c(16.4f, 5.6f, 16.6f, 8.4f, 15f, 10.2f)
                c(14.2f, 11.2f, 13.2f, 11.7f, 12f, 11.7f)
                c(10.8f, 11.7f, 9.8f, 11.2f, 9f, 10.2f)
                c(7.4f, 8.4f, 7.6f, 5.6f, 12f, 1.8f); z()
            }
            p.fill(Color(0xFFBFE6FF)) {
                m(12f, 5.4f); c(14f, 7.6f, 14.1f, 9f, 13.2f, 10f)
                c(12.8f, 10.5f, 12.4f, 10.7f, 12f, 10.7f)
                c(11.6f, 10.7f, 11.2f, 10.5f, 10.8f, 10f)
                c(9.9f, 9f, 10f, 7.6f, 12f, 5.4f); z()
            }
        }
        IconId.FRASCO -> {
            p.line(m, 1.6f) { m(9.8f, 2.5f); l(14.2f, 2.5f) }
            p.line(m, 1.6f) { m(10.6f, 2.8f); l(10.6f, 8f); l(6.6f, 17f); c(5.6f, 19.6f, 7.3f, 22f, 10f, 22f); l(14f, 22f); c(16.7f, 22f, 18.4f, 19.6f, 17.4f, 17f); l(13.4f, 8f); l(13.4f, 2.8f) }
            p.fill(a) { m(7.6f, 14.6f); l(16.4f, 14.6f); l(17.4f, 17f); c(18.4f, 19.6f, 16.7f, 22f, 14f, 22f); l(10f, 22f); c(7.3f, 22f, 5.6f, 19.6f, 6.6f, 17f); z() }
            p.dot(10f, 17.6f, 0.9f, Color(0xFFFFF0CC))
            p.dot(13.6f, 19.4f, 0.65f, Color(0xFFFFF0CC))
        }
        IconId.BURBUJA -> {
            p.ring(12f, 13.5f, 7.4f, m, 1.7f)
            p.fill(a.copy(alpha = 0.35f)) { oval(12f, 13.5f, 6.4f, 6.4f) }
            p.fill(Color(0xFFFFFFFF).copy(alpha = 0.85f)) { oval(9.4f, 10.6f, 2f, 1.4f) }
            p.ring(18.4f, 5.6f, 2.5f, m, 1.5f)
            p.ring(7.6f, 4.4f, 1.5f, m, 1.4f)
        }
        IconId.HILO -> {
            p.ring(12f, 12f, 8f, dim, 1.3f)
            p.line(m, 1.8f) {
                m(4.6f, 9f); c(8f, 6.4f, 10f, 12f, 13.4f, 9.6f)
                c(16f, 7.8f, 17.4f, 10.6f, 19.4f, 9.6f)
            }
            p.line(a, 1.8f) {
                m(4.6f, 15f); c(7.4f, 17.8f, 10.4f, 12.4f, 13.6f, 14.6f)
                c(16.2f, 16.4f, 17.6f, 14f, 19.4f, 15f)
            }
            p.dot(19.4f, 15f, 1.5f, a)
        }
        IconId.BRUJULA -> {
            p.ring(12f, 12f, 9f, m, 1.7f)
            p.ring(12f, 12f, 6.4f, dim, 1f)
            p.fill(a) { m(12f, 5.6f); l(14.4f, 12f); l(12f, 10.4f); z() }
            p.fill(m) { m(12f, 18.4f); l(9.6f, 12f); l(12f, 13.6f); z() }
            p.dot(12f, 12f, 1.1f, m)
        }
        IconId.OJO -> {
            p.line(m, 1.7f) {
                m(2.4f, 12f); c(6f, 6.4f, 18f, 6.4f, 21.6f, 12f)
                c(18f, 17.6f, 6f, 17.6f, 2.4f, 12f); z()
            }
            p.fill(a) { oval(12f, 12f, 3.6f, 3.6f) }
            p.dot(12f, 12f, 1.7f, Color(0xFF1A1712))
            p.dot(13.2f, 10.7f, 0.7f, Color(0xFFFFFFFF))
        }
        IconId.MAPA -> {
            p.fill(dim) { m(2.6f, 5.6f); l(8.8f, 3.2f); l(15.2f, 6f); l(21.4f, 3.6f); l(21.4f, 18.4f); l(15.2f, 20.8f); l(8.8f, 18f); l(2.6f, 20.4f); z() }
            p.line(m, 1.4f) { m(2.6f, 5.6f); l(8.8f, 3.2f); l(15.2f, 6f); l(21.4f, 3.6f); l(21.4f, 18.4f); l(15.2f, 20.8f); l(8.8f, 18f); l(2.6f, 20.4f); z() }
            p.seg(8.8f, 3.2f, 8.8f, 18f, m, 1.2f)
            p.seg(15.2f, 6f, 15.2f, 20.8f, m, 1.2f)
            p.dot(18.2f, 11.6f, 1.5f, a)
        }
        IconId.PERGAMINO -> {
            p.fill(dim) { rrect(5.4f, 3f, 13.2f, 18f, 1.6f) }
            p.line(m, 1.5f) { rrect(5.4f, 3f, 13.2f, 18f, 1.6f) }
            p.line(a, 1.4f) { m(3.2f, 3f); c(2f, 5.4f, 2f, 6.6f, 3.2f, 9f) }
            p.line(a, 1.4f) { m(20.8f, 15f); c(22f, 17.4f, 22f, 18.6f, 20.8f, 21f) }
            p.seg(8f, 7.6f, 16f, 7.6f, m, 1.2f)
            p.seg(8f, 11f, 16f, 11f, m, 1.2f)
            p.seg(8f, 14.4f, 13.4f, 14.4f, m, 1.2f)
            p.seg(8f, 17.6f, 15f, 17.6f, m, 1.2f)
        }
        IconId.TIZA -> {
            p.fill(m) { m(5f, 17.4f); l(15.6f, 3.6f); l(19.4f, 6.4f); l(8.8f, 20.2f); z() }
            p.fill(a) { m(5f, 17.4f); l(8.8f, 20.2f); l(4.4f, 21.4f); z() }
            p.seg(15.6f, 3.6f, 19.4f, 6.4f, dim, 1.2f)
            p.dot(19.6f, 16.6f, 1.1f, a)
            p.dot(16.4f, 20f, 0.8f, a)
        }
        IconId.PICO -> {
            p.line(m, 2.2f) { m(11f, 8.4f); l(6.2f, 20.8f) }
            p.line(a, 2f) {
                m(2.6f, 9.4f); c(7.6f, 3.6f, 16.4f, 3.6f, 21.4f, 9.4f)
                c(17.6f, 7f, 15.4f, 6.4f, 12f, 7.6f)
                c(8.6f, 6.4f, 6.4f, 7f, 2.6f, 9.4f); z()
            }
            p.dot(12f, 8.2f, 1.3f, m)
        }
        IconId.FANTASMA -> {
            p.fill(m.copy(alpha = 0.55f)) {
                m(4.6f, 21.2f); l(4.6f, 10.6f); c(4.6f, 5.6f, 8f, 2.4f, 12f, 2.4f)
                c(16f, 2.4f, 19.4f, 5.6f, 19.4f, 10.6f); l(19.4f, 21.2f)
                l(16.8f, 19f); l(14.4f, 21.2f); l(12f, 19f); l(9.6f, 21.2f); l(7.2f, 19f); z()
            }
            p.line(m, 1.5f) {
                m(4.6f, 21.2f); l(4.6f, 10.6f); c(4.6f, 5.6f, 8f, 2.4f, 12f, 2.4f)
                c(16f, 2.4f, 19.4f, 5.6f, 19.4f, 10.6f); l(19.4f, 21.2f)
                l(16.8f, 19f); l(14.4f, 21.2f); l(12f, 19f); l(9.6f, 21.2f); l(7.2f, 19f); z()
            }
            p.dot(9.4f, 10.4f, 1.35f, a)
            p.dot(14.6f, 10.4f, 1.35f, a)
        }
        IconId.IMAN -> {
            p.line(m, 3.2f) { m(4.6f, 19f); l(4.6f, 11.6f); c(4.6f, 7.4f, 8f, 4f, 12f, 4f); c(16f, 4f, 19.4f, 7.4f, 19.4f, 11.6f); l(19.4f, 19f) }
            p.line(a, 3.2f) { m(4.6f, 19f); l(4.6f, 15.6f) }
            p.line(Color(0xFF6FA8DC), 3.2f) { m(19.4f, 19f); l(19.4f, 15.6f) }
            p.seg(11f, 1.6f, 13f, 1.6f, dim, 1.2f)
        }
        IconId.RELOJ -> {
            p.line(m, 1.7f) { m(6.4f, 2.6f); l(17.6f, 2.6f) }
            p.line(m, 1.7f) { m(6.4f, 21.4f); l(17.6f, 21.4f) }
            p.line(m, 1.6f) { m(7.6f, 2.6f); l(7.6f, 6.4f); l(12f, 12f); l(16.4f, 6.4f); l(16.4f, 2.6f) }
            p.line(m, 1.6f) { m(7.6f, 21.4f); l(7.6f, 17.6f); l(12f, 12f); l(16.4f, 17.6f); l(16.4f, 21.4f) }
            p.fill(a) { m(9f, 18.6f); l(15f, 18.6f); l(15f, 20.2f); l(9f, 20.2f); z() }
            p.fill(a) { m(9.6f, 4.6f); l(14.4f, 4.6f); l(12f, 9.4f); z() }
        }
        IconId.BENGALA -> {
            p.dot(12f, 12f, 3.4f, a)
            p.dot(12f, 12f, 1.8f, Color(0xFFFFF6D8))
            for (i in 0 until 8) {
                val ang = i * 45.0 * Math.PI / 180.0
                val cx = 12f + (Math.cos(ang) * 5.2).toFloat()
                val cy = 12f + (Math.sin(ang) * 5.2).toFloat()
                val ex = 12f + (Math.cos(ang) * 9.6).toFloat()
                val ey = 12f + (Math.sin(ang) * 9.6).toFloat()
                p.seg(cx, cy, ex, ey, if (i % 2 == 0) a else m, if (i % 2 == 0) 1.9f else 1.2f)
            }
        }
        IconId.VENDA -> {
            p.fill(dim) { rrect(2.4f, 8.6f, 19.2f, 6.8f, 3.2f) }
            p.line(m, 1.5f) { rrect(2.4f, 8.6f, 19.2f, 6.8f, 3.2f) }
            p.fill(a) { rrect(8.6f, 8.6f, 6.8f, 6.8f, 1.4f) }
            p.dot(10.4f, 10.8f, 0.6f, m); p.dot(13.6f, 10.8f, 0.6f, m)
            p.dot(10.4f, 13.2f, 0.6f, m); p.dot(13.6f, 13.2f, 0.6f, m)
        }
        IconId.ESCUDO -> {
            p.fill(dim) { m(12f, 2.2f); l(20.4f, 5.4f); l(20.4f, 12f); c(20.4f, 17.4f, 16.6f, 20.6f, 12f, 22f); c(7.4f, 20.6f, 3.6f, 17.4f, 3.6f, 12f); l(3.6f, 5.4f); z() }
            p.line(m, 1.6f) { m(12f, 2.2f); l(20.4f, 5.4f); l(20.4f, 12f); c(20.4f, 17.4f, 16.6f, 20.6f, 12f, 22f); c(7.4f, 20.6f, 3.6f, 17.4f, 3.6f, 12f); l(3.6f, 5.4f); z() }
            p.fill(a) { m(12f, 6.4f); l(15.6f, 11.2f); l(12f, 17.2f); l(8.4f, 11.2f); z() }
        }
        IconId.ALA -> {
            p.fill(dim) { m(2.6f, 16.6f); c(6f, 7.6f, 13.4f, 3.6f, 21.4f, 5.4f); c(18.6f, 12.4f, 11.6f, 17.6f, 2.6f, 16.6f); z() }
            p.line(m, 1.5f) { m(2.6f, 16.6f); c(6f, 7.6f, 13.4f, 3.6f, 21.4f, 5.4f); c(18.6f, 12.4f, 11.6f, 17.6f, 2.6f, 16.6f); z() }
            p.line(a, 1.2f) { m(6.4f, 14.6f); c(9.4f, 10.4f, 13.4f, 8f, 18.4f, 7f) }
            p.line(a, 1.2f) { m(9.6f, 16.2f); c(12.4f, 13f, 15.4f, 11f, 19.6f, 9.6f) }
            p.seg(3.4f, 18.8f, 12f, 20.6f, m, 1.4f)
        }
        IconId.SEMILLA -> {
            p.fill(m) { oval(12f, 14.4f, 5.4f, 6.6f) }
            p.line(dim, 1.2f) { m(12f, 8.6f); l(12f, 20.4f) }
            p.line(a, 1.8f) { m(12f, 8.4f); c(12f, 5f, 14.4f, 3f, 17.6f, 2.8f); c(17.2f, 6f, 15.2f, 8.2f, 12f, 8.4f) }
            p.line(a, 1.4f) { m(12f, 8.4f); c(11f, 6.2f, 9f, 5f, 6.6f, 5.2f); c(7f, 7.4f, 9.2f, 8.6f, 12f, 8.4f) }
        }
        IconId.ONDA -> {
            p.dot(12f, 12f, 2f, a)
            p.ring(12f, 12f, 5f, m, 1.5f)
            p.ring(12f, 12f, 8f, m.copy(alpha = 0.6f), 1.3f)
            p.ring(12f, 12f, 10.8f, m.copy(alpha = 0.3f), 1.1f)
        }
        IconId.GOTA -> {
            p.fill(a) { m(12f, 2.4f); c(17.4f, 8.6f, 19.4f, 12.4f, 19.4f, 15.2f); c(19.4f, 19.2f, 16.1f, 22f, 12f, 22f); c(7.9f, 22f, 4.6f, 19.2f, 4.6f, 15.2f); c(4.6f, 12.4f, 6.6f, 8.6f, 12f, 2.4f); z() }
            p.fill(Color(0xFFFFFFFF).copy(alpha = 0.55f)) { oval(9.4f, 16.4f, 1.6f, 2.4f) }
        }
        IconId.ESTRELLA_SUERTE -> {
            p.fill(a) { m(12f, 1.8f); c(13.4f, 8f, 16f, 10.6f, 22.2f, 12f); c(16f, 13.4f, 13.4f, 16f, 12f, 22.2f); c(10.6f, 16f, 8f, 13.4f, 1.8f, 12f); c(8f, 10.6f, 10.6f, 8f, 12f, 1.8f); z() }
            p.dot(19.4f, 4.6f, 1.3f, m)
            p.dot(4.8f, 19f, 0.95f, m)
        }
        IconId.SOMBRA -> {
            p.fill(Color(0xFF241F2E)) { m(12f, 2.4f); c(16.4f, 2.4f, 19.6f, 6f, 19.6f, 11f); l(19.6f, 21.4f); l(4.4f, 21.4f); l(4.4f, 11f); c(4.4f, 6f, 7.6f, 2.4f, 12f, 2.4f); z() }
            p.line(m, 1.5f) { m(12f, 2.4f); c(16.4f, 2.4f, 19.6f, 6f, 19.6f, 11f); l(19.6f, 21.4f); l(4.4f, 21.4f); l(4.4f, 11f); c(4.4f, 6f, 7.6f, 2.4f, 12f, 2.4f); z() }
            p.dot(9.4f, 11f, 1.15f, a)
            p.dot(14.6f, 11f, 1.15f, a)
            p.seg(7.2f, 16.4f, 16.8f, 16.4f, m.copy(alpha = 0.35f), 1.2f)
        }
        IconId.CRISTAL_TIEMPO -> {
            p.fill(a.copy(alpha = 0.5f)) { m(12f, 1.8f); l(19.4f, 8.4f); l(16.4f, 22.2f); l(7.6f, 22.2f); l(4.6f, 8.4f); z() }
            p.line(m, 1.5f) { m(12f, 1.8f); l(19.4f, 8.4f); l(16.4f, 22.2f); l(7.6f, 22.2f); l(4.6f, 8.4f); z() }
            p.seg(12f, 1.8f, 12f, 22.2f, m.copy(alpha = 0.5f), 1.1f)
            p.seg(4.6f, 8.4f, 19.4f, 8.4f, m.copy(alpha = 0.5f), 1.1f)
            p.line(m, 1.5f) { m(14.2f, 12.4f); c(12f, 10.6f, 9.6f, 11.4f, 9.2f, 14f) }
            p.fill(m) { m(9.4f, 16.6f); l(7.2f, 13.6f); l(11.4f, 13.6f); z() }
        }
        IconId.PAN -> {
            p.fill(a) { m(3.4f, 14.4f); c(3.4f, 9.6f, 7.2f, 6.4f, 12f, 6.4f); c(16.8f, 6.4f, 20.6f, 9.6f, 20.6f, 14.4f); l(20.6f, 17.4f); c(20.6f, 18.6f, 19.6f, 19.4f, 18.4f, 19.4f); l(5.6f, 19.4f); c(4.4f, 19.4f, 3.4f, 18.6f, 3.4f, 17.4f); z() }
            p.line(m, 1.4f) { m(3.4f, 14.4f); c(3.4f, 9.6f, 7.2f, 6.4f, 12f, 6.4f); c(16.8f, 6.4f, 20.6f, 9.6f, 20.6f, 14.4f); l(20.6f, 17.4f); c(20.6f, 18.6f, 19.6f, 19.4f, 18.4f, 19.4f); l(5.6f, 19.4f); c(4.4f, 19.4f, 3.4f, 18.6f, 3.4f, 17.4f); z() }
            p.seg(8.4f, 9.4f, 6.8f, 12.4f, m, 1.3f)
            p.seg(12f, 9f, 12f, 12.4f, m, 1.3f)
            p.seg(15.6f, 9.4f, 17.2f, 12.4f, m, 1.3f)
        }

        // ---------------------------------------------------- mejoras
        IconId.BOTA -> {
            p.fill(dim) { m(6.4f, 2.6f); l(11.4f, 2.6f); l(11.4f, 12.4f); l(19f, 15.4f); c(21f, 16.2f, 21.6f, 17.6f, 21.6f, 19f); l(21.6f, 21.4f); l(4.4f, 21.4f); l(4.4f, 4.6f); c(4.4f, 3.4f, 5.2f, 2.6f, 6.4f, 2.6f); z() }
            p.line(m, 1.5f) { m(6.4f, 2.6f); l(11.4f, 2.6f); l(11.4f, 12.4f); l(19f, 15.4f); c(21f, 16.2f, 21.6f, 17.6f, 21.6f, 19f); l(21.6f, 21.4f); l(4.4f, 21.4f); l(4.4f, 4.6f); c(4.4f, 3.4f, 5.2f, 2.6f, 6.4f, 2.6f); z() }
            p.seg(4.4f, 18.2f, 21.6f, 18.2f, a, 1.5f)
            p.seg(6.6f, 6.2f, 11.4f, 6.2f, m, 1.2f)
            p.seg(6.6f, 9.2f, 11.4f, 9.2f, m, 1.2f)
        }
        IconId.PULMON -> {
            p.seg(12f, 2.6f, 12f, 10.4f, m, 1.7f)
            p.seg(9.4f, 6.4f, 14.6f, 6.4f, m, 1.4f)
            p.fill(a.copy(alpha = 0.45f)) { m(11.2f, 9.4f); c(11.2f, 12.4f, 9.6f, 12.4f, 7.4f, 14.6f); c(4.6f, 17.4f, 4.6f, 21.4f, 7.6f, 21.4f); c(10.4f, 21.4f, 11.2f, 18.6f, 11.2f, 15.4f); z() }
            p.fill(a.copy(alpha = 0.45f)) { m(12.8f, 9.4f); c(12.8f, 12.4f, 14.4f, 12.4f, 16.6f, 14.6f); c(19.4f, 17.4f, 19.4f, 21.4f, 16.4f, 21.4f); c(13.6f, 21.4f, 12.8f, 18.6f, 12.8f, 15.4f); z() }
            p.line(m, 1.4f) { m(11.2f, 9.4f); c(11.2f, 12.4f, 9.6f, 12.4f, 7.4f, 14.6f); c(4.6f, 17.4f, 4.6f, 21.4f, 7.6f, 21.4f); c(10.4f, 21.4f, 11.2f, 18.6f, 11.2f, 15.4f); z() }
            p.line(m, 1.4f) { m(12.8f, 9.4f); c(12.8f, 12.4f, 14.4f, 12.4f, 16.6f, 14.6f); c(19.4f, 17.4f, 19.4f, 21.4f, 16.4f, 21.4f); c(13.6f, 21.4f, 12.8f, 18.6f, 12.8f, 15.4f); z() }
        }
        IconId.CORAZON -> {
            p.fill(a) { m(12f, 21f); c(4.4f, 15.6f, 2.4f, 11.6f, 2.4f, 8.6f); c(2.4f, 5.4f, 4.8f, 3.2f, 7.6f, 3.2f); c(9.6f, 3.2f, 11.2f, 4.4f, 12f, 6f); c(12.8f, 4.4f, 14.4f, 3.2f, 16.4f, 3.2f); c(19.2f, 3.2f, 21.6f, 5.4f, 21.6f, 8.6f); c(21.6f, 11.6f, 19.6f, 15.6f, 12f, 21f); z() }
            p.line(Color(0xFFFFFFFF).copy(alpha = 0.5f), 1.5f) { m(6.4f, 7f); c(5.4f, 8f, 5.2f, 9.4f, 5.6f, 10.6f) }
        }
        IconId.FAROL -> {
            p.line(m, 1.5f) { m(9.4f, 3.4f); c(9.4f, 1.2f, 14.6f, 1.2f, 14.6f, 3.4f) }
            p.fill(dim) { m(6.4f, 6.4f); l(17.6f, 6.4f); l(19f, 19f); l(5f, 19f); z() }
            p.line(m, 1.5f) { m(6.4f, 6.4f); l(17.6f, 6.4f); l(19f, 19f); l(5f, 19f); z() }
            p.seg(5f, 4.6f, 19f, 4.6f, m, 1.6f)
            p.seg(4.2f, 21f, 19.8f, 21f, m, 1.6f)
            p.fill(a) { oval(12f, 12.6f, 3f, 3.6f) }
            p.dot(12f, 12.6f, 1.5f, Color(0xFFFFF6D8))
        }
        IconId.MOCHILA -> {
            p.fill(dim) { rrect(4.4f, 7f, 15.2f, 14.4f, 3f) }
            p.line(m, 1.5f) { rrect(4.4f, 7f, 15.2f, 14.4f, 3f) }
            p.line(m, 1.5f) { m(8.6f, 7f); l(8.6f, 5.2f); c(8.6f, 3.4f, 15.4f, 3.4f, 15.4f, 5.2f); l(15.4f, 7f) }
            p.fill(a) { rrect(8.4f, 12.4f, 7.2f, 6f, 1.4f) }
            p.seg(4.6f, 11.4f, 19.4f, 11.4f, m, 1.2f)
        }
        IconId.ESPIRAL -> {
            p.line(m, 1.7f) {
                m(12f, 12f)
                c(12f, 10.6f, 13.6f, 10.6f, 13.6f, 12f)
                c(13.6f, 14.2f, 10.4f, 14.2f, 10.4f, 12f)
                c(10.4f, 8.6f, 15.6f, 8.6f, 15.6f, 12f)
                c(15.6f, 16.8f, 8.4f, 16.8f, 8.4f, 12f)
                c(8.4f, 6f, 17.6f, 6f, 17.6f, 12f)
                c(17.6f, 19.2f, 6.4f, 19.2f, 6.4f, 12f)
                c(6.4f, 4f, 19.6f, 4f, 19.6f, 12f)
            }
            p.dot(12f, 12f, 1.1f, a)
        }
        IconId.CUADRICULA -> {
            p.line(m, 1.4f) { rrect(3.4f, 3.4f, 17.2f, 17.2f, 1.6f) }
            p.seg(9.2f, 3.4f, 9.2f, 20.6f, dim, 1.2f)
            p.seg(14.8f, 3.4f, 14.8f, 20.6f, dim, 1.2f)
            p.seg(3.4f, 9.2f, 20.6f, 9.2f, dim, 1.2f)
            p.seg(3.4f, 14.8f, 20.6f, 14.8f, dim, 1.2f)
            p.fill(a) { rect(9.2f, 9.2f, 5.6f, 5.6f) }
        }
        IconId.CALAVERA -> {
            p.fill(dim) { m(12f, 2.6f); c(17.4f, 2.6f, 20.6f, 6.4f, 20.6f, 11.4f); c(20.6f, 14.4f, 19.2f, 16.2f, 17.4f, 17.2f); l(17.4f, 20.4f); l(6.6f, 20.4f); l(6.6f, 17.2f); c(4.8f, 16.2f, 3.4f, 14.4f, 3.4f, 11.4f); c(3.4f, 6.4f, 6.6f, 2.6f, 12f, 2.6f); z() }
            p.line(m, 1.5f) { m(12f, 2.6f); c(17.4f, 2.6f, 20.6f, 6.4f, 20.6f, 11.4f); c(20.6f, 14.4f, 19.2f, 16.2f, 17.4f, 17.2f); l(17.4f, 20.4f); l(6.6f, 20.4f); l(6.6f, 17.2f); c(4.8f, 16.2f, 3.4f, 14.4f, 3.4f, 11.4f); c(3.4f, 6.4f, 6.6f, 2.6f, 12f, 2.6f); z() }
            p.dot(8.6f, 11f, 2.1f, a)
            p.dot(15.4f, 11f, 2.1f, a)
            p.fill(m) { m(12f, 13.4f); l(13.4f, 16f); l(10.6f, 16f); z() }
            p.seg(10f, 20.4f, 10f, 17.6f, m, 1.2f)
            p.seg(14f, 20.4f, 14f, 17.6f, m, 1.2f)
        }
        IconId.MANO -> {
            p.fill(dim) { m(7.4f, 21.4f); c(4.6f, 18.6f, 3.6f, 15.4f, 3.6f, 12.4f); l(3.6f, 9.4f); c(3.6f, 8.2f, 5.4f, 8.2f, 5.4f, 9.4f); l(5.4f, 12f); l(6.6f, 12f); l(6.6f, 4.4f); c(6.6f, 3f, 8.6f, 3f, 8.6f, 4.4f); l(8.6f, 11f); l(9.8f, 11f); l(9.8f, 3f); c(9.8f, 1.6f, 11.8f, 1.6f, 11.8f, 3f); l(11.8f, 11f); l(13f, 11f); l(13f, 4f); c(13f, 2.6f, 15f, 2.6f, 15f, 4f); l(15f, 11.4f); l(16.2f, 11.4f); l(16.6f, 7.4f); c(16.8f, 6f, 18.8f, 6.2f, 18.6f, 7.6f); l(18f, 14.4f); c(17.6f, 18f, 16.6f, 20f, 15.4f, 21.4f); z() }
            p.line(m, 1.4f) { m(7.4f, 21.4f); c(4.6f, 18.6f, 3.6f, 15.4f, 3.6f, 12.4f); l(3.6f, 9.4f); c(3.6f, 8.2f, 5.4f, 8.2f, 5.4f, 9.4f); l(5.4f, 12f); l(6.6f, 12f); l(6.6f, 4.4f); c(6.6f, 3f, 8.6f, 3f, 8.6f, 4.4f); l(8.6f, 11f); l(9.8f, 11f); l(9.8f, 3f); c(9.8f, 1.6f, 11.8f, 1.6f, 11.8f, 3f); l(11.8f, 11f); l(13f, 11f); l(13f, 4f); c(13f, 2.6f, 15f, 2.6f, 15f, 4f); l(15f, 11.4f); l(16.2f, 11.4f); l(16.6f, 7.4f); c(16.8f, 6f, 18.8f, 6.2f, 18.6f, 7.6f); l(18f, 14.4f); c(17.6f, 18f, 16.6f, 20f, 15.4f, 21.4f); z() }
        }
        IconId.ROCA -> {
            p.fill(dim) { m(3.4f, 13.4f); l(7.6f, 5.4f); l(15.6f, 3.4f); l(21f, 9.6f); l(19.4f, 19.4f); l(8.4f, 21.2f); z() }
            p.line(m, 1.5f) { m(3.4f, 13.4f); l(7.6f, 5.4f); l(15.6f, 3.4f); l(21f, 9.6f); l(19.4f, 19.4f); l(8.4f, 21.2f); z() }
            p.line(a, 1.3f) { m(7.6f, 5.4f); l(11.4f, 12f); l(21f, 9.6f) }
            p.line(a, 1.3f) { m(11.4f, 12f); l(8.4f, 21.2f) }
            p.line(a, 1.3f) { m(11.4f, 12f); l(19.4f, 19.4f) }
        }
        IconId.PLUMA -> {
            p.fill(dim) { m(20.6f, 3.4f); c(20.6f, 12.4f, 15.4f, 17.4f, 8.4f, 18.4f); l(5.6f, 18.4f); c(6.6f, 11.4f, 11.6f, 4.4f, 20.6f, 3.4f); z() }
            p.line(m, 1.4f) { m(20.6f, 3.4f); c(20.6f, 12.4f, 15.4f, 17.4f, 8.4f, 18.4f); l(5.6f, 18.4f); c(6.6f, 11.4f, 11.6f, 4.4f, 20.6f, 3.4f); z() }
            p.line(a, 1.3f) { m(20.6f, 3.4f); l(3.4f, 20.6f) }
            p.seg(11.4f, 9.4f, 15.4f, 8.4f, m, 1.1f)
            p.seg(9.4f, 13.4f, 13.4f, 12.4f, m, 1.1f)
        }
        IconId.CATALEJO -> {
            p.fill(dim) { m(2.6f, 14.6f); l(8.4f, 11.4f); l(9.6f, 13.6f); l(3.8f, 16.8f); z() }
            p.fill(dim) { m(8.4f, 11.4f); l(15f, 7.8f); l(16.6f, 10.6f); l(9.6f, 13.6f); z() }
            p.fill(a) { m(15f, 7.8f); l(20.4f, 4.8f); l(22.2f, 8f); l(16.6f, 10.6f); z() }
            p.line(m, 1.3f) { m(2.6f, 14.6f); l(8.4f, 11.4f); l(15f, 7.8f); l(20.4f, 4.8f); l(22.2f, 8f); l(16.6f, 10.6f); l(9.6f, 13.6f); l(3.8f, 16.8f); z() }
            p.seg(8.4f, 11.4f, 9.6f, 13.6f, m, 1.2f)
            p.seg(15f, 7.8f, 16.6f, 10.6f, m, 1.2f)
            p.dot(6f, 20f, 1f, a)
        }
        IconId.SACO_MONEDAS -> {
            p.fill(dim) { m(7.4f, 7.4f); l(16.6f, 7.4f); c(20.4f, 10.4f, 21.6f, 14.4f, 20.4f, 18f); c(19.4f, 20.8f, 16.4f, 21.6f, 12f, 21.6f); c(7.6f, 21.6f, 4.6f, 20.8f, 3.6f, 18f); c(2.4f, 14.4f, 3.6f, 10.4f, 7.4f, 7.4f); z() }
            p.line(m, 1.5f) { m(7.4f, 7.4f); l(16.6f, 7.4f); c(20.4f, 10.4f, 21.6f, 14.4f, 20.4f, 18f); c(19.4f, 20.8f, 16.4f, 21.6f, 12f, 21.6f); c(7.6f, 21.6f, 4.6f, 20.8f, 3.6f, 18f); c(2.4f, 14.4f, 3.6f, 10.4f, 7.4f, 7.4f); z() }
            p.line(m, 1.5f) { m(6.6f, 7.4f); l(9.4f, 2.6f); l(14.6f, 2.6f); l(17.4f, 7.4f) }
            p.ring(12f, 15f, 3.2f, a, 1.5f)
            p.seg(12f, 12.8f, 12f, 17.2f, a, 1.4f)
        }
        IconId.HUELLA -> {
            p.fill(dim) { oval(9f, 9.4f, 4.2f, 5.6f) }
            p.line(m, 1.3f) { oval(9f, 9.4f, 4.2f, 5.6f) }
            p.fill(a.copy(alpha = 0.55f)) { oval(15.4f, 17f, 3.4f, 4.4f) }
            p.line(m, 1.3f) { oval(15.4f, 17f, 3.4f, 4.4f) }
            p.dot(5.6f, 3.6f, 1.15f, m)
            p.dot(8.6f, 2.8f, 1.15f, m)
            p.dot(11.6f, 3.6f, 1.05f, m)
            p.dot(13.4f, 11.6f, 0.95f, m)
            p.dot(16f, 11f, 0.95f, m)
            p.dot(18.2f, 11.8f, 0.85f, m)
        }
        IconId.CUERDA -> {
            p.line(m, 2.2f) {
                m(4.4f, 2.6f)
                c(9.4f, 5f, 4.4f, 8f, 9.4f, 10.4f)
                c(14.4f, 12.8f, 9.4f, 15.8f, 14.4f, 18.2f)
                c(17.4f, 19.6f, 17.4f, 20.6f, 16.4f, 21.4f)
            }
            p.line(a, 1.4f) { m(6.4f, 3.6f); l(7.4f, 1.8f) }
            p.line(a, 1.4f) { m(11.4f, 11.4f); l(12.4f, 9.6f) }
            p.ring(17.4f, 17.4f, 3.4f, a, 1.6f)
        }

        // -------------------------------------------------- reliquias
        IconId.COLMILLO -> {
            p.fill(Color(0xFFF0E6D2)) { m(8.4f, 2.6f); l(15.6f, 2.6f); c(15.6f, 10f, 14.4f, 16.4f, 12f, 21.4f); c(9.6f, 16.4f, 8.4f, 10f, 8.4f, 2.6f); z() }
            p.line(m, 1.4f) { m(8.4f, 2.6f); l(15.6f, 2.6f); c(15.6f, 10f, 14.4f, 16.4f, 12f, 21.4f); c(9.6f, 16.4f, 8.4f, 10f, 8.4f, 2.6f); z() }
            p.seg(6.4f, 2.6f, 17.6f, 2.6f, a, 1.8f)
            p.seg(10.4f, 7.4f, 10.8f, 14.4f, m.copy(alpha = 0.4f), 1.1f)
        }
        IconId.AMBAR -> {
            p.fill(Color(0xFFE8A33D)) { m(12f, 2.4f); l(19.6f, 7.4f); l(19.6f, 16.6f); l(12f, 21.6f); l(4.4f, 16.6f); l(4.4f, 7.4f); z() }
            p.line(m, 1.4f) { m(12f, 2.4f); l(19.6f, 7.4f); l(19.6f, 16.6f); l(12f, 21.6f); l(4.4f, 16.6f); l(4.4f, 7.4f); z() }
            p.fill(Color(0xFF3A2A16)) { oval(12f, 11f, 1.5f, 2.4f) }
            p.seg(12f, 8.6f, 10.4f, 6.6f, Color(0xFF3A2A16), 1f)
            p.seg(12f, 8.6f, 13.6f, 6.6f, Color(0xFF3A2A16), 1f)
            p.seg(11.4f, 13.4f, 9.8f, 15.4f, Color(0xFF3A2A16), 1f)
            p.seg(12.6f, 13.4f, 14.2f, 15.4f, Color(0xFF3A2A16), 1f)
            p.fill(Color(0xFFFFFFFF).copy(alpha = 0.4f)) { m(7.4f, 8.6f); l(10f, 7f); l(10f, 9.4f); l(7.4f, 11f); z() }
        }
        IconId.ROSA_VIENTOS -> {
            p.ring(12f, 12f, 9.4f, dim, 1.2f)
            p.fill(m) { m(12f, 1.6f); l(14f, 10f); l(12f, 12f); l(10f, 10f); z() }
            p.fill(a) { m(12f, 22.4f); l(10f, 14f); l(12f, 12f); l(14f, 14f); z() }
            p.fill(dim) { m(1.6f, 12f); l(10f, 10f); l(12f, 12f); l(10f, 14f); z() }
            p.fill(dim) { m(22.4f, 12f); l(14f, 14f); l(12f, 12f); l(14f, 10f); z() }
            p.dot(12f, 12f, 1.1f, a)
        }
        IconId.LAGRIMA -> {
            p.fill(Color(0xFF9FD9F5)) { m(12f, 2.6f); c(16.4f, 9f, 18.4f, 12.6f, 18.4f, 15.4f); c(18.4f, 19f, 15.6f, 21.6f, 12f, 21.6f); c(8.4f, 21.6f, 5.6f, 19f, 5.6f, 15.4f); c(5.6f, 12.6f, 7.6f, 9f, 12f, 2.6f); z() }
            p.line(m, 1.4f) { m(12f, 2.6f); c(16.4f, 9f, 18.4f, 12.6f, 18.4f, 15.4f); c(18.4f, 19f, 15.6f, 21.6f, 12f, 21.6f); c(8.4f, 21.6f, 5.6f, 19f, 5.6f, 15.4f); c(5.6f, 12.6f, 7.6f, 9f, 12f, 2.6f); z() }
            p.fill(a) { m(12f, 11.4f); c(14.4f, 14.4f, 14.6f, 16.4f, 13.4f, 17.8f); c(12.6f, 18.6f, 11.4f, 18.6f, 10.6f, 17.8f); c(9.4f, 16.4f, 9.6f, 14.4f, 12f, 11.4f); z() }
        }
        IconId.NUDO -> {
            p.line(m, 2.2f) { m(4.6f, 8.4f); c(8.6f, 3.4f, 15.4f, 3.4f, 19.4f, 8.4f); c(15.4f, 12.4f, 15.4f, 17.4f, 19.4f, 20.4f) }
            p.line(a, 2.2f) { m(19.4f, 8.4f); c(15.4f, 3.4f, 8.6f, 3.4f, 4.6f, 8.4f); c(8.6f, 12.4f, 8.6f, 17.4f, 4.6f, 20.4f) }
            p.dot(12f, 10.8f, 1.5f, m)
        }
        IconId.LUCIERNAGA -> {
            p.fill(dim) { oval(11f, 14.4f, 3.4f, 5.6f) }
            p.line(m, 1.3f) { oval(11f, 14.4f, 3.4f, 5.6f) }
            p.fill(Color(0xFFB9F27C)) { oval(11f, 18.4f, 2.6f, 2.6f) }
            p.line(m, 1.3f) { m(9.4f, 9.4f); c(6.4f, 5.4f, 4.4f, 4.4f, 2.6f, 4.6f); c(3.4f, 7.4f, 5.6f, 9.4f, 9f, 10.6f) }
            p.line(m, 1.3f) { m(12.6f, 9.4f); c(15.6f, 5.4f, 18.4f, 4.4f, 21.4f, 4.6f); c(20.4f, 7.4f, 17.4f, 9.4f, 13.2f, 10.6f) }
            p.dot(11f, 8.4f, 1.5f, m)
            p.seg(10.2f, 6.4f, 9.4f, 4.4f, m, 1f)
            p.seg(11.8f, 6.4f, 12.6f, 4.4f, m, 1f)
        }
        IconId.RUNA -> {
            p.line(m, 1.4f) { m(12f, 1.8f); l(20.6f, 6.6f); l(20.6f, 17.4f); l(12f, 22.2f); l(3.4f, 17.4f); l(3.4f, 6.6f); z() }
            p.line(a, 2f) { m(9f, 6.4f); l(9f, 17.6f) }
            p.line(a, 2f) { m(9f, 11.4f); l(15f, 6.4f) }
            p.line(a, 2f) { m(9f, 11.4f); l(15f, 17.6f) }
        }
        IconId.TORRE_VIGIA -> {
            p.fill(dim) { m(6.6f, 21.4f); l(8.4f, 9.4f); l(15.6f, 9.4f); l(17.4f, 21.4f); z() }
            p.line(m, 1.4f) { m(6.6f, 21.4f); l(8.4f, 9.4f); l(15.6f, 9.4f); l(17.4f, 21.4f); z() }
            p.line(m, 1.4f) { m(5.4f, 9.4f); l(18.6f, 9.4f); l(18.6f, 6.4f); l(16.4f, 6.4f); l(16.4f, 4.4f); l(14.2f, 4.4f); l(14.2f, 6.4f); l(9.8f, 6.4f); l(9.8f, 4.4f); l(7.6f, 4.4f); l(7.6f, 6.4f); l(5.4f, 6.4f); z() }
            p.fill(a) { oval(12f, 14.6f, 1.8f, 2.4f) }
            p.seg(9.6f, 21.4f, 10.4f, 17.4f, m, 1.1f)
        }
        IconId.CORAZONES_GEMELOS -> {
            p.fill(a) { m(8.4f, 18.4f); c(3.4f, 14.6f, 2.2f, 11.6f, 2.2f, 9.4f); c(2.2f, 7.2f, 3.8f, 5.6f, 5.8f, 5.6f); c(7f, 5.6f, 8f, 6.4f, 8.4f, 7.4f); c(8.8f, 6.4f, 9.8f, 5.6f, 11f, 5.6f); c(13f, 5.6f, 14.6f, 7.2f, 14.6f, 9.4f); c(14.6f, 11.6f, 13.4f, 14.6f, 8.4f, 18.4f); z() }
            p.fill(m) { m(16.2f, 20.4f); c(12.6f, 17.6f, 11.8f, 15.4f, 11.8f, 13.8f); c(11.8f, 12.2f, 12.9f, 11f, 14.3f, 11f); c(15.2f, 11f, 15.9f, 11.6f, 16.2f, 12.3f); c(16.5f, 11.6f, 17.2f, 11f, 18.1f, 11f); c(19.5f, 11f, 20.6f, 12.2f, 20.6f, 13.8f); c(20.6f, 15.4f, 19.8f, 17.6f, 16.2f, 20.4f); z() }
        }
        IconId.VETA -> {
            p.fill(dim) { m(2.6f, 15.4f); l(6.4f, 6.4f); l(16.4f, 3.4f); l(21.4f, 10.4f); l(18.4f, 20.6f); l(7.4f, 21.4f); z() }
            p.line(m, 1.4f) { m(2.6f, 15.4f); l(6.4f, 6.4f); l(16.4f, 3.4f); l(21.4f, 10.4f); l(18.4f, 20.6f); l(7.4f, 21.4f); z() }
            p.line(a, 2.1f) { m(5.4f, 11.4f); l(9.4f, 12.4f); l(12.4f, 8.4f); l(16.4f, 11.4f); l(19.4f, 9.4f) }
            p.line(a, 1.6f) { m(9.4f, 12.4f); l(10.4f, 18.4f) }
            p.dot(12.4f, 8.4f, 1.2f, a)
        }
        IconId.ESCAMA -> {
            p.fill(Color(0xFFE07A3C)) { m(12f, 21.6f); c(6.4f, 18.4f, 4.4f, 13.4f, 4.4f, 8.4f); c(4.4f, 5.4f, 7.4f, 2.4f, 12f, 2.4f); c(16.6f, 2.4f, 19.6f, 5.4f, 19.6f, 8.4f); c(19.6f, 13.4f, 17.6f, 18.4f, 12f, 21.6f); z() }
            p.line(m, 1.4f) { m(12f, 21.6f); c(6.4f, 18.4f, 4.4f, 13.4f, 4.4f, 8.4f); c(4.4f, 5.4f, 7.4f, 2.4f, 12f, 2.4f); c(16.6f, 2.4f, 19.6f, 5.4f, 19.6f, 8.4f); c(19.6f, 13.4f, 17.6f, 18.4f, 12f, 21.6f); z() }
            p.line(Color(0xFFFFD9A8), 1.3f) { m(7.4f, 8.4f); c(9.4f, 6.4f, 14.6f, 6.4f, 16.6f, 8.4f) }
            p.line(Color(0xFFFFD9A8), 1.3f) { m(7.6f, 13f); c(9.6f, 11f, 14.4f, 11f, 16.4f, 13f) }
            p.line(Color(0xFFFFD9A8), 1.3f) { m(9f, 17.2f); c(10.4f, 15.8f, 13.6f, 15.8f, 15f, 17.2f) }
        }
        IconId.DIAPASON -> {
            p.line(m, 2.1f) { m(8f, 2.6f); l(8f, 12.4f) }
            p.line(m, 2.1f) { m(16f, 2.6f); l(16f, 12.4f) }
            p.line(m, 2.1f) { m(8f, 12.4f); c(8f, 15.4f, 16f, 15.4f, 16f, 12.4f) }
            p.line(m, 2.1f) { m(12f, 14.8f); l(12f, 21.4f) }
            p.line(a, 1.3f) { m(4.4f, 4.4f); c(2.6f, 6.4f, 2.6f, 8.6f, 4.4f, 10.6f) }
            p.line(a, 1.3f) { m(19.6f, 4.4f); c(21.4f, 6.4f, 21.4f, 8.6f, 19.6f, 10.6f) }
        }

        // ------------------------------------------------- cosmeticos
        IconId.GUANTE -> {
            p.fill(Color(0xFF8A6340)) { m(5.4f, 21.4f); l(5.4f, 11f); c(5.4f, 9.6f, 7.2f, 9.6f, 7.2f, 11f); l(7.2f, 12.4f); l(8.2f, 12.4f); l(8.2f, 5.4f); c(8.2f, 4f, 10f, 4f, 10f, 5.4f); l(10f, 12f); l(11f, 12f); l(11f, 4.4f); c(11f, 3f, 12.8f, 3f, 12.8f, 4.4f); l(12.8f, 12f); l(13.8f, 12f); l(13.8f, 5.6f); c(13.8f, 4.2f, 15.6f, 4.2f, 15.6f, 5.6f); l(15.6f, 12.6f); l(16.6f, 12.6f); l(17.4f, 9f); c(17.7f, 7.6f, 19.4f, 8f, 19.1f, 9.4f); l(18.2f, 15.4f); c(18.2f, 18.4f, 17.4f, 20.2f, 16.6f, 21.4f); z() }
            p.line(m, 1.3f) { m(5.4f, 21.4f); l(5.4f, 11f); c(5.4f, 9.6f, 7.2f, 9.6f, 7.2f, 11f); l(7.2f, 12.4f); l(8.2f, 12.4f); l(8.2f, 5.4f); c(8.2f, 4f, 10f, 4f, 10f, 5.4f); l(10f, 12f); l(11f, 12f); l(11f, 4.4f); c(11f, 3f, 12.8f, 3f, 12.8f, 4.4f); l(12.8f, 12f); l(13.8f, 12f); l(13.8f, 5.6f); c(13.8f, 4.2f, 15.6f, 4.2f, 15.6f, 5.6f); l(15.6f, 12.6f); l(16.6f, 12.6f); l(17.4f, 9f); c(17.7f, 7.6f, 19.4f, 8f, 19.1f, 9.4f); l(18.2f, 15.4f); c(18.2f, 18.4f, 17.4f, 20.2f, 16.6f, 21.4f); z() }
            p.seg(5.6f, 18.4f, 18f, 18.4f, a, 1.6f)
        }
        IconId.MALLA -> {
            p.line(m, 1.4f) { m(5.4f, 21.4f); l(5.4f, 10.4f); c(5.4f, 4.6f, 18.6f, 4.6f, 18.6f, 10.4f); l(18.6f, 21.4f); z() }
            var yy = 8.4f
            var row = 0
            while (yy < 20.4f) {
                var xx = if (row % 2 == 0) 7f else 8.4f
                while (xx < 18f) { p.ring(xx, yy, 1.25f, a, 0.85f); xx += 2.8f }
                yy += 2.4f; row++
            }
            p.seg(5.4f, 6.6f, 18.6f, 6.6f, m, 1.5f)
        }
        IconId.CENIZA -> {
            p.fill(Color(0xFF3A3330)) { m(5.4f, 21.4f); l(5.4f, 10.4f); c(5.4f, 4.6f, 18.6f, 4.6f, 18.6f, 10.4f); l(18.6f, 21.4f); z() }
            p.line(m, 1.3f) { m(5.4f, 21.4f); l(5.4f, 10.4f); c(5.4f, 4.6f, 18.6f, 4.6f, 18.6f, 10.4f); l(18.6f, 21.4f); z() }
            p.line(Color(0xFFFF7A2E), 1.5f) { m(8.4f, 20.4f); l(10.4f, 15.4f); l(8.8f, 13.4f); l(11.4f, 9.4f) }
            p.line(Color(0xFFFFB05E), 1.3f) { m(14.4f, 20.4f); l(13.4f, 16.4f); l(15.6f, 13.4f); l(14.4f, 9.6f) }
            p.dot(11.4f, 8.6f, 0.9f, Color(0xFFFFD9A0))
        }
        IconId.CAZADOR -> {
            p.fill(Color(0xFF6B4A2E)) { m(5.4f, 21.4f); l(5.4f, 10.4f); c(5.4f, 4.6f, 18.6f, 4.6f, 18.6f, 10.4f); l(18.6f, 21.4f); z() }
            p.line(m, 1.3f) { m(5.4f, 21.4f); l(5.4f, 10.4f); c(5.4f, 4.6f, 18.6f, 4.6f, 18.6f, 10.4f); l(18.6f, 21.4f); z() }
            p.fill(Color(0xFF9EE7FF)) { m(12f, 9.4f); l(15.4f, 13f); l(12f, 17.4f); l(8.6f, 13f); z() }
            p.line(m, 1.2f) { m(12f, 9.4f); l(15.4f, 13f); l(12f, 17.4f); l(8.6f, 13f); z() }
            p.seg(5.4f, 19.6f, 18.6f, 19.6f, a, 1.5f)
        }
        IconId.BRAZALETE -> {
            p.line(m, 1.4f) { m(6.4f, 21.4f); l(6.4f, 12.4f); c(6.4f, 7.4f, 17.6f, 7.4f, 17.6f, 12.4f); l(17.6f, 21.4f); z() }
            p.fill(Color(0xFF9EE7FF).copy(alpha = 0.85f)) { m(12f, 2.4f); l(15.4f, 6.4f); l(14.4f, 12f); l(9.6f, 12f); l(8.6f, 6.4f); z() }
            p.line(m, 1.3f) { m(12f, 2.4f); l(15.4f, 6.4f); l(14.4f, 12f); l(9.6f, 12f); l(8.6f, 6.4f); z() }
            p.seg(12f, 2.4f, 12f, 12f, m.copy(alpha = 0.5f), 1f)
            p.dot(7.4f, 4.4f, 0.9f, a); p.dot(17f, 3.6f, 0.7f, a)
            p.seg(6.6f, 17.4f, 17.4f, 17.4f, a, 1.5f)
        }
        IconId.TINTE_CALIDO -> drawTint(p, Color(0xFFFFC58A))
        IconId.TINTE_AMBAR -> drawTint(p, Color(0xFFFFB25E))
        IconId.TINTE_VERDE -> drawTint(p, Color(0xFF7BF0A8))
        // ------------------------------------------------ poderes
        IconId.LINTERNA -> {
            // Cuerpo de linterna de carburo con el haz saliendo.
            p.fill(dim) { m(3.4f, 8.6f); l(9.4f, 8.6f); l(9.4f, 15.4f); l(3.4f, 15.4f); z() }
            p.line(m, 1.4f) { rect(3.4f, 8.6f, 6f, 6.8f) }
            p.line(m, 1.4f) { m(9.4f, 9.8f); l(11.6f, 7.6f); l(11.6f, 16.4f); l(9.4f, 14.2f); z() }
            p.seg(4.6f, 15.4f, 4.6f, 19.4f, m, 1.4f)
            p.seg(8.2f, 15.4f, 8.2f, 19.4f, m, 1.4f)
            p.fill(a.copy(alpha = 0.32f)) { m(11.6f, 7.6f); l(21.4f, 3.4f); l(21.4f, 20.6f); l(11.6f, 16.4f); z() }
            p.seg(13.4f, 12f, 20.4f, 12f, a, 1.2f)
        }
        IconId.GATEO -> {
            // Silueta a cuatro patas avanzando.
            p.dot(6.4f, 11.4f, 2f, m)
            p.line(m, 1.9f) { m(8.2f, 12.4f); l(13.4f, 13.4f); l(18.4f, 12.4f) }
            p.line(m, 1.9f) { m(9.6f, 12.8f); l(9.4f, 18.4f) }
            p.line(m, 1.9f) { m(13.4f, 13.4f); l(13.4f, 18.4f) }
            p.line(m, 1.9f) { m(17.8f, 12.6f); l(19.4f, 18.4f) }
            p.line(a, 1.4f) { m(2.6f, 20.6f); l(21.4f, 20.6f) }
            p.line(a, 1.3f) { m(19.4f, 6.6f); l(21.4f, 8.6f); l(19.4f, 10.6f) }
        }
        IconId.PEZUNA -> {
            // Huella hendida de cabra con lineas de impulso.
            p.fill(m) { m(9.4f, 5.4f); c(11.4f, 4.4f, 11.4f, 9.4f, 11f, 13.4f); c(10.8f, 16.4f, 8.4f, 16.4f, 8.2f, 13.4f); c(7.8f, 9.4f, 7.4f, 6.4f, 9.4f, 5.4f); z() }
            p.fill(m) { m(15.4f, 5.4f); c(17.4f, 6.4f, 17f, 9.4f, 16.6f, 13.4f); c(16.4f, 16.4f, 14f, 16.4f, 13.8f, 13.4f); c(13.4f, 9.4f, 13.4f, 4.4f, 15.4f, 5.4f); z() }
            p.line(a, 1.4f) { m(6.4f, 19.4f); l(10.4f, 19.4f) }
            p.line(a, 1.4f) { m(13.6f, 19.4f); l(17.6f, 19.4f) }
            p.line(a, 1.3f) { m(4.4f, 21.6f); l(19.6f, 21.6f) }
        }
        IconId.VETA_DORADA -> {
            // Ojo con una veta de mineral en lugar de pupila.
            p.line(m, 1.6f) { m(2.6f, 12f); c(6.4f, 6.4f, 17.6f, 6.4f, 21.4f, 12f); c(17.6f, 17.6f, 6.4f, 17.6f, 2.6f, 12f); z() }
            p.dot(12f, 12f, 4.2f, dim)
            p.fill(a) { m(10f, 15.4f); l(12.4f, 10.4f); l(13.4f, 12.4f); l(15f, 9f); l(14f, 12.6f); l(12.6f, 11.4f); z() }
            p.dot(10.2f, 10.4f, 1.05f, a)
        }
        IconId.PICO_ETERNO -> {
            // Pico con un anillo de "no se gasta nunca".
            p.line(m, 1.9f) { m(4.4f, 8.4f); c(9.4f, 4.4f, 14.6f, 4.4f, 19.6f, 8.4f) }
            p.seg(12f, 6.4f, 12f, 19.4f, m, 2f)
            p.line(a, 1.5f) { m(6.4f, 15.4f); c(6.4f, 12.4f, 10.4f, 12.4f, 10.4f, 15.4f); c(10.4f, 18.4f, 6.4f, 18.4f, 6.4f, 15.4f); z() }
            p.line(a, 1.5f) { m(13.6f, 15.4f); c(13.6f, 12.4f, 17.6f, 12.4f, 17.6f, 15.4f); c(17.6f, 18.4f, 13.6f, 18.4f, 13.6f, 15.4f); z() }
        }
        IconId.MAPA_GRABADO -> {
            // Tablilla de piedra con el plano grabado.
            p.fill(dim) { m(4.4f, 3.6f); l(19.6f, 3.6f); l(19.6f, 20.4f); l(4.4f, 20.4f); z() }
            p.line(m, 1.5f) { rect(4.4f, 3.6f, 15.2f, 16.8f) }
            p.line(a, 1.5f) { m(7.4f, 17.4f); l(7.4f, 11.4f); l(12f, 11.4f); l(12f, 7f); l(16.6f, 7f) }
            p.dot(7.4f, 17.4f, 1.2f, a)
            p.dot(16.6f, 7f, 1.2f, a)
        }
        IconId.LATIDO -> {
            // Corazon de roca con la linea del pulso.
            p.fill(dim) { m(12f, 20.4f); c(4.4f, 14.4f, 3.4f, 10.4f, 5.4f, 7.4f); c(7.4f, 4.4f, 11f, 5.4f, 12f, 8.4f); c(13f, 5.4f, 16.6f, 4.4f, 18.6f, 7.4f); c(20.6f, 10.4f, 19.6f, 14.4f, 12f, 20.4f); z() }
            p.line(m, 1.4f) { m(12f, 20.4f); c(4.4f, 14.4f, 3.4f, 10.4f, 5.4f, 7.4f); c(7.4f, 4.4f, 11f, 5.4f, 12f, 8.4f); c(13f, 5.4f, 16.6f, 4.4f, 18.6f, 7.4f); c(20.6f, 10.4f, 19.6f, 14.4f, 12f, 20.4f); z() }
            p.line(a, 1.7f) { m(4.6f, 12.4f); l(8.4f, 12.4f); l(10f, 9.4f); l(12.4f, 15.4f); l(14.4f, 12.4f); l(19.4f, 12.4f) }
        }
        IconId.SIGILO -> {
            // Silueta encapuchada difuminandose en la sombra.
            p.fill(dim) { m(12f, 3.6f); c(16f, 3.6f, 17.6f, 7.4f, 17f, 11.4f); l(18.4f, 20.4f); l(5.6f, 20.4f); l(7f, 11.4f); c(6.4f, 7.4f, 8f, 3.6f, 12f, 3.6f); z() }
            p.line(m, 1.4f) { m(12f, 3.6f); c(16f, 3.6f, 17.6f, 7.4f, 17f, 11.4f); l(18.4f, 20.4f); l(5.6f, 20.4f); l(7f, 11.4f); c(6.4f, 7.4f, 8f, 3.6f, 12f, 3.6f); z() }
            p.dot(10.2f, 9.6f, 0.95f, a)
            p.dot(13.8f, 9.6f, 0.95f, a)
            p.seg(2.6f, 15.4f, 5.4f, 15.4f, dim, 1.3f)
            p.seg(18.6f, 15.4f, 21.4f, 15.4f, dim, 1.3f)
        }

        IconId.TINTE_ROJO -> drawTint(p, Color(0xFFFF6A5E))

        else -> return false
    }
    return true
}

private fun drawTint(p: Pen, c: Color) {
    p.dot(12f, 12f, 6.4f, c.copy(alpha = 0.25f))
    p.dot(12f, 12f, 4.4f, c.copy(alpha = 0.55f))
    p.dot(12f, 12f, 2.6f, c)
    for (i in 0 until 6) {
        val ang = i * 60.0 * Math.PI / 180.0
        val sx = 12f + (Math.cos(ang) * 8.2).toFloat()
        val sy = 12f + (Math.sin(ang) * 8.2).toFloat()
        val ex = 12f + (Math.cos(ang) * 10.8).toFloat()
        val ey = 12f + (Math.sin(ang) * 10.8).toFloat()
        p.seg(sx, sy, ex, ey, c, 1.5f)
    }
}
