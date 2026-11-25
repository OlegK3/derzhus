package ru.derzhus.app

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Экран «Достижения»: уровни, XP, награды за дни и за финансовый прогресс. */
@Composable
fun AchievementsScreen(data: AppData, now: Long) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val xp = Game.xp(data, now)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        ScreenTitle("Достижения")

        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dim.PagePadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Pill("Мои достижения", tab == 0, { tab = 0 }, Modifier.weight(1f))
            Pill("Уровни", tab == 1, { tab = 1 }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))

        if (tab == 0) MyAwards(data, now, xp) else LevelsTab(xp)
    }
}

@Composable
private fun MyAwards(data: AppData, now: Long, xp: Int) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        MountainBackdrop(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(22.dp)))
        LevelCard(xp, Modifier.align(Alignment.BottomCenter).padding(10.dp))
    }

    SectionTitle("Ключевые достижения")
    AwardGrid(Game.dayAwards, data)

    SectionTitle("Финансовый прогресс")
    AwardGrid(Game.moneyAwards + Game.goalAwards, data)

    SectionTitle("Игровая статистика")
    val record = data.habits.maxOfOrNull { Calc.recordDays(it, now) } ?: 0L
    val earned = Game.awards.count { it.id in data.unlocked }
    AppCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        StatLine("Всего XP", "$xp")
        StatLine("Лучшая серия", Calc.daysText(record))
        StatLine("Открыто наград", "$earned из ${Game.awards.size}")
        StatLine("Достигнуто целей", "${data.goals.count { it.achievedAt != null }}")
        StatLine("Реально отложено", Money.rub(data.totalSavedKop))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp)
    )
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AwardGrid(list: List<Award>, data: AppData) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        list.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { a ->
                    AwardTile(a, a.id in data.unlocked, Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AwardTile(a: Award, open: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Dim.CardRadius)
    Box(
        modifier
            .shadow(if (open) 2.dp else 0.dp, shape)
            .clip(shape)
            .background(if (open) Surface1 else Surface2)
            .border(1.dp, Border, shape)
            .padding(vertical = 12.dp, horizontal = 6.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            AppIcon(a.icon, tint = if (open) Accent else Muted.copy(alpha = 0.55f), size = 40.dp)
            Text(
                a.title, color = if (open) TextMain else Muted, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(a.desc, color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
        Box(
            Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape)
                .background(if (open) Accent else Border),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                if (open) R.drawable.ic_check else R.drawable.ic_lock,
                tint = if (open) OnAccent else Muted, size = 12.dp,
                contentDescription = if (open) "Получено" else "Закрыто"
            )
        }
    }
}

@Composable
private fun LevelsTab(xp: Int) {
    val current = Levels.forXp(xp)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LevelCard(xp, Modifier.fillMaxWidth())

        Levels.all.forEach { l ->
            val reached = xp >= l.fromXp
            val isCurrent = l.n == current.n
            AppCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.alpha(if (reached) 1f else 0.4f)) { LevelBadge(l.n, Modifier.size(44.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Уровень ${l.n} · ${l.name}",
                            color = if (reached) TextMain else Muted,
                            fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                        )
                        Text("от ${l.fromXp} XP", color = Muted, fontSize = 13.sp)
                    }
                    Text(
                        if (isCurrent) "Сейчас" else if (reached) "Получен" else "Закрыт",
                        color = if (reached) Accent else Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        AppCard(Modifier.fillMaxWidth()) {
            Text("Как начисляется XP", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "5 XP за каждый день вашего рекорда, бонус за каждую награду (один раз) и 50 XP за каждую достигнутую мечту. " +
                    "XP не отнимается: срыв не уменьшает уровень, а повторное открытие приложения ничего не начисляет заново.",
                color = Muted, fontSize = 14.sp, lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
