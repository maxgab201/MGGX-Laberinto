package com.mggx.laberinto.ui.icons

import androidx.compose.ui.graphics.Color

/** Iconos de interfaz. Todos vectoriales, sin ninguna dependencia externa. */
fun drawUiIcon(p: Pen, id: IconId) {
    val m = p.main
    val a = p.accent
    val dim = m.copy(alpha = 0.42f)
    when (id) {
        IconId.MONEDA_ECO -> {
            p.dot(12f, 12f, 9.4f, Color(0xFF6E8FA8))
            p.dot(12f, 12f, 7.6f, Color(0xFFA9CBE0))
            p.ring(12f, 12f, 4.6f, Color(0xFF3E5568), 1.3f)
            p.ring(12f, 12f, 2.4f, Color(0xFF3E5568), 1.1f)
            p.dot(12f, 12f, 0.9f, Color(0xFF3E5568))
            p.fill(Color(0xFFFFFFFF).copy(alpha = 0.45f)) { m(6.4f, 8.4f); c(8f, 5.6f, 11f, 4.4f, 13.4f, 4.8f); c(10.4f, 5.6f, 8.2f, 7f, 6.4f, 8.4f); z() }
        }
        IconId.MONEDA_VETAGRIS -> {
            p.fill(Color(0xFF8F98A6)) { m(12f, 2.2f); l(20f, 7f); l(20f, 17f); l(12f, 21.8f); l(4f, 17f); l(4f, 7f); z() }
            p.fill(Color(0xFFCBD5E0)) { m(12f, 5.4f); l(17.2f, 8.6f); l(17.2f, 15.4f); l(12f, 18.6f); l(6.8f, 15.4f); l(6.8f, 8.6f); z() }
            p.line(Color(0xFF4A525C), 1.4f) { m(9.4f, 14.6f); l(11.4f, 9.4f); l(13.4f, 13f); l(15f, 10.4f) }
            p.dot(15f, 10.4f, 1.05f, Color(0xFF4A525C))
        }
        IconId.JUGAR -> {
            p.fill(m) { m(7.4f, 4.4f); l(19.4f, 12f); l(7.4f, 19.6f); z() }
        }
        IconId.TIENDA -> {
            p.line(m, 1.5f) { m(3.4f, 8.6f); l(5.4f, 3.6f); l(18.6f, 3.6f); l(20.6f, 8.6f) }
            p.line(m, 1.5f) { m(4.6f, 8.6f); l(4.6f, 20.4f); l(19.4f, 20.4f); l(19.4f, 8.6f) }
            p.fill(a) { m(3.4f, 8.6f); c(3.4f, 11.4f, 7.6f, 11.4f, 7.6f, 8.6f); c(7.6f, 11.4f, 11.8f, 11.4f, 11.8f, 8.6f); c(11.8f, 11.4f, 16f, 11.4f, 16f, 8.6f); c(16f, 11.4f, 20.6f, 11.4f, 20.6f, 8.6f); l(20.6f, 8.6f); l(3.4f, 8.6f); z() }
            p.line(m, 1.4f) { rect(9.4f, 13.6f, 5.2f, 6.8f) }
        }
        IconId.ENGRANAJE -> {
            p.ring(12f, 12f, 4.4f, m, 1.7f)
            for (i in 0 until 8) {
                val ang = i * 45.0 * Math.PI / 180.0
                val sx = 12f + (Math.cos(ang) * 6.6).toFloat()
                val sy = 12f + (Math.sin(ang) * 6.6).toFloat()
                val ex = 12f + (Math.cos(ang) * 9.8).toFloat()
                val ey = 12f + (Math.sin(ang) * 9.8).toFloat()
                p.seg(sx, sy, ex, ey, m, 2.3f)
            }
            p.dot(12f, 12f, 1.6f, a)
        }
        IconId.LOGROS -> {
            p.line(m, 1.5f) { m(7.4f, 3.4f); l(16.6f, 3.4f); l(16.6f, 9.4f); c(16.6f, 12.4f, 14.4f, 14.4f, 12f, 14.4f); c(9.6f, 14.4f, 7.4f, 12.4f, 7.4f, 9.4f); z() }
            p.line(m, 1.4f) { m(7.4f, 5f); l(4f, 5f); l(4f, 7.4f); c(4f, 9.6f, 5.6f, 10.6f, 7.4f, 10.6f) }
            p.line(m, 1.4f) { m(16.6f, 5f); l(20f, 5f); l(20f, 7.4f); c(20f, 9.6f, 18.4f, 10.6f, 16.6f, 10.6f) }
            p.seg(12f, 14.4f, 12f, 18f, m, 1.6f)
            p.seg(8f, 20.6f, 16f, 20.6f, m, 1.8f)
            p.seg(9.6f, 18f, 14.4f, 18f, m, 1.6f)
            p.fill(a) { m(12f, 6.2f); l(13f, 8.4f); l(15.4f, 8.7f); l(13.7f, 10.4f); l(14.1f, 12.8f); l(12f, 11.6f); l(9.9f, 12.8f); l(10.3f, 10.4f); l(8.6f, 8.7f); l(11f, 8.4f); z() }
        }
        IconId.ATRAS -> {
            p.line(m, 2.1f) { m(14.4f, 5.4f); l(7.8f, 12f); l(14.4f, 18.6f) }
        }
        IconId.FLECHA_ABAJO -> { p.line(m, 2.1f) { m(5.4f, 9.6f); l(12f, 16.2f); l(18.6f, 9.6f) } }
        IconId.PAUSA -> {
            p.fill(m) { rrect(6.6f, 4.4f, 3.6f, 15.2f, 1.2f) }
            p.fill(m) { rrect(13.8f, 4.4f, 3.6f, 15.2f, 1.2f) }
        }
        IconId.MANDO -> {
            p.line(m, 1.5f) { m(8.4f, 7.4f); l(15.6f, 7.4f); c(19.6f, 7.4f, 21.6f, 11f, 20.6f, 15.4f); c(20f, 18.4f, 17.4f, 19.4f, 15.6f, 17f); l(14.2f, 15.2f); l(9.8f, 15.2f); l(8.4f, 17f); c(6.6f, 19.4f, 4f, 18.4f, 3.4f, 15.4f); c(2.4f, 11f, 4.4f, 7.4f, 8.4f, 7.4f); z() }
            p.seg(7.6f, 11.4f, 10.4f, 11.4f, m, 1.5f)
            p.seg(9f, 10f, 9f, 12.8f, m, 1.5f)
            p.dot(15.4f, 10.4f, 1.15f, a)
            p.dot(17.4f, 12.4f, 1.15f, a)
        }
        IconId.ALTAVOZ -> {
            p.fill(m) { m(3.4f, 9.4f); l(7.4f, 9.4f); l(12.4f, 4.6f); l(12.4f, 19.4f); l(7.4f, 14.6f); l(3.4f, 14.6f); z() }
            p.line(a, 1.5f) { m(15.4f, 9f); c(17.2f, 10.8f, 17.2f, 13.2f, 15.4f, 15f) }
            p.line(a, 1.4f) { m(18.2f, 6.6f); c(21.2f, 9.6f, 21.2f, 14.4f, 18.2f, 17.4f) }
        }
        IconId.NOTA_MUSICAL -> {
            p.line(m, 1.7f) { m(9.4f, 18f); l(9.4f, 4.6f); l(19.4f, 2.6f); l(19.4f, 15.4f) }
            p.seg(9.4f, 8.4f, 19.4f, 6.4f, m, 1.7f)
            p.fill(a) { oval(6.8f, 18f, 2.9f, 2.4f) }
            p.fill(a) { oval(16.8f, 15.4f, 2.9f, 2.4f) }
        }
        IconId.VIBRACION -> {
            p.line(m, 1.5f) { rrect(8.6f, 4.4f, 6.8f, 15.2f, 1.6f) }
            p.seg(10.4f, 17.4f, 13.6f, 17.4f, m, 1.2f)
            p.line(a, 1.5f) { m(5.4f, 9f); l(3.4f, 12f); l(5.4f, 15f) }
            p.line(a, 1.5f) { m(18.6f, 9f); l(20.6f, 12f); l(18.6f, 15f) }
        }
        IconId.INFO -> {
            p.ring(12f, 12f, 9.2f, m, 1.6f)
            p.dot(12f, 7.4f, 1.15f, a)
            p.line(m, 1.9f) { m(12f, 10.6f); l(12f, 17f) }
        }
        IconId.CANDADO -> {
            p.line(m, 1.6f) { m(8f, 10.4f); l(8f, 7.4f); c(8f, 3.4f, 16f, 3.4f, 16f, 7.4f); l(16f, 10.4f) }
            p.fill(dim) { rrect(5.4f, 10.4f, 13.2f, 10.6f, 2.2f) }
            p.line(m, 1.5f) { rrect(5.4f, 10.4f, 13.2f, 10.6f, 2.2f) }
            p.dot(12f, 14.6f, 1.5f, a)
            p.seg(12f, 15.8f, 12f, 18f, a, 1.5f)
        }
        IconId.TILDE -> { p.line(m, 2.3f) { m(4.6f, 12.6f); l(9.8f, 17.8f); l(19.4f, 6.4f) } }
        IconId.CERRAR -> {
            p.line(m, 2.1f) { m(6.4f, 6.4f); l(17.6f, 17.6f) }
            p.line(m, 2.1f) { m(17.6f, 6.4f); l(6.4f, 17.6f) }
        }
        IconId.MAS -> {
            p.line(m, 2.2f) { m(12f, 5.4f); l(12f, 18.6f) }
            p.line(m, 2.2f) { m(5.4f, 12f); l(18.6f, 12f) }
        }
        IconId.TROFEO -> {
            p.fill(a) { m(7.6f, 3.4f); l(16.4f, 3.4f); l(16.4f, 9.6f); c(16.4f, 12.6f, 14.4f, 14.6f, 12f, 14.6f); c(9.6f, 14.6f, 7.6f, 12.6f, 7.6f, 9.6f); z() }
            p.line(m, 1.4f) { m(7.6f, 5f); l(4.2f, 5f); l(4.2f, 7.6f); c(4.2f, 9.8f, 5.8f, 10.8f, 7.6f, 10.8f) }
            p.line(m, 1.4f) { m(16.4f, 5f); l(19.8f, 5f); l(19.8f, 7.6f); c(19.8f, 9.8f, 18.2f, 10.8f, 16.4f, 10.8f) }
            p.seg(12f, 14.6f, 12f, 18.2f, m, 1.7f)
            p.seg(8.4f, 20.8f, 15.6f, 20.8f, m, 1.9f)
        }
        IconId.CRONOMETRO -> {
            p.ring(12f, 13.4f, 8.2f, m, 1.6f)
            p.seg(9.6f, 2.6f, 14.4f, 2.6f, m, 1.7f)
            p.seg(12f, 2.6f, 12f, 5.2f, m, 1.7f)
            p.line(a, 1.7f) { m(12f, 13.4f); l(12f, 8.6f) }
            p.line(a, 1.5f) { m(12f, 13.4f); l(15.6f, 15.4f) }
            p.dot(12f, 13.4f, 1f, m)
        }
        IconId.VIDA -> {
            p.fill(Color(0xFFE05A50)) { m(12f, 20.6f); c(5.2f, 15.6f, 3.4f, 12f, 3.4f, 9.2f); c(3.4f, 6.4f, 5.6f, 4.4f, 8.2f, 4.4f); c(9.9f, 4.4f, 11.3f, 5.4f, 12f, 6.8f); c(12.7f, 5.4f, 14.1f, 4.4f, 15.8f, 4.4f); c(18.4f, 4.4f, 20.6f, 6.4f, 20.6f, 9.2f); c(20.6f, 12f, 18.8f, 15.6f, 12f, 20.6f); z() }
        }
        IconId.AGUANTE -> {
            p.fill(Color(0xFFF0C24A)) { m(13.4f, 1.8f); l(5.4f, 13.4f); l(11f, 13.4f); l(9.6f, 22.2f); l(18.6f, 9.6f); l(12.6f, 9.6f); z() }
        }
        IconId.SALIDA -> {
            p.line(m, 1.6f) { m(13.4f, 3.6f); l(4.6f, 3.6f); l(4.6f, 20.4f); l(13.4f, 20.4f) }
            p.line(a, 1.9f) { m(10.4f, 12f); l(20.4f, 12f) }
            p.line(a, 1.9f) { m(16.6f, 8.2f); l(20.4f, 12f); l(16.6f, 15.8f) }
        }
        IconId.REINICIAR -> {
            p.line(m, 1.8f) { m(19.4f, 12f); c(19.4f, 16.6f, 15.4f, 20.2f, 11f, 19.9f); c(6.6f, 19.6f, 3.4f, 15.8f, 4f, 11.4f); c(4.6f, 7f, 8.6f, 3.9f, 13f, 4.4f); c(15.4f, 4.7f, 17.4f, 6f, 18.6f, 7.8f) }
            p.fill(a) { m(20.6f, 3f); l(20.2f, 9.4f); l(14.2f, 7.6f); z() }
        }
        IconId.CASA -> {
            p.line(m, 1.6f) { m(3.4f, 11.4f); l(12f, 3.6f); l(20.6f, 11.4f) }
            p.line(m, 1.6f) { m(5.6f, 10.4f); l(5.6f, 20.4f); l(18.4f, 20.4f); l(18.4f, 10.4f) }
            p.fill(a) { rrect(9.6f, 13.6f, 4.8f, 6.8f, 0.8f) }
        }
        IconId.ESTADISTICA -> {
            p.line(m, 1.6f) { m(3.6f, 3.6f); l(3.6f, 20.4f); l(20.4f, 20.4f) }
            p.fill(dim) { rect(6.6f, 12.4f, 3.2f, 5.4f) }
            p.fill(a) { rect(11.4f, 8.4f, 3.2f, 9.4f) }
            p.fill(dim) { rect(16.2f, 5.4f, 3.2f, 12.4f) }
        }
        IconId.PANTALLA -> {
            p.line(m, 1.5f) { rrect(2.6f, 4.4f, 18.8f, 12.8f, 1.8f) }
            p.seg(8.4f, 20.4f, 15.6f, 20.4f, m, 1.6f)
            p.seg(12f, 17.2f, 12f, 20.4f, m, 1.5f)
            p.fill(a.copy(alpha = 0.4f)) { rect(4.8f, 6.6f, 14.4f, 8.4f) }
        }
        IconId.IDIOMA -> {
            p.ring(12f, 12f, 9.2f, m, 1.5f)
            p.seg(2.8f, 12f, 21.2f, 12f, m, 1.3f)
            p.line(m, 1.3f) { m(12f, 2.8f); c(16.4f, 7.4f, 16.4f, 16.6f, 12f, 21.2f); c(7.6f, 16.6f, 7.6f, 7.4f, 12f, 2.8f); z() }
            p.seg(4.6f, 7.4f, 19.4f, 7.4f, dim, 1.1f)
            p.seg(4.6f, 16.6f, 19.4f, 16.6f, dim, 1.1f)
        }
        IconId.RESET -> {
            p.line(m, 1.6f) { m(6.4f, 7.4f); l(17.6f, 7.4f); l(16.6f, 20.4f); l(7.4f, 20.4f); z() }
            p.seg(4.4f, 7.4f, 19.6f, 7.4f, m, 1.7f)
            p.line(m, 1.5f) { m(9.6f, 7.4f); l(9.6f, 4.6f); l(14.4f, 4.6f); l(14.4f, 7.4f) }
            p.seg(10.4f, 11f, 10.4f, 17f, a, 1.4f)
            p.seg(13.6f, 11f, 13.6f, 17f, a, 1.4f)
        }
        IconId.DEDO -> {
            p.fill(dim) { m(9.4f, 21.4f); c(6.6f, 19.4f, 5.4f, 16.4f, 5.4f, 13.4f); l(5.4f, 11.4f); c(5.4f, 10.2f, 7f, 10.2f, 7.2f, 11.4f); l(7.4f, 12.6f); l(8.6f, 12.6f); l(8.6f, 4.4f); c(8.6f, 2.8f, 11f, 2.8f, 11f, 4.4f); l(11f, 11.4f); l(12.2f, 11.4f); l(12.4f, 9.4f); c(12.5f, 8.2f, 14.5f, 8.4f, 14.4f, 9.6f); l(14.6f, 11.6f); l(15.8f, 11.6f); l(16.2f, 10.4f); c(16.5f, 9.2f, 18.4f, 9.6f, 18.2f, 10.8f); l(17.6f, 15.4f); c(17.2f, 18.4f, 16.4f, 20.2f, 15.4f, 21.4f); z() }
            p.line(m, 1.4f) { m(9.4f, 21.4f); c(6.6f, 19.4f, 5.4f, 16.4f, 5.4f, 13.4f); l(5.4f, 11.4f); c(5.4f, 10.2f, 7f, 10.2f, 7.2f, 11.4f); l(7.4f, 12.6f); l(8.6f, 12.6f); l(8.6f, 4.4f); c(8.6f, 2.8f, 11f, 2.8f, 11f, 4.4f); l(11f, 11.4f); l(12.2f, 11.4f); l(12.4f, 9.4f); c(12.5f, 8.2f, 14.5f, 8.4f, 14.4f, 9.6f); l(14.6f, 11.6f); l(15.8f, 11.6f); l(16.2f, 10.4f); c(16.5f, 9.2f, 18.4f, 9.6f, 18.2f, 10.8f); l(17.6f, 15.4f); c(17.2f, 18.4f, 16.4f, 20.2f, 15.4f, 21.4f); z() }
        }
        IconId.EQUIPAR -> {
            p.line(m, 1.5f) { m(12f, 2.6f); l(20.4f, 6.4f); l(20.4f, 12.4f); c(20.4f, 17.4f, 16.6f, 20.4f, 12f, 21.6f); c(7.4f, 20.4f, 3.6f, 17.4f, 3.6f, 12.4f); l(3.6f, 6.4f); z() }
            p.line(a, 2f) { m(8.4f, 12.4f); l(11f, 15f); l(15.8f, 9f) }
        }
        IconId.CORRER -> {
            p.dot(15.4f, 4.6f, 2.15f, m)
            p.line(m, 1.9f) { m(13.4f, 10.4f); l(9.4f, 13.4f); l(11.4f, 17.4f); l(9.4f, 21.4f) }
            p.line(m, 1.9f) { m(13.4f, 10.4f); l(16.4f, 14.4f); l(20.4f, 15f) }
            p.line(m, 1.9f) { m(13.4f, 10.4f); l(8.4f, 8.4f); l(5.4f, 10.4f) }
            p.seg(2.6f, 14.4f, 6.4f, 14.4f, a, 1.4f)
            p.seg(3.6f, 18f, 7f, 18f, a, 1.2f)
        }
        IconId.DIFICULTAD -> {
            p.fill(dim) { m(2.6f, 20.4f); l(8f, 20.4f); l(8f, 14.4f); l(2.6f, 14.4f); z() }
            p.fill(a.copy(alpha = 0.7f)) { m(9.4f, 20.4f); l(14.6f, 20.4f); l(14.6f, 9.4f); l(9.4f, 9.4f); z() }
            p.fill(a) { m(16f, 20.4f); l(21.4f, 20.4f); l(21.4f, 4.4f); l(16f, 4.4f); z() }
            p.line(m, 1.2f) { m(2.6f, 20.4f); l(8f, 20.4f); l(8f, 14.4f); l(2.6f, 14.4f); z() }
            p.line(m, 1.2f) { m(9.4f, 20.4f); l(14.6f, 20.4f); l(14.6f, 9.4f); l(9.4f, 9.4f); z() }
            p.line(m, 1.2f) { m(16f, 20.4f); l(21.4f, 20.4f); l(21.4f, 4.4f); l(16f, 4.4f); z() }
        }
        IconId.AGACHARSE -> {
            // Silueta agachada bajo un techo bajo.
            p.seg(3.4f, 5.4f, 20.6f, 5.4f, a, 1.8f)
            p.seg(5.4f, 5.4f, 5.4f, 7.4f, a.copy(alpha = 0.5f), 1.2f)
            p.seg(18.6f, 5.4f, 18.6f, 7.4f, a.copy(alpha = 0.5f), 1.2f)
            p.dot(9.6f, 10.6f, 2.1f, m)
            p.line(m, 1.9f) { m(9.6f, 12.9f); l(13.4f, 14.6f); l(16.4f, 12.4f) }
            p.line(m, 1.9f) { m(13.4f, 14.6f); l(12.4f, 18.4f); l(16f, 20.6f) }
            p.line(m, 1.9f) { m(12.4f, 18.4f); l(7.4f, 19.4f); l(6.4f, 20.6f) }
        }
        IconId.ARRASTRARSE -> {
            // Silueta tirada en el piso pasando por una gatera.
            p.seg(3.4f, 7.4f, 20.6f, 7.4f, a, 1.8f)
            p.seg(3.4f, 20.6f, 20.6f, 20.6f, m.copy(alpha = 0.55f), 1.5f)
            p.dot(7.4f, 15.4f, 2f, m)
            p.line(m, 1.9f) { m(9.2f, 16.4f); l(14.4f, 17.4f); l(19.4f, 16.2f) }
            p.line(m, 1.7f) { m(11.4f, 17f); l(11.4f, 19.6f) }
            p.line(m, 1.7f) { m(16.4f, 17f); l(17.4f, 19.6f) }
            p.line(m, 1.7f) { m(9f, 14.4f); l(12.4f, 12.6f) }
        }
        IconId.SALTAR -> {
            // Silueta en el aire con una flecha de impulso.
            p.dot(13.4f, 5.6f, 2.15f, m)
            p.line(m, 1.9f) { m(13.4f, 8.2f); l(11.4f, 13.4f); l(14.6f, 16.4f) }
            p.line(m, 1.9f) { m(11.4f, 13.4f); l(7.4f, 15.4f) }
            p.line(m, 1.9f) { m(13.4f, 9.6f); l(17.6f, 11.4f) }
            p.line(m, 1.9f) { m(14.6f, 16.4f); l(13.4f, 19.6f) }
            p.line(a, 1.6f) { m(4.4f, 20.6f); l(4.4f, 14.4f) }
            p.fill(a) { m(4.4f, 11.6f); l(6.8f, 15.4f); l(2f, 15.4f); z() }
        }
        IconId.DE_PIE -> {
            // Silueta erguida con una flecha de levantarse.
            p.dot(13.4f, 4.8f, 2.15f, m)
            p.line(m, 1.9f) { m(13.4f, 7.4f); l(13.4f, 14.4f) }
            p.line(m, 1.9f) { m(13.4f, 9.4f); l(9.4f, 11.6f) }
            p.line(m, 1.9f) { m(13.4f, 9.4f); l(17.6f, 11.6f) }
            p.line(m, 1.9f) { m(13.4f, 14.4f); l(11f, 20.6f) }
            p.line(m, 1.9f) { m(13.4f, 14.4f); l(16.4f, 20.6f) }
            p.line(a, 1.6f) { m(4.4f, 20.6f); l(4.4f, 9.4f) }
            p.fill(a) { m(4.4f, 6.4f); l(6.8f, 10.4f); l(2f, 10.4f); z() }
        }
        IconId.MULTIJUGADOR -> {
            // Dos siluetas, una adelante de la otra.
            p.dot(8.4f, 8.4f, 3.1f, m)
            p.fill(m) { m(2.6f, 20.4f); c(2.6f, 15.4f, 6f, 13.6f, 8.4f, 13.6f); c(10.8f, 13.6f, 14.2f, 15.4f, 14.2f, 20.4f); z() }
            p.dot(16.4f, 7.4f, 2.5f, a)
            p.fill(a.copy(alpha = 0.85f)) { m(11.4f, 20.4f); c(11.4f, 16.2f, 14f, 14.6f, 16.4f, 14.6f); c(18.8f, 14.6f, 21.4f, 16.2f, 21.4f, 20.4f); z() }
        }
        IconId.RELOJ_ARENA -> {
            // Reloj de arena: lo que todavia no llego.
            p.line(m, 1.6f) { m(6.4f, 3.6f); l(17.6f, 3.6f) }
            p.line(m, 1.6f) { m(6.4f, 20.4f); l(17.6f, 20.4f) }
            p.line(m, 1.5f) { m(7.6f, 3.6f); l(7.6f, 7.4f); l(12f, 12f); l(7.6f, 16.6f); l(7.6f, 20.4f) }
            p.line(m, 1.5f) { m(16.4f, 3.6f); l(16.4f, 7.4f); l(12f, 12f); l(16.4f, 16.6f); l(16.4f, 20.4f) }
            p.fill(a) { m(9.2f, 5.4f); l(14.8f, 5.4f); l(12f, 9.6f); z() }
            p.fill(a) { m(9.6f, 18.6f); l(14.4f, 18.6f); l(12f, 15.4f); z() }
        }
        else -> {
            // Marcador visible si alguna vez faltara un icono: nunca deberia verse.
            p.ring(12f, 12f, 8.4f, m, 1.5f)
            p.seg(12f, 7f, 12f, 13f, m, 1.8f)
            p.dot(12f, 16.6f, 1.1f, m)
        }
    }
}
