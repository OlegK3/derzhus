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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
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

/** Пейзаж из дизайн-комплекта с переходом в фон страницы. */
@Composable
fun MountainBackdrop(modifier: Modifier = Modifier) {
    PhotoBackdrop(Photos.sunrise, modifier, fadeTop = false, fadeBottom = true)
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

/** Кольцо с количеством дней поверх пейзажа. Под текстом светлая подложка для читаемости. */
@Composable
fun DaysRing(days: Long, label: String, progress: Float, modifier: Modifier = Modifier) {
    val anim by animateFloatAsState(progress, tween(900, easing = FastOutSlowInEasing), label = "ring")
    val digits = days.toString().length
    val numberSize = when {
        digits <= 2 -> 64.sp
        digits == 3 -> 52.sp
        else -> 40.sp
    }
    Box(
        modifier.size(220.dp).semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val tl = Offset(inset, inset)
            val sz = Size(size.width - stroke, size.height - stroke)
            drawCircle(Surface1.copy(alpha = 0.84f))
            drawArc(
                color = Color.White.copy(alpha = 0.8f), startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = tl, size = sz, style = Stroke(stroke)
            )
            drawArc(
                color = Accent, startAngle = -90f, sweepAngle = 360f * anim, useCenter = false,
                topLeft = tl, size = sz, style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$days", color = TextMain, fontSize = numberSize, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                label, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.width(150.dp)
            )
            AppIcon(R.drawable.ic_mountain, tint = Accent, size = 30.dp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Поздравление с наградой: светлая подложка, конфетти, карточка. */
@Composable
fun CelebrationOverlay(award: Award, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xF2F7F5EF))) {
        Confetti(Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.Center).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(104.dp).clip(CircleShape).background(AccentSoft),
                contentAlignment = Alignment.Center
            ) { AppIcon(R.drawable.ic_achievements, tint = AccentDeep, size = 56.dp) }
            Text("Новая награда!", color = Accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
            Text(
                award.title, color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
            )
            Text(award.desc, color = Muted, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
            Text("+${award.xp} XP", color = AccentDeep, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            PrimaryButton("Ура!", onClose, Modifier.padding(top = 28.dp))
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
        val colors = listOf(Accent, AccentLight, Gold, Color(0xFF5BA8E8), Color(0xFFE8738F))
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
