package io.github.yohann69.sing.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Ember preset from sing-theme.json, mapped onto Material 3. */
object Ember {
    val accent = Color(0xFFF26B1D)
    val accentSoft = Color(0xFFFF9A57)
    val background = Color(0xFF0F0A08)
    val text = Color(0xFFFFF4EC)
    val blobs = listOf(Color(0xFFFF6A13), Color(0xFFC2264B), Color(0xFFFFB347), Color(0xFF5B1A7A))
}

private val scheme = darkColorScheme(
    primary = Ember.accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5A2A10),
    onPrimaryContainer = Ember.accentSoft,
    secondary = Ember.accentSoft,
    background = Ember.background,
    onBackground = Ember.text,
    surface = Color(0xFF16100D),
    onSurface = Ember.text,
    surfaceContainer = Color(0xFF221814),
    surfaceContainerHigh = Color(0xFF2B1F1A),
    surfaceContainerHighest = Color(0xFF352620),
    onSurfaceVariant = Color(0xFFD9C6BB),
    outlineVariant = Color(0xFF4A3830),
)

@Composable
fun SingTheme(content: @Composable () -> Unit) {
    val base = Typography()
    MaterialTheme(
        colorScheme = scheme,
        typography = base.copy(
            displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black),
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        ),
        content = content,
    )
}

val LyricStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)

fun hexColor(hex: String?): Color {
    if (hex == null) return Ember.accent
    val s = hex.removePrefix("#")
    val v = s.toLongOrNull(16) ?: return Ember.accent
    return if (s.length == 8) Color((v and 0xFF) shl 24 or (v shr 8)) else Color(0xFF000000 or v)
}

/** Slowly drifting blurred blobs in the artwork colours, like the TV. */
@Composable
fun BlobBackground(palette: List<String>?, modifier: Modifier = Modifier) {
    val colors = palette?.takeIf { it.size >= 3 }?.map(::hexColor) ?: Ember.blobs
    val t by rememberInfiniteTransition(label = "blobs").animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(30_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    val c0 by animateColorAsState(colors[0], tween(1500), label = "c0")
    val c1 by animateColorAsState(colors[1], tween(1500), label = "c1")
    val c2 by animateColorAsState(colors[2], tween(1500), label = "c2")
    Box(modifier.fillMaxSize().background(Ember.background)) {
        Canvas(Modifier.fillMaxSize().blur(90.dp)) {
            val u = max(size.width, size.height)
            fun blob(c: Color, x: Float, y: Float, r: Float) =
                drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.85f), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
            blob(c0, size.width * (0.15f + 0.15f * sin(t)), size.height * (0.15f + 0.1f * cos(t)), u * 0.6f)
            blob(c1, size.width * (0.85f + 0.1f * cos(t * 1.3f)), size.height * (0.3f + 0.15f * sin(t * 0.8f)), u * 0.55f)
            blob(c2, size.width * (0.4f + 0.2f * sin(t * 0.7f)), size.height * (0.9f + 0.08f * cos(t)), u * 0.6f)
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)))
    }
}
