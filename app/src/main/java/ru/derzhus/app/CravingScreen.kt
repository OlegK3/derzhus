package ru.derzhus.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val CYCLES = 5

/** Дыхание 4 секунды вдох, 6 секунд выдох, 5 раз (около 50 секунд). */
@Composable
fun CravingScreen(habit: Habit, now: Long, onClose: () -> Unit) {
    val scale = remember { Animatable(0.55f) }
    var phase by remember { mutableStateOf("Приготовься") }
    var cycle by remember { mutableIntStateOf(0) }
    var run by remember { mutableIntStateOf(0) }
    val done = cycle >= CYCLES

    LaunchedEffect(run) {
        cycle = 0
        scale.snapTo(0.55f)
        repeat(CYCLES) { i ->
            phase = "Вдох"
            scale.animateTo(1f, tween(4000, easing = FastOutSlowInEasing))
            phase = "Выдох"
            scale.animateTo(0.55f, tween(6000, easing = FastOutSlowInEasing))
            cycle = i + 1
        }
        phase = "Ты справился"
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Тянет — это нормально", color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Дыши вместе с кругом. Обычно тяга стихает за несколько минут.",
            color = Muted, fontSize = 15.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(280.dp)) {
                val r = size.minDimension / 2
                drawCircle(Accent.copy(alpha = 0.12f), radius = r)
                drawCircle(Accent.copy(alpha = 0.55f), radius = r * scale.value)
            }
            Text(phase, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        }

        Text(
            if (done) "Ты держишься уже ${Calc.duration(Calc.elapsed(habit, now))}. Это стоит сохранить."
            else Praise.support(cycle),
            color = TextMain, fontSize = 17.sp, textAlign = TextAlign.Center, lineHeight = 24.sp
        )
        if (habit.pricePerUnit > 0) {
            Text(
                "Уже сэкономлено ${Calc.money(Calc.saved(habit, now))}",
                color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp)
            )
        }

        Row(
            Modifier.padding(top = 20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = { run++ }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Ещё раз") }
            Button(onClick = onClose, modifier = Modifier.weight(1f).height(52.dp)) { Text("Мне лучше") }
        }
    }
}
