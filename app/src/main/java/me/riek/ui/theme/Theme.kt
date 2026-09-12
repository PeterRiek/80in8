package me.riek.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cyan = Color(0xFF00E5FF)
val DimCyan = Color(0xFF0A4A52)  // muted cyan for the timer bar
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val Surface = Color(0xFF121212)
val Faded = Color(0xFF4A4A4A)
val Wrong = Color(0xFFFF5252)
val Right = Cyan

// 80in8.com result-screen palette (Tailwind, dark variants)
val Emerald = Color(0xFF10B981)      // emerald-500
val EmeraldText = Color(0xFF34D399)  // emerald-400
val EmeraldBg = Color(0x1A10B981)    // emerald-500/10
val RedC = Color(0xFFEF4444)         // red-500
val RedText = Color(0xFFF87171)      // red-400
val RedBg = Color(0x1AEF4444)        // red-500/10
val Blue500 = Color(0xFF3B82F6)
val Sky500 = Color(0xFF0EA5E9)
val Lime = Color(0xFFA3E635)         // yellow-green (lime-400)
val Amber500 = Color(0xFFF59E0B)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc500 = Color(0xFF71717A)
val Zinc800 = Color(0xFF27272A)
val SkippedBg = Color(0x6627272A)    // zinc-800/40

private val Scheme = darkColorScheme(
    primary = Cyan,
    onPrimary = Black,
    background = Black,
    onBackground = White,
    surface = Surface,
    onSurface = White,
    surfaceVariant = Surface,
    onSurfaceVariant = White,
)

@Composable
fun EightyIn8Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
}
