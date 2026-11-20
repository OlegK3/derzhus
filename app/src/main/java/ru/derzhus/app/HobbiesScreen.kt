package ru.derzhus.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Экран «Увлечения»: занятия вместо привычки. Работает полностью офлайн. */
@Composable
fun HobbiesScreen(data: AppData, onToggle: (String) -> Unit) {
    var group by rememberSaveable { mutableStateOf("Все") }
    var open by remember { mutableStateOf<Hobby?>(null) }
    val shown = if (group == "Все") Hobbies.all else Hobbies.all.filter { it.group == group }

    Column(Modifier.fillMaxSize()) {
        Text("Увлечения", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 14.dp))
        Text(
            "Идеи, на что потратить время и деньги вместо привычки",
            color = Muted, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(Hobbies.groups) { g -> Pill(g, g == group, { group = g }) }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(shown, key = { it.id }) { h ->
                AppCard(Modifier.fillMaxWidth(), onClick = { open = h }, padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HobbyThumb(h, Modifier.size(width = 84.dp, height = 72.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.title, color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text(h.blurb, color = Muted, fontSize = 13.sp, lineHeight = 17.sp)
                        }
                        Text(
                            if (h.id in data.settings.interests) "★" else "›",
                            color = if (h.id in data.settings.interests) Gold else Muted,
                            fontSize = 22.sp
                        )
                    }
                }
            }
        }
    }

    val hobby = open
    if (hobby != null) {
        val interested = hobby.id in data.settings.interests
        AppDialog({ open = null }, hobby.title) {
            HobbyThumb(hobby, Modifier.fillMaxWidth().height(140.dp), big = true)
            Text(hobby.about, color = TextMain, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 14.dp))
            Text("Идеи для начала", color = Accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            hobby.ideas.forEach { idea ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("•", color = Accent, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(idea, color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (interested) {
                GhostButton("★ В интересных, убрать", onClick = { onToggle(hobby.id) }, modifier = Modifier.fillMaxWidth())
            } else {
                PrimaryButton("Мне интересно", onClick = { onToggle(hobby.id) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Иллюстрация занятия: градиент и крупный значок (без загрузки из интернета). */
@Composable
private fun HobbyThumb(h: Hobby, modifier: Modifier, big: Boolean = false) {
    Box(
        modifier
            .clip(RoundedCornerShape(if (big) 18.dp else 14.dp))
            .background(Brush.linearGradient(listOf(Color(h.color1), Color(h.color2)))),
        contentAlignment = Alignment.Center
    ) {
        Text(h.emoji, fontSize = if (big) 64.sp else 34.sp)
    }
}
