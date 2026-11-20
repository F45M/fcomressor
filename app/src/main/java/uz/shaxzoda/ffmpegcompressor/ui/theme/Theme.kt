package uz.shaxzoda.ffmpegcompressor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class FCompressorTheme(val label: String) {
    LEMON_COOKIE("Лимонное печенье"),
    SMOKY_CHERRY("Вишня в дыму"),
    DEAD_FLAMINGO("Дохлый фламинго"),
    MINT_LATTE("Мятный латте"),
    LIGHT("Светлая"),
    PICKME_DARK("Темная Пикми")
}

private val LemonCookieColors = lightColorScheme(
    primary = Color(0xFF9A7A63),
    onPrimary = Color(0xFFF8F6F1),
    primaryContainer = Color(0xFFF1E6AD),
    onPrimaryContainer = Color(0xFF6D5E52),
    secondary = Color(0xFF9A7A63),
    onSecondary = Color(0xFFF8F6F1),
    secondaryContainer = Color(0xFFE8DED0),
    onSecondaryContainer = Color(0xFF6D5E52),
    background = Color(0xFFE8DED0),
    onBackground = Color(0xFF6D5E52),
    surface = Color(0xFFF8F6F1),
    onSurface = Color(0xFF6D5E52),
    surfaceVariant = Color(0xFFF1E6AD),
    onSurfaceVariant = Color(0xFF776A5F),
    error = Color(0xFF9A4A3D),
    onError = Color(0xFFF8F6F1),
    outline = Color(0xFFB6A798),
)

private val SmokyCherryColors = lightColorScheme(
    primary = Color(0xFF52000D),
    onPrimary = Color(0xFFF2F7FA),
    primaryContainer = Color(0xFF9FC4DC),
    onPrimaryContainer = Color(0xFF52000D),
    secondary = Color(0xFF8FB6D0),
    onSecondary = Color(0xFF52000D),
    secondaryContainer = Color(0xFFC2D7E7),
    onSecondaryContainer = Color(0xFF52000D),
    background = Color(0xFF9FC4DC),
    onBackground = Color(0xFF52000D),
    surface = Color(0xFFC2D7E7),
    onSurface = Color(0xFF52000D),
    surfaceVariant = Color(0xFFD6E6F1),
    onSurfaceVariant = Color(0xFF4B6575),
    error = Color(0xFF9B1C2D),
    onError = Color(0xFFF2F7FA),
    outline = Color(0xFF789BB1),
)

private val DeadFlamingoColors = lightColorScheme(
    primary = Color(0xFF4A3F4B),
    onPrimary = Color(0xFFF7EEF3),
    primaryContainer = Color(0xFFC5A7B8),
    onPrimaryContainer = Color(0xFF3F3640),
    secondary = Color(0xFF4A3F4B),
    onSecondary = Color(0xFFF7EEF3),
    secondaryContainer = Color(0xFFE7CEDB),
    onSecondaryContainer = Color(0xFF4A3F4B),
    background = Color(0xFFE8CEDD),
    onBackground = Color(0xFF4A3F4B),
    surface = Color(0xFFF7EEF3),
    onSurface = Color(0xFF4A3F4B),
    surfaceVariant = Color(0xFFD8BCCA),
    onSurfaceVariant = Color(0xFF6A606B),
    error = Color(0xFF9A3B55),
    onError = Color(0xFFF7EEF3),
    outline = Color(0xFFAA8F9F),
)

private val MintLatteColors = lightColorScheme(
    primary = Color(0xFF3B0616),
    onPrimary = Color(0xFFF5F0E8),
    primaryContainer = Color(0xFF9DB5C7),
    onPrimaryContainer = Color(0xFF3B0616),
    secondary = Color(0xFF8FA9BD),
    onSecondary = Color(0xFF3B0616),
    secondaryContainer = Color(0xFFD8CEBF),
    onSecondaryContainer = Color(0xFF3B0616),
    background = Color(0xFFD8CEBF),
    onBackground = Color(0xFF3B0616),
    surface = Color(0xFFEDE7DC),
    onSurface = Color(0xFF3B0616),
    surfaceVariant = Color(0xFF9DB5C7),
    onSurfaceVariant = Color(0xFF5E5A53),
    error = Color(0xFF8F2E3D),
    onError = Color(0xFFF5F0E8),
    outline = Color(0xFF9B9388),
)

private val PurpleLightColors = lightColorScheme(
    primary = Color(0xFF6A35D9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF24005A),
    secondary = Color(0xFF7B4CE2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E8FF),
    onSecondaryContainer = Color(0xFF2B145F),
    background = Color.White,
    onBackground = Color(0xFF211733),
    surface = Color(0xFFF8F4FF),
    onSurface = Color(0xFF211733),
    surfaceVariant = Color(0xFFE7D9FF),
    onSurfaceVariant = Color(0xFF5E4D78),
    error = Color(0xFFB3261E),
    onError = Color.White,
    outline = Color(0xFF8C7BA8),
)

private val PickmeDarkColors = darkColorScheme(
    primary = Color(0xFFFF7AB8),
    onPrimary = Color(0xFF24000F),
    primaryContainer = Color(0xFF4A1530),
    onPrimaryContainer = Color(0xFFFFD7E8),
    secondary = Color(0xFFFF9CCA),
    onSecondary = Color(0xFF24000F),
    secondaryContainer = Color(0xFF32101F),
    onSecondaryContainer = Color(0xFFFFD7E8),
    background = Color.Black,
    onBackground = Color(0xFFFFD7E8),
    surface = Color(0xFF12080D),
    onSurface = Color(0xFFFFD7E8),
    surfaceVariant = Color(0xFF2A111B),
    onSurfaceVariant = Color(0xFFFFA9CF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFFFF7AB8),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(5),
    small = RoundedCornerShape(5),
    medium = RoundedCornerShape(5),
    large = RoundedCornerShape(5),
    extraLarge = RoundedCornerShape(5)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

@Composable
fun ShaxzodaTheme(theme: FCompressorTheme = FCompressorTheme.LEMON_COOKIE, content: @Composable () -> Unit) {
    val colors = when (theme) {
        FCompressorTheme.LEMON_COOKIE -> LemonCookieColors
        FCompressorTheme.SMOKY_CHERRY -> SmokyCherryColors
        FCompressorTheme.DEAD_FLAMINGO -> DeadFlamingoColors
        FCompressorTheme.MINT_LATTE -> MintLatteColors
        FCompressorTheme.LIGHT -> PurpleLightColors
        FCompressorTheme.PICKME_DARK -> PickmeDarkColors
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
}
