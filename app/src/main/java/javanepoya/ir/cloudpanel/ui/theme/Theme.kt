package javanepoya.ir.cloudpanel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CfOrange,
    onPrimary = Color.White,
    primaryContainer = CfOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = CfOrangeLight,
    onSecondary = Color.Black,
    background = DeepObsidian,
    onBackground = TextPrimary,
    surface = DarkSlateCard,
    onSurface = TextPrimary,
    surfaceVariant = DarkSlateCardHover,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    error = AccentRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CfOrange,
    onPrimary = Color.White,
    primaryContainer = CfOrangeLight,
    onPrimaryContainer = Color.Black,
    secondary = CfOrangeDark,
    onSecondary = Color.White,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = AccentRed,
    onError = Color.White
)

@Composable
fun CfSwitcherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
