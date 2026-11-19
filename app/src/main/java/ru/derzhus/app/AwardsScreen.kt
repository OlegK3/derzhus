package ru.derzhus.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AwardsScreen(habit: Habit, now: Long, onBack: () -> Unit) {
    val unlocked = Awards.unlocked(habit, now).map { it.id }.toSet()
    val moneyGoals = if (habit.pricePerUnit > 0) Awards.moneyGoals else emptyList()

    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        TextButton(onClick = onBack) { Text("← Назад", color = Muted) }
        Text("Награды", color = Accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "Открыто ${unlocked.size} из ${Awards.dayGoals.size + moneyGoals.size} · ${habit.title}",
            color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        LazyColumn {
            item { SectionTitle("За дни") }
            items(Awards.dayGoals) { AwardRow(it, habit, now, it.id in unlocked) }
            if (moneyGoals.isNotEmpty()) {
                item { SectionTitle("За сэкономленное") }
                items(moneyGoals) { AwardRow(it, habit, now, it.id in unlocked) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
}

@Composable
private fun AwardRow(a: Award, habit: Habit, now: Long, open: Boolean) {
    Row(
        Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (open) Color1 else Surface1)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(a.emoji, fontSize = 32.sp, modifier = Modifier.alpha(if (open) 1f else 0.3f))
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(
                a.title, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = if (open) TextMain else Muted
            )
            Text(
                if (open) a.desc else Awards.remainingText(a, habit, now),
                fontSize = 13.sp, color = Muted
            )
        }
        if (open) Text("✓", color = Accent, fontSize = 20.sp, modifier = Modifier.width(24.dp))
    }
}

private val Color1 = androidx.compose.ui.graphics.Color(0xFF1F3D38)
