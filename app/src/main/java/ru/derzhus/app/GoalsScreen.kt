package ru.derzhus.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Экран «Цели и мечты». */
@Composable
fun GoalsScreen(
    data: AppData,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onMakeMain: (String) -> Unit
) {
    var filter by rememberSaveable { mutableStateOf(0) }
    var toDelete by remember { mutableStateOf<Goal?>(null) }
    val pace = Calc.paceKop(data)
    val ordered = data.goals.sortedWith(compareByDescending<Goal> { it.isMain }.thenByDescending { it.createdAt })
    val shown = when (filter) {
        1 -> ordered.filter { it.isMain }
        2 -> ordered.filter { it.achieved }
        else -> ordered
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Цели и мечты") {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Accent).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Add, contentDescription = "Добавить цель", tint = OnAccent) }
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Все цели (${data.goals.size})", filter == 0, { filter = 0 })
            Pill("Главная", filter == 1, { filter = 1 })
            Pill("Достигнутые", filter == 2, { filter = 2 })
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (shown.isEmpty()) {
                item {
                    AppCard(Modifier.fillMaxWidth()) {
                        Text(
                            when (filter) {
                                1 -> "Главная цель не выбрана"
                                2 -> "Достигнутых целей пока нет"
                                else -> "Целей пока нет"
                            },
                            color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Добавь мечту, например мотоцикл, поездку или снаряжение, и откладывай на неё реальные деньги.",
                            color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        PrimaryButton("Новая цель", onClick = onAdd, modifier = Modifier.fillMaxWidth(), leading = "+")
                    }
                }
            }
            items(shown, key = { it.id }) { g ->
                GoalCard(
                    g, Calc.daysToGoal(g, pace),
                    onOpen = { onOpen(g.id) },
                    onEdit = { onEdit(g.id) },
                    onDelete = { toDelete = g },
                    onMakeMain = { onMakeMain(g.id) }
                )
            }
        }
    }

    val del = toDelete
    if (del != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = Surface1,
            title = { Text("Удалить цель?") },
            text = { Text("«${del.title}», её фотографии и история накоплений будут удалены.") },
            confirmButton = {
                TextButton(onClick = { toDelete = null; onDelete(del.id) }) { Text("Удалить", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun GoalCard(
    g: Goal,
    daysLeft: Long?,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMakeMain: () -> Unit
) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    AppCard(Modifier.fillMaxWidth(), onClick = onOpen, padding = 0.dp) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            GoalImage(g.photos.firstOrNull()?.let { photoFile(ctx, it) }, Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
            )
            if (g.isMain) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xCC16301F))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("★ Главная цель", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
            }
            Box(Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Меню", tint = Color.White)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Редактировать") }, onClick = { menu = false; onEdit() })
                    if (!g.isMain) {
                        DropdownMenuItem(text = { Text("Сделать главной") }, onClick = { menu = false; onMakeMain() })
                    }
                    DropdownMenuItem(text = { Text("Удалить", color = Danger) }, onClick = { menu = false; onDelete() })
                }
            }
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(g.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(Money.rub(g.costKop), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            if (g.achieved) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(12.dp).size(32.dp).clip(CircleShape).background(Accent),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Check, contentDescription = null, tint = OnAccent) }
            }
        }
        Column(Modifier.padding(14.dp)) {
            AppProgress(g.progress)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${Money.rub(g.savedKop)} (${g.percent}%)", color = Muted, fontSize = 13.sp)
                Text(
                    when {
                        g.achieved -> "Достигнута!"
                        daysLeft != null -> "Осталось " + Calc.daysText(daysLeft)
                        else -> ""
                    },
                    color = if (g.achieved) Accent else Muted, fontSize = 13.sp
                )
            }
        }
    }
}
