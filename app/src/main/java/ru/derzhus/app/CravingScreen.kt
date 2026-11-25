package ru.derzhus.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
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

/**
 * «Тянет? Нужна помощь»: личная запись пользователя (или случайный совет с кнопкой «Другой совет»)
 * и дыхание 4 секунды вдох, 6 секунд выдох, 5 раз (около 50 секунд).
 */
@Composable
fun CravingScreen(habit: Habit, now: Long, distraction: String, onClose: () -> Unit) {
    val scale = remember { Animatable(0.55f) }
    var phase by remember { mutableStateOf("Приготовься") }
    var cycle by remember { mutableIntStateOf(0) }
    var run by remember { mutableIntStateOf(0) }
    var tip by rememberSaveable { mutableIntStateOf(Praise.nextTip(-1)) }
    val done = cycle >= CYCLES
    val own = distraction.trim()

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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth()) {
            PhotoBackdrop(Photos.support, Modifier.fillMaxWidth().height(170.dp), fadeTop = true, fadeBottom = true)
            BackHeader("Тянет? Нужна помощь", onClose)
        }
        Column(
            Modifier.padding(horizontal = Dim.PagePadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(R.drawable.ic_leaf, tint = Accent)
                    Text(
                        if (own.isNotEmpty()) "  Твой способ отвлечься" else "  Совет на сейчас",
                        style = AppType.Section
                    )
                }
                Text(
                    if (own.isNotEmpty()) own else Praise.tips[tip],
                    style = AppType.Body.copy(fontSize = 17.sp, lineHeight = 25.sp),
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            if (own.isEmpty()) {
                Spacer(Modifier.height(Dim.CardGap))
                GhostButton("Другой совет", onClick = { tip = Praise.nextTip(tip) }, modifier = Modifier.fillMaxWidth())
            }

            Text(
                "Дыши вместе с кругом. Обычно тяга стихает за несколько минут.",
                style = AppType.BodyMuted, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
            Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(200.dp)) {
                    val r = size.minDimension / 2
                    drawCircle(Accent.copy(alpha = 0.12f), radius = r)
                    drawCircle(Accent.copy(alpha = 0.55f), radius = r * scale.value)
                }
                Text(phase, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                if (done) "Ты держишься уже ${Calc.duration(Calc.elapsed(habit, now))}. Это стоит сохранить."
                else Praise.support(cycle),
                color = TextMain, fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 23.sp
            )
            if (habit.perDayKop > 0) {
                Text(
                    "Уже сэкономлено ${Money.rub(Calc.savedKop(habit, now))}",
                    style = AppType.BodyMuted, modifier = Modifier.padding(top = 8.dp)
                )
            }
            Row(
                Modifier.padding(top = 20.dp, bottom = Dim.PagePadding).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GhostButton("Дыхание ещё раз", onClick = { run++ }, modifier = Modifier.weight(1f))
                PrimaryButton("Понятно, справлюсь!", onClick = onClose, modifier = Modifier.weight(1f))
            }
        }
    }
}
