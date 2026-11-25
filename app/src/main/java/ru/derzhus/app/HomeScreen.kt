package ru.derzhus.app

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** «5 дней», «1 250 дней»: число с пробелом-разделителем и правильным словом. */
fun daysLabel(n: Long): String = "${Money.group(n)} ${Calc.dayWord(n)}"

/** Главный экран: уровень, кольцо дней на фоне пейзажа, деньги, главная мечта. */
@Composable
fun HomeScreen(
    data: AppData,
    now: Long,
    onSelectHabit: (String) -> Unit,
    onAddHabit: () -> Unit,
    onOpenGoal: (String) -> Unit,
    onAddGoal: () -> Unit,
    onCraving: () -> Unit,
    onRelapse: (Habit) -> Unit,
    onOpenAwards: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val habit = data.mainHabit
    val xp = Game.xp(data, now)
    var askRelapse by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        // Шапка: логотип, название, напоминания
        Row(
            Modifier.fillMaxWidth().padding(start = Dim.PagePadding, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(R.drawable.ic_mountain, tint = Accent, size = 30.dp)
            Spacer(Modifier.width(8.dp))
            Text("Новая жизнь", color = AccentDeep, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenProfile) {
                AppIcon(R.drawable.ic_bell, tint = TextMain, contentDescription = "Напоминания")
            }
        }

        // Уровень и XP
        LevelCard(xp, Modifier.padding(horizontal = Dim.PagePadding, vertical = 6.dp), onClick = onOpenAwards)

        if (data.habits.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Dim.PagePadding),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                items(data.habits, key = { it.id }) { h ->
                    Pill(h.title, selected = h.id == habit?.id, onClick = { onSelectHabit(h.id) })
                }
            }
        }

        if (habit != null) {
            val daysD = Calc.days(habit, now)
            val days = Calc.fullDays(habit, now)
            val next = Game.dayAwards.firstOrNull { it.threshold > daysD }
            val prev = Game.dayAwards.lastOrNull { it.threshold <= daysD }?.threshold ?: 0L
            val progress = if (next == null) 1f
            else ((daysD - prev) / (next.threshold - prev)).toFloat().coerceIn(0f, 1f)

            // Кольцо дней поверх пейзажа
            Box(Modifier.fillMaxWidth().height(340.dp)) {
                PhotoBackdrop(Photos.sunrise, Modifier.fillMaxSize(), fadeTop = true, fadeBottom = true)
                Box(Modifier.align(Alignment.Center).padding(bottom = 28.dp)) {
                    DaysRing(days, "${Calc.dayWord(days)} без ${habit.name}", progress)
                }
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(AccentSoft)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(Praise.tag(habit, now), color = AccentDeep, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                Praise.daily(habit, now),
                color = Muted, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
            )

            // Расчётная экономия и реальные накопления: два независимых показателя
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Dim.PagePadding),
                horizontalArrangement = Arrangement.spacedBy(Dim.CardGap)
            ) {
                if (habit.perDayKop > 0) {
                    MetricCard(
                        R.drawable.ic_coins, "Сэкономлено", Money.rub(Calc.savedKop(habit, now)),
                        "предполагаемо · ≈ ${Money.rub(habit.perDayKop)} / день", Modifier.weight(1f)
                    )
                } else {
                    MetricCard(
                        R.drawable.ic_coins, "Обошёлся без",
                        "${Calc.count(Calc.avoided(habit, now))} ${habit.unitLabel}", null, Modifier.weight(1f)
                    )
                }
                MetricCard(R.drawable.ic_wallet, "Реально отложено", Money.rub(data.totalSavedKop), "по вашим пополнениям", Modifier.weight(1f))
            }

            Spacer(Modifier.height(Dim.CardGap))
            DreamCard(data, onOpenGoal, onAddGoal, Modifier.padding(horizontal = Dim.PagePadding))

            // Ближайшая награда по дням
            if (next != null) {
                val left = (next.threshold * Calc.DAY) - Calc.elapsed(habit, now)
                AppCard(
                    Modifier.fillMaxWidth().padding(horizontal = Dim.PagePadding).padding(top = Dim.CardGap),
                    onClick = onOpenAwards, padding = 14.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(R.drawable.ic_achievements, size = 44.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ближайшая награда", color = Muted, fontSize = 12.sp)
                            Text(
                                "${next.threshold} ${Calc.dayWord(next.threshold)} без ${habit.name}",
                                color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Осталось " + daysLabel(Calc.ceilDiv(left, Calc.DAY)),
                                color = Muted, fontSize = 13.sp
                            )
                        }
                        AppIcon(R.drawable.ic_chevron_right, tint = Muted)
                    }
                }
            }

            // Поддержка
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Dim.PagePadding).padding(top = Dim.CardGap),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton("Тянет? Нужна помощь", onClick = onCraving, modifier = Modifier.weight(1f))
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = { askRelapse = true }) { Text("Сорвался", color = Muted) }
            }
        } else {
            // Первая привычка ещё не добавлена
            PhotoBackdrop(Photos.sunrise, Modifier.fillMaxWidth().height(220.dp), fadeTop = true, fadeBottom = true)
            AppCard(Modifier.fillMaxWidth().padding(horizontal = Dim.PagePadding)) {
                Text("Начни новую жизнь", style = AppType.Section)
                Text(
                    "Добавь привычку, от которой отказываешься: алкоголь, сигареты, сладкое или своё. " +
                        "Приложение посчитает дни и расчётную экономию.",
                    style = AppType.BodyMuted, lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                )
                PrimaryButton("Добавить привычку", onClick = onAddHabit, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(Dim.CardGap))
            DreamCard(data, onOpenGoal, onAddGoal, Modifier.padding(horizontal = Dim.PagePadding))
        }
        Spacer(Modifier.height(24.dp))
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

/** Метрика: значок, подпись, крупное значение, пояснение. */
@Composable
private fun MetricCard(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    note: String?,
    modifier: Modifier = Modifier
) {
    AppCard(modifier, padding = 14.dp) {
        IconBadge(icon, size = 38.dp)
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        Text(
            value,
            color = TextMain,
            fontSize = if (value.length > 10) 20.sp else 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        if (note != null) Text(note, color = Muted, fontSize = 12.sp, maxLines = 2)
    }
}

/** Карточка «Главная мечта»: фото, название и цена на светлой подложке, прогресс и прогноз. */
@Composable
private fun DreamCard(
    data: AppData,
    onOpenGoal: (String) -> Unit,
    onAddGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val goal = data.mainGoal
    if (goal == null) {
        AppCard(modifier.fillMaxWidth(), onClick = onAddGoal) {
            Text("Главная мечта", style = AppType.Section)
            Text(
                "Добавь цель, на которую будешь копить: мотоцикл, поездка, палатка.",
                style = AppType.BodyMuted, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            PrimaryButton("Добавить мечту", onClick = onAddGoal, modifier = Modifier.fillMaxWidth(), leading = "+")
        }
        return
    }
    val daysLeft = Calc.daysToGoal(goal, Calc.paceKop(data))
    AppCard(modifier.fillMaxWidth(), onClick = { onOpenGoal(goal.id) }, padding = 0.dp) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Главная мечта", style = AppType.Section, modifier = Modifier.weight(1f))
            AppIcon(R.drawable.ic_chevron_right, tint = Muted)
        }
        Box(Modifier.fillMaxWidth().height(132.dp)) {
            GoalImage(goal.photos.firstOrNull()?.let { photoFile(ctx, it) }, Modifier.fillMaxSize())
            // Светлая подложка слева: текст не лежит на пёстрой фотографии
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to Surface1.copy(alpha = 0.95f),
                        0.55f to Surface1.copy(alpha = 0.6f),
                        1f to Color.Transparent
                    )
                )
            )
            Column(Modifier.align(Alignment.CenterStart).padding(start = 16.dp).fillMaxWidth(0.62f)) {
                Text(
                    goal.title.uppercase(), color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(Money.rub(goal.costKop), color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
            AppProgress(goal.progress)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${Money.rub(goal.savedKop)} (${goal.percent}%)", color = TextMain, fontSize = 13.sp)
                Text(
                    when {
                        goal.achieved -> "Достигнута!"
                        daysLeft != null -> "Осталось " + daysLabel(daysLeft)
                        else -> "Прогноз не задан"
                    },
                    color = if (goal.achieved) Accent else Muted,
                    fontSize = 13.sp,
                    fontWeight = if (goal.achieved) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
