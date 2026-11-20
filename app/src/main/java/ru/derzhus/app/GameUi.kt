package ru.derzhus.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Атмосферные горы, нарисованные в коде (работает без интернета и без файлов-картинок). */
@Composable
fun MountainBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF1B3A31), Color(0xFF0E1A16), Bg)))
        // дальний хребет
        val far = Path().apply {
            moveTo(0f, h * 0.72f)
            lineTo(w * 0.16f, h * 0.48f)
            lineTo(w * 0.28f, h * 0.6f)
            lineTo(w * 0.52f, h * 0.2f)
            lineTo(w * 0.7f, h * 0.52f)
            lineTo(w * 0.84f, h * 0.36f)
            lineTo(w, h * 0.62f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(far, Color(0xFF244A3E))
        // снежная вершина
        val snow = Path().apply {
            moveTo(w * 0.52f, h * 0.2f)
            lineTo(w * 0.46f, h * 0.33f)
            lineTo(w * 0.5f, h * 0.31f)
            lineTo(w * 0.53f, h * 0.36f)
            lineTo(w * 0.56f, h * 0.31f)
            lineTo(w * 0.6f, h * 0.34f)
            close()
        }
        drawPath(snow, Color(0xFFCFE8DD))
        // ближний склон
        val near = Path().apply {
            moveTo(0f, h * 0.85f)
            lineTo(w * 0.22f, h * 0.62f)
            lineTo(w * 0.4f, h * 0.8f)
            lineTo(w * 0.62f, h * 0.66f)
            lineTo(w * 0.86f, h * 0.84f)
            lineTo(w, h * 0.74f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(near, Color(0xFF12241E))
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Bg), startY = h * 0.55f, endY = h))
    }
}

/** Маленькая зелёная вершина у заголовка. */
@Composable
fun PeakIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(0f, h * 0.9f)
            lineTo(w * 0.36f, h * 0.3f)
            lineTo(w * 0.52f, h * 0.55f)
            lineTo(w * 0.7f, h * 0.15f)
            lineTo(w, h * 0.9f)
            close()
        }
        drawPath(p, Brush.verticalGradient(listOf(AccentLight, AccentDeep)))
    }
}

@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier.size(52.dp)) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val p = Path().apply {
                moveTo(w * 0.5f, 0f)
                lineTo(w * 0.95f, h * 0.18f)
                lineTo(w * 0.95f, h * 0.58f)
                cubicTo(w * 0.95f, h * 0.8f, w * 0.7f, h * 0.93f, w * 0.5f, h)
                cubicTo(w * 0.3f, h * 0.93f, w * 0.05f, h * 0.8f, w * 0.05f, h * 0.58f)
                lineTo(w * 0.05f, h * 0.18f)
                close()
            }
            drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFD36B), Color(0xFFC98A12))))
            drawPath(p, Color(0xFFFFE9A8), style = Stroke(2.dp.toPx()))
        }
        Text("$level", color = Color(0xFF3A2A05), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
    }
}

/** Карточка уровня: «Уровень 4 · Уверенность · 340 / 500 XP». */
@Composable
fun LevelCard(xp: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val level = Levels.forXp(xp)
    val next = Levels.next(xp)
    AppCard(modifier.fillMaxWidth(), onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevelBadge(level.n)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Уровень ${level.n}", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(level.name, color = Muted, fontSize = 13.sp)
            }
            Text(
                if (next != null) "$xp / ${next.fromXp} XP" else "$xp XP",
                color = TextMain,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        AppProgress(Levels.bar(xp))
    }
}

/** Лавровые ветви по бокам кольца. */
@Composable
fun Laurel(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2 * 0.95f
        val lw = 7.dp.toPx()
        val lh = 17.dp.toPx()
        for (side in 0..1) {
            for (i in 0..6) {
                val phi = 105.0 + i * (130.0 / 6.0)
                val deg = if (side == 0) phi else 180.0 - phi
                val rad = Math.toRadians(deg)
                val px = center.x + (r * cos(rad)).toFloat()
                val py = center.y + (r * sin(rad)).toFloat()
                val tilt = if (side == 0) 28f else -28f
                val alpha = 0.55f + 0.45f * (i / 6f)
                rotate(degrees = (deg - 180.0).toFloat() + tilt, pivot = Offset(px, py)) {
                    drawOval(
                        color = Accent.copy(alpha = alpha),
                        topLeft = Offset(px - lw / 2, py - lh / 2),
                        size = Size(lw, lh)
                    )
                }
            }
        }
    }
}

/** Крупное кольцо с количеством дней (главный экран). */
@Composable
fun DaysRing(days: Long, label: String, progress: Float, modifier: Modifier = Modifier) {
    val anim by animateFloatAsState(progress, tween(900, easing = FastOutSlowInEasing), label = "ring")
    Box(modifier.size(250.dp), contentAlignment = Alignment.Center) {
        Laurel(Modifier.fillMaxSize())
        Canvas(Modifier.size(180.dp)) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val tl = Offset(inset, inset)
            val sz = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color(0xFF1C2622), startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = tl, size = sz, style = Stroke(stroke)
            )
            // мягкое свечение
            drawArc(
                color = Accent.copy(alpha = 0.22f), startAngle = -90f, sweepAngle = 360f * anim, useCenter = false,
                topLeft = tl, size = sz, style = Stroke(stroke * 1.9f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Accent, startAngle = -90f, sweepAngle = 360f * anim, useCenter = false,
                topLeft = tl, size = sz, style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$days", color = TextMain, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.width(120.dp))
        }
    }
}

/** Поздравление с наградой: затемнение, конфетти, карточка. */
@Composable
fun CelebrationOverlay(award: Award, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xE6000000))) {
        Confetti(Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.Center).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(award.icon, fontSize = 80.sp)
            Text("Новая награда!", color = Accent, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
            Text(
                award.title, color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
            )
            Text(award.desc, color = Muted, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
            Text("+${award.xp} XP", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            Button(onClick = onClose, modifier = Modifier.padding(top = 28.dp)) {
                Text("Ура!", fontSize = 17.sp)
            }
        }
    }
}

private class Piece(
    val x: Float, val y0: Float, val speed: Float, val size: Float,
    val color: Color, val freq: Float, val phase: Float
)

@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val pieces = remember {
        val r = kotlin.random.Random(7)
        val colors = listOf(Accent, Color(0xFFFBBF24), Color(0xFFF472B6), Color(0xFF60A5FA), Color(0xFFA78BFA))
        List(70) {
            Piece(
                x = r.nextFloat(), y0 = r.nextFloat(),
                speed = (1 + r.nextInt(2)).toFloat(),
                size = 8f + r.nextFloat() * 10f,
                color = colors[r.nextInt(colors.size)],
                freq = (1 + r.nextInt(3)).toFloat(),
                phase = r.nextFloat() * 6.28f
            )
        }
    }
    val t by rememberInfiniteTransition(label = "confetti").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "t"
    )
    Canvas(modifier) {
        pieces.forEach { p ->
            val y = ((p.y0 + t * p.speed) % 1f) * size.height
            val x = p.x * size.width + sin(t * 6.2832f * p.freq + p.phase) * 24f
            drawRect(p.color, Offset(x, y), Size(p.size, p.size * 1.6f))
        }
    }
}
