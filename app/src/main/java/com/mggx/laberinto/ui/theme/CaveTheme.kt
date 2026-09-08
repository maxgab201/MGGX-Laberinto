package com.mggx.laberinto.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Paleta de la cueva: negros calidos, piedra y ambar de antorcha. */
object Cave {
    val Void = Color(0xFF070605)
    val Deep = Color(0xFF0D0B09)
    val Stone = Color(0xFF16130F)
    val StoneHi = Color(0xFF211C16)
    val StoneEdge = Color(0xFF3A3128)
    val Moss = Color(0xFF4E6B4A)

    val Amber = Color(0xFFFFA24B)
    val AmberSoft = Color(0xFFFFC58A)
    val AmberDeep = Color(0xFFC46A1E)
    val Ember = Color(0xFFFF6A2E)

    val Ice = Color(0xFF7FC7E8)
    val Crystal = Color(0xFF9EE7FF)
    val Vetagris = Color(0xFFCBD5E0)

    val Text = Color(0xFFE9E0D2)
    val TextDim = Color(0xFF9C9184)
    val TextFaint = Color(0xFF6B6257)

    val Good = Color(0xFF7BD98C)
    val Warn = Color(0xFFE8B44A)
    val Bad = Color(0xFFE0655A)

    val Health = Color(0xFFD9524A)
    val Stamina = Color(0xFFE8C24A)
}

private val CaveColors = darkColorScheme(
    primary = Cave.Amber,
    onPrimary = Cave.Void,
    primaryContainer = Cave.AmberDeep,
    onPrimaryContainer = Cave.Text,
    secondary = Cave.Ice,
    onSecondary = Cave.Void,
    background = Cave.Deep,
    onBackground = Cave.Text,
    surface = Cave.Stone,
    onSurface = Cave.Text,
    surfaceVariant = Cave.StoneHi,
    onSurfaceVariant = Cave.TextDim,
    outline = Cave.StoneEdge,
    error = Cave.Bad
)

private val CaveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Black,
        fontSize = 44.sp, letterSpacing = 3.sp, color = Cave.Text
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 27.sp, letterSpacing = 1.4.sp, color = Cave.Text
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 21.sp, letterSpacing = 1.1.sp, color = Cave.Text
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, letterSpacing = 0.7.sp, color = Cave.Text
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, letterSpacing = 0.5.sp, color = Cave.Text
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, color = Cave.Text
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp, color = Cave.TextDim
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 13.sp, letterSpacing = 1.2.sp, color = Cave.Text
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, letterSpacing = 0.9.sp, color = Cave.TextDim
    )
)

/**
 * [textScale] agranda o achica todos los textos sin tocar el resto del diseno:
 * se multiplica el fontScale de la densidad, no la densidad entera.
 */
@Composable
fun MggxTheme(textScale: Float = 1f, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val density = Density(base.density, base.fontScale * textScale.coerceIn(0.7f, 1.6f))
    CompositionLocalProvider(LocalDensity provides density) {
        MaterialTheme(
            colorScheme = CaveColors,
            typography = CaveTypography,
            content = content
        )
    }
}
