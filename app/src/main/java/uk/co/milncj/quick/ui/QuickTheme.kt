package uk.co.milncj.quick.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val QuickBlue = Color(0xFF1557FF)
private val QuickBlueDark = Color(0xFF7CA2FF)

private val QuickLightColors = lightColorScheme(
    primary = QuickBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7EEFF),
    onPrimaryContainer = Color(0xFF082566),
    background = Color(0xFFFAF9F6),
    onBackground = Color(0xFF17191D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17191D),
    surfaceVariant = Color(0xFFF0F1F3),
    onSurfaceVariant = Color(0xFF666D77),
    outline = Color(0xFFD6DAE0),
    outlineVariant = Color(0xFFE8EAED)
)

private val QuickDarkColors = darkColorScheme(
    primary = QuickBlueDark,
    onPrimary = Color(0xFF001D52),
    primaryContainer = Color(0xFF123A91),
    onPrimaryContainer = Color(0xFFDCE6FF),
    background = Color(0xFF0D0F12),
    onBackground = Color(0xFFF4F5F7),
    surface = Color(0xFF15181D),
    onSurface = Color(0xFFF4F5F7),
    surfaceVariant = Color(0xFF20242A),
    onSurfaceVariant = Color(0xFFB7BDC7),
    outline = Color(0xFF3A4049),
    outlineVariant = Color(0xFF2A2F36)
)

private val QuickShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun QuickTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) QuickDarkColors else QuickLightColors,
        shapes = QuickShapes,
        content = content
    )
}
