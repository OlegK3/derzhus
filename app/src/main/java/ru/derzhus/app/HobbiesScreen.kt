package ru.derzhus.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Экран «Увлечения»: занятия вместо привычки. Работает полностью офлайн. */
@Composable
fun HobbiesScreen(data: AppData, onToggle: (String) -> Unit) {
    var open by remember { mutableStateOf<Hobby?>(null) }
    val ideaOfDay = Hobbies.all[(System.currentTimeMillis() / 86_400_000L % Hobbies.all.size).toInt()]

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Увлечения")
        Text(
            "Идеи, на что потратить время и деньги вместо привычки",
            style = AppType.BodyMuted, modifier = Modifier.padding(horizontal = Dim.PagePadding)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            contentPadding = PaddingValues(Dim.PagePadding),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
            verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
            modifier = Modifier.fillMaxSize()
        ) {
            items(Hobbies.all, key = { it.id }) { h ->
                HobbyTile(h, h.id in data.settings.interests, Modifier) { open = h }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                AppCard(Modifier.fillMaxWidth(), onClick = { open = ideaOfDay }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(R.drawable.ic_leaf)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Идея на сегодня", style = AppType.Section)
                            Text("Чем заняться, если накатывает тяга?", style = AppType.Caption)
                        }
                        AppIcon(R.drawable.ic_chevron_right, tint = Muted)
                    }
                }
            }
        }
    }

    val hobby = open
    if (hobby != null) {
        val interested = hobby.id in data.settings.interests
        AppDialog({ open = null }, hobby.title) {
            HobbyPicture(hobby, Modifier.fillMaxWidth().height(160.dp), 18.dp)
            Text(hobby.about, color = TextMain, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 14.dp))
            Text("Идеи для начала", color = Accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            hobby.ideas.forEach { idea ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(Accent))
                    Spacer(Modifier.width(10.dp))
                    Text(idea, color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (interested) {
                GhostButton("В интересных, убрать", onClick = { onToggle(hobby.id) }, modifier = Modifier.fillMaxWidth())
            } else {
                PrimaryButton("Мне интересно", onClick = { onToggle(hobby.id) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Плитка занятия: фото (или значок на мягком зелёном) и подпись под ним. */
@Composable
private fun HobbyTile(h: Hobby, interested: Boolean, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier, onClick = onClick, padding = 6.dp) {
        Box {
            HobbyPicture(h, Modifier.fillMaxWidth().aspectRatio(0.85f), 14.dp)
            if (interested) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape).background(Accent),
                    contentAlignment = Alignment.Center
                ) { AppIcon(R.drawable.ic_check, tint = OnAccent, size = 14.dp, contentDescription = "В интересных") }
            }
        }
        Text(
            h.title, style = AppType.Body.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            textAlign = TextAlign.Center, maxLines = 2,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)
        )
    }
}

@Composable
private fun HobbyPicture(h: Hobby, modifier: Modifier, radius: androidx.compose.ui.unit.Dp) {
    val shape = RoundedCornerShape(radius)
    if (h.photo != null) {
        Image(
            painterResource(h.photo), contentDescription = h.title,
            contentScale = ContentScale.Crop, modifier = modifier.clip(shape)
        )
    } else {
        Box(modifier.clip(shape).background(AccentSoft), contentAlignment = Alignment.Center) {
            AppIcon(h.icon, tint = AccentDeep, size = 40.dp, contentDescription = h.title)
        }
    }
}
