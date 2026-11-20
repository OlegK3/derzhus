package ru.derzhus.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Экран отдельной цели. */
@Composable
fun GoalDetailScreen(vm: MainVm, goalId: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val data = vm.data
    val goal = data.goals.firstOrNull { it.id == goalId }
    if (goal == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableIntStateOf(0) }
    var showSavings by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var askDeleteGoal by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<String?>(null) }
    var showAllHistory by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val names = vm.importPhotos(uris)
                vm.updateGoal(goal.id) { it.copy(photos = it.photos + names) }
            }
        }
    }
    val pickPhotos = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    val pace = Calc.paceKop(data)
    val daysLeft = Calc.daysToGoal(goal, pace)
    val perDay = data.mainHabit?.perDayKop ?: 0L
    val bigIndex = selected.coerceIn(0, maxOf(0, goal.photos.size - 1))

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Большая фотография
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            GoalImage(goal.photos.getOrNull(bigIndex)?.let { photoFile(ctx, it) }, Modifier.fillMaxSize(), 64.sp)
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent, Bg)))
            )
            Box(
                Modifier.align(Alignment.TopStart).padding(12.dp).size(40.dp).clip(CircleShape)
                    .background(Color(0x99000000)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Text("←", color = Color.White, fontSize = 20.sp) }
            Row(Modifier.align(Alignment.TopEnd).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color(0x99000000)).clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Edit, contentDescription = "Редактировать", tint = Color.White) }
                Box {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color(0x99000000)).clickable { menu = true },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.MoreVert, contentDescription = "Меню", tint = Color.White) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (!goal.isMain) {
                            DropdownMenuItem(
                                text = { Text("Сделать главной") },
                                onClick = { menu = false; vm.makeMainGoal(goal.id) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Удалить цель", color = Danger) },
                            onClick = { menu = false; askDeleteGoal = true }
                        )
                    }
                }
            }
        }

        // Галерея
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            item {
                Column(
                    Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Border, RoundedCornerShape(12.dp))
                        .background(Surface1).clickable(onClick = pickPhotos),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("+", color = TextMain, fontSize = 20.sp)
                    Text("Добавить\nфото", color = Muted, fontSize = 8.sp, lineHeight = 10.sp)
                }
            }
            itemsIndexed(goal.photos) { i, name ->
                Box(Modifier.size(64.dp)) {
                    GoalImage(
                        photoFile(ctx, name),
                        Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                            .border(if (i == bigIndex) 2.dp else 1.dp, if (i == bigIndex) Accent else Border, RoundedCornerShape(12.dp))
                            .clickable { selected = i },
                        20.sp
                    )
                    Box(
                        Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape)
                            .background(Color(0xCC000000)).clickable { photoToDelete = name },
                        contentAlignment = Alignment.Center
                    ) { Text("✕", color = Color.White, fontSize = 10.sp) }
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            if (goal.isMain) {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(AccentSoft).padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("★ Главная цель", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
            }
            Text(goal.title, color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            Text(Money.rub(goal.costKop), color = Accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            AppProgress(goal.progress, height = 10.dp)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${Money.rub(goal.savedKop)} из ${Money.rub(goal.costKop)}", color = Accent, fontSize = 14.sp)
                Text("${goal.percent}%", color = TextMain, fontSize = 14.sp)
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppCard(Modifier.weight(1f), padding = 14.dp) {
                    Text("До цели", color = Muted, fontSize = 13.sp)
                    Text(
                        when {
                            goal.achieved -> "Достигнута!"
                            daysLeft != null -> Calc.daysText(daysLeft)
                            else -> "—"
                        },
                        color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                    if (!goal.achieved && daysLeft == null) {
                        Text("темп не задан", color = Muted, fontSize = 11.sp)
                    }
                }
                AppCard(Modifier.weight(1f), padding = 14.dp) {
                    Text("Осталось", color = Muted, fontSize = 13.sp)
                    Text(Money.rub(goal.remainingKop), color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Расчётная экономия: отдельно от реальных накоплений
            Spacer(Modifier.height(12.dp))
            AppCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                Text("Расчётная экономия", color = Muted, fontSize = 12.sp)
                SavingRow("Экономия в день", if (perDay > 0) Money.rub(perDay) else "—")
                SavingRow("Экономия в неделю", if (perDay > 0) Money.rub(perDay * 7) else "—")
                SavingRow("Экономия в месяц", if (perDay > 0) Money.rub(perDay * 30) else "—")
                Text(
                    "Это предположение по привычке, а не накопленные деньги.",
                    color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton("Добавить накопление", onClick = { showSavings = true }, modifier = Modifier.fillMaxWidth(), leading = "+")

            // История пополнений и корректировок
            if (goal.history.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("История накоплений", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val items = goal.history.sortedByDescending { it.ts }
                val shown = if (showAllHistory) items else items.take(5)
                AppCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                    shown.forEach { h ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    when (h.kind) {
                                        "adjust" -> "Корректировка"
                                        "initial" -> "Начальная сумма"
                                        else -> "Пополнение"
                                    },
                                    color = TextMain, fontSize = 14.sp
                                )
                                val sub = formatDateTime(h.ts) + if (h.comment.isNotBlank() && h.kind == "deposit") " · " + h.comment else ""
                                Text(sub, color = Muted, fontSize = 12.sp)
                            }
                            Text(
                                Money.signed(h.amountKop),
                                color = if (h.amountKop >= 0) Accent else Danger,
                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (items.size > 5) {
                        TextButton(onClick = { showAllHistory = !showAllHistory }) {
                            Text(if (showAllHistory) "Свернуть" else "Показать все (${items.size})", color = Accent)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSavings) {
        SavingsDialog(
            goal = goal,
            onDismiss = { showSavings = false },
            onDeposit = { kop, comment -> vm.addDeposit(goal.id, kop, comment); showSavings = false },
            onChangeGoal = { title, cost, saved -> vm.changeGoal(goal.id, title, cost, saved); showSavings = false }
        )
    }

    if (askDeleteGoal) {
        AlertDialog(
            onDismissRequest = { askDeleteGoal = false },
            containerColor = Surface1,
            title = { Text("Удалить цель?") },
            text = { Text("«${goal.title}», её фотографии и история накоплений будут удалены.") },
            confirmButton = {
                TextButton(onClick = { askDeleteGoal = false; vm.deleteGoal(goal.id) }) { Text("Удалить", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { askDeleteGoal = false }) { Text("Отмена") } }
        )
    }

    val delPhoto = photoToDelete
    if (delPhoto != null) {
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            containerColor = Surface1,
            title = { Text("Удалить фото?") },
            confirmButton = {
                TextButton(onClick = {
                    photoToDelete = null
                    vm.deletePhotoFiles(listOf(delPhoto))
                    vm.updateGoal(goal.id) { it.copy(photos = it.photos - delPhoto) }
                }) { Text("Удалить", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { photoToDelete = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun SavingRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMain, fontSize = 15.sp)
        Text(value, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/** Нижняя панель «Добавить накопление»: пополнение или изменение цели. */
@Composable
fun SavingsDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onDeposit: (Long, String) -> Unit,
    onChangeGoal: (String, Long, Long) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var title by remember { mutableStateOf(goal.title) }
    var cost by remember { mutableStateOf(Money.edit(goal.costKop)) }
    var saved by remember { mutableStateOf(Money.edit(goal.savedKop)) }

    AppDialog(onDismiss, "Добавить накопление") {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Пополнение", tab == 0, { tab = 0 }, Modifier.weight(1f))
            Pill("Изменить цель", tab == 1, { tab = 1 }, Modifier.weight(1f))
        }
        if (tab == 0) {
            val kop = Money.parse(amount)
            AppField("Сумма", amount, { amount = it }, Modifier.fillMaxWidth(), suffix = "₽", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(10.dp))
            AppField("Комментарий (необязательно)", comment, { comment = it }, Modifier.fillMaxWidth(), placeholder = "Отложил с зарплаты")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(500L, 1000L, 5000L, 10000L).forEach { add ->
                    Pill("+" + Money.group(add), false, {
                        val cur = Money.parse(amount) ?: 0L
                        amount = Money.edit(cur + add * 100)
                    }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                "Добавить",
                onClick = { if (kop != null && kop > 0) onDeposit(kop, comment.trim()) },
                enabled = kop != null && kop > 0,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            val costKop = Money.parse(cost)
            val savedKop = Money.parse(saved)
            val ok = title.isNotBlank() && costKop != null && costKop > 0 && savedKop != null
            AppField("Название цели", title, { title = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            AppField("Стоимость", cost, { cost = it }, Modifier.fillMaxWidth(), suffix = "₽", keyboardType = KeyboardType.Decimal, error = costKop == null || costKop <= 0)
            Spacer(Modifier.height(10.dp))
            AppField("Накоплено фактически (корректировка)", saved, { saved = it }, Modifier.fillMaxWidth(), suffix = "₽", keyboardType = KeyboardType.Decimal, error = savedKop == null)
            Text(
                "Изменение суммы запишется в историю как корректировка.",
                color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                "Сохранить",
                onClick = { if (ok) onChangeGoal(title.trim(), costKop!!, savedKop!!) },
                enabled = ok,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
