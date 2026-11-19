package ru.derzhus.app

import android.content.Intent
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.sin

@Composable
fun HomeScreen(
    habits: List<Habit>,
    habit: Habit,
    now: Long,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onCraving: () -> Unit,
    onAwards: () -> Unit,
    onUpdate: (Habit) -> Unit,
    onDelete: (Habit) -> Unit
) {
    val ctx = LocalContext.current
    var askRelapse by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val elapsed = Calc.elapsed(habit, now)
    val daysD = Calc.days(habit, now)
    val fullDays = Calc.fullDays(habit, now)
    val unlocked = Awards.unlocked(habit, now)
    val fresh = unlocked.filter { it.id !in habit.seen }

    val goals = Awards.dayGoals
    val next = goals.firstOrNull { it.threshold > daysD }
    val prevT = goals.lastOrNull { it.threshold <= daysD }?.threshold ?: 0.0
    val progress = if (next == null) 1f
    else ((daysD - prevT) / (next.threshold - prevT)).toFloat().coerceIn(0f, 1f)
    val animProgress by animateFloatAsState(progress, tween(800, easing = FastOutSlowInEasing), label = "progress")

    val bestMs = max(habit.bestMillis, elapsed)
    val bestDays = bestMs / Calc.DAY

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Шапка
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Держусь",
                    color = Accent, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    TextButton(onClick = { menuOpen = true }) { Text("⋯", color = Muted, fontSize = 24.sp) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Удалить «${habit.title}»") },
                            onClick = { menuOpen = false; askDelete = true }
                        )
                    }
                }
            }

            // Переключатель целей
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                items(habits, key = { it.id }) { h ->
                    FilterChip(
                        selected = h.id == habit.id,
                        onClick = { onSelect(h.id) },
                        label = { Text(h.title) }
                    )
                }
                item { AssistChip(onClick = onAdd, label = { Text("+ Добавить") }) }
            }

            // Кольцо прогресса
            Box(
                Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 16.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = Surface2, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke)
                    )
                    drawArc(
                        color = Accent, startAngle = -90f, sweepAngle = 360f * animProgress, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(habit.title, color = Muted, fontSize = 15.sp)
                    Text("$fullDays", color = TextMain, fontSize = 84.sp, fontWeight = FontWeight.Light)
                    Text(Calc.dayWord(fullDays), color = Accent, fontSize = 20.sp)
                    Text(
                        Calc.duration(elapsed % Calc.DAY, withSeconds = true),
                        color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            // Следующая цель
            Text(
                text = if (next != null)
                    "Следующая цель: ${next.emoji} ${next.title}, осталось " +
                        Calc.duration((next.threshold * Calc.DAY).toLong() - elapsed)
                else "Ты прошёл все цели. Это легенда.",
                color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp)
            )

            // Статистика 2x2
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (habit.pricePerUnit > 0)
                    StatCard("Сэкономлено", Calc.money(Calc.saved(habit, now)), Modifier.weight(1f))
                else
                    StatCard("Награды", "${unlocked.size} из ${Awards.all.size}", Modifier.weight(1f))
                StatCard(
                    "Обошёлся без",
                    "${Calc.count(Calc.avoided(habit, now))} ${habit.unitLabel}",
                    Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Рекорд", Calc.daysText(bestDays), Modifier.weight(1f))
                StatCard("Уровень", Calc.level(fullDays).name, Modifier.weight(1f))
            }

            // Похвала
            Column(
                Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1F3D38))
                    .padding(16.dp)
            ) {
                Text(Praise.daily(habit, now), color = TextMain, fontSize = 16.sp, lineHeight = 22.sp)
            }

            // Кнопки
            Button(
                onClick = onCraving,
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(54.dp)
            ) { Text("Тянет? Помоги мне", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }

            Row(
                Modifier.padding(top = 10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = onAwards, modifier = Modifier.weight(1f)) {
                    Text("Награды ${unlocked.size}/${Awards.all.size}")
                }
                OutlinedButton(
                    onClick = {
                        val text = "Я держусь уже ${Calc.daysText(fullDays)}: ${habit.title.lowercase()}! " +
                            (if (habit.pricePerUnit > 0) "Сэкономил ${Calc.money(Calc.saved(habit, now))}. " else "") +
                            "Считаю в приложении «Держусь»."
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        ctx.startActivity(Intent.createChooser(send, "Поделиться"))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Поделиться") }
            }

            TextButton(
                onClick = { askRelapse = true },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp, bottom = 24.dp)
            ) { Text("Сорвался", color = Muted) }
        }

        if (fresh.isNotEmpty()) {
            CelebrationOverlay(fresh.last()) {
                onUpdate(habit.copy(seen = habit.seen + unlocked.map { it.id }))
            }
        }
    }

    if (askRelapse) {
        AlertDialog(
            onDismissRequest = { askRelapse = false },
            containerColor = Surface1,
            title = { Text("Сорвался?") },
            text = { Text(Praise.relapse(bestDays)) },
            confirmButton = {
                TextButton(onClick = {
                    askRelapse = false
                    onUpdate(habit.copy(startMillis = now, bestMillis = bestMs, seen = emptySet()))
                }) { Text("Начать новую серию") }
            },
            dismissButton = {
                TextButton(onClick = { askRelapse = false }) { Text("Продолжить счёт", color = Muted) }
            }
        )
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            containerColor = Surface1,
            title = { Text("Удалить цель?") },
            text = { Text("«${habit.title}» и вся статистика будут удалены.") },
            confirmButton = {
                TextButton(onClick = { askDelete = false; onDelete(habit) }) { Text("Удалить", color = Warn) }
            },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text("Отмена") } }
        )
    }
}

@Composable
fun CelebrationOverlay(award: Award, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
    ) {
        Confetti(Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.Center).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(award.emoji, fontSize = 80.sp)
            Text(
                "Новая награда!", color = Accent, fontSize = 16.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                award.title, color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
            )
            Text(award.desc, color = Muted, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
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
                speed = (1 + r.nextInt(2)).toFloat(),     // целые значения: движение без «скачков»
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
