package ru.derzhus.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Главный экран «Новая глава». */
@Composable
fun HomeScreen(
    data: AppData,
    now: Long,
    onSelectHabit: (String) -> Unit,
    onAddHabit: () -> Unit,
    onOpenGoal: (String) -> Unit,
    onAddGoal: () -> Unit,
    onCraving: () -> Unit,
    onRules: () -> Unit,
    onRelapse: (Habit) -> Unit,
    onOpenAwards: () -> Unit,
    onOpenGoals: () -> Unit
) {
    val ctx = LocalContext.current
    val habit = data.mainHabit
    val xp = Game.xp(data, now)
    var askRelapse by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        // Верх: горы, заголовок, карточка уровня
        Box(Modifier.fillMaxWidth()) {
            MountainBackdrop(Modifier.fillMaxWidth().height(200.dp))
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PeakIcon(Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Новая глава", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                LevelCard(xp, onClick = onOpenAwards)
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            if (data.habits.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    items(data.habits, key = { it.id }) { h ->
                        Pill(h.title, selected = h.id == habit?.id, onClick = { onSelectHabit(h.id) })
                    }
                }
            }

            if (habit != null) {
                val days = Calc.fullDays(habit, now)
                val daysD = Calc.days(habit, now)
                val next = Game.dayAwards.firstOrNull { it.threshold > daysD }
                val prev = Game.dayAwards.lastOrNull { it.threshold <= daysD }?.threshold ?: 0L
                val progress = if (next == null) 1f
                else ((daysD - prev) / (next.threshold - prev)).toFloat().coerceIn(0f, 1f)

                // Кольцо дней
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DaysRing(days, "${Calc.dayWord(days)} без ${habit.name}", progress)
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentSoft)
                            .border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .padding(horizontal = 18.dp, vertical = 7.dp)
                    ) {
                        Text(Praise.tag(habit, now), color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Text(
                    Praise.daily(habit, now),
                    color = Muted, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp)
                )

                // Расчётная экономия и реальные накопления: независимые показатели
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (habit.perDayKop > 0) {
                        StatTile("💳", "Сэкономлено", Money.rub(Calc.savedKop(habit, now)), "(предполагаемо)", Modifier.weight(1f))
                    } else {
                        StatTile("✨", "Обошёлся без", "${Calc.count(Calc.avoided(habit, now))} ${habit.unitLabel}", null, Modifier.weight(1f))
                    }
                    StatTile("🐷", "Реально отложено", Money.rub(data.totalSavedKop), null, Modifier.weight(1f))
                }

                Spacer(Modifier.height(14.dp))
                DreamCard(data, now, onOpenGoal, onAddGoal, onOpenGoals)

                // Ближайшая цель по дням
                if (next != null) {
                    Spacer(Modifier.height(12.dp))
                    val left = (next.threshold * Calc.DAY) - Calc.elapsed(habit, now)
                    AppCard(Modifier.fillMaxWidth(), onClick = onOpenAwards, padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(next.icon, fontSize = 34.sp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Ближайшая цель", color = Muted, fontSize = 12.sp)
                                Text(
                                    "${next.threshold} ${Calc.dayWord(next.threshold)} без ${habit.name}",
                                    color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Осталось " + Calc.daysText(Calc.ceilDiv(left, Calc.DAY)),
                                    color = Muted, fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Поддержка
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton("Тянет? Помощь", onClick = onCraving, modifier = Modifier.weight(1f))
                    GhostButton("Три правила", onClick = onRules, modifier = Modifier.weight(1f))
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { askRelapse = true }) { Text("Сорвался", color = Muted) }
                }
            } else {
                // Первая привычка ещё не добавлена
                Spacer(Modifier.height(24.dp))
                AppCard(Modifier.fillMaxWidth()) {
                    Text("Начни новую главу", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Добавь привычку, от которой отказываешься: алкоголь, сигареты, сладкое или своё. " +
                            "Приложение посчитает дни и расчётную экономию.",
                        color = Muted, fontSize = 15.sp, lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                    )
                    PrimaryButton("Добавить привычку", onClick = onAddHabit, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(12.dp))
                DreamCard(data, now, onOpenGoal, onAddGoal, onOpenGoals)
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (askRelapse && habit != null) {
        AlertDialog(
            onDismissRequest = { askRelapse = false },
            containerColor = Surface1,
            title = { Text("Сорвался?") },
            text = { Text(Praise.relapse(Calc.recordDays(habit, now))) },
            confirmButton = {
                TextButton(onClick = {
                    askRelapse = false
                    onRelapse(habit)
                }) { Text("Начать новую серию") }
            },
            dismissButton = {
                TextButton(onClick = { askRelapse = false }) { Text("Продолжить счёт", color = Muted) }
            }
        )
    }
}

@Composable
private fun StatTile(icon: String, label: String, value: String, note: String?, modifier: Modifier = Modifier) {
    AppCard(modifier, padding = 14.dp) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(AccentSoft),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 20.sp) }
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        Text(value, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (note != null) Text(note, color = Muted, fontSize = 12.sp)
    }
}

/** Карточка «Главная мечта» с фотографией, процентом и прогнозом. */
@Composable
private fun DreamCard(
    data: AppData,
    now: Long,
    onOpenGoal: (String) -> Unit,
    onAddGoal: () -> Unit,
    onOpenGoals: () -> Unit
) {
    val ctx = LocalContext.current
    val goal = data.mainGoal
    if (goal == null) {
        AppCard(Modifier.fillMaxWidth(), onClick = onAddGoal) {
            Text("Главная мечта", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Добавь цель, на которую будешь копить: мотоцикл, поездка, палатка.",
                color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            PrimaryButton("Добавить мечту", onClick = onAddGoal, modifier = Modifier.fillMaxWidth(), leading = "+")
        }
        return
    }
    val daysLeft = Calc.daysToGoal(goal, Calc.paceKop(data))
    AppCard(Modifier.fillMaxWidth(), onClick = { onOpenGoal(goal.id) }, padding = 14.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Главная мечта", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("›", color = Muted, fontSize = 22.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoalImage(
                goal.photos.firstOrNull()?.let { photoFile(ctx, it) },
                Modifier.size(width = 96.dp, height = 84.dp).clip(RoundedCornerShape(14.dp)),
                emojiSize = 30.sp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.title, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(Money.rub(goal.costKop), color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                AppProgress(goal.progress)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${Money.rub(goal.savedKop)} (${goal.percent}%)", color = Muted, fontSize = 12.sp)
                    Text(
                        when {
                            goal.achieved -> "Достигнута!"
                            daysLeft != null -> "Осталось " + Calc.daysText(daysLeft)
                            else -> ""
                        },
                        color = if (goal.achieved) Accent else Muted, fontSize = 12.sp
                    )
                }
            }
        }
    }
}
