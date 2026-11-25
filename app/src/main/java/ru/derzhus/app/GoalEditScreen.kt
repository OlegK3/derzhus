package ru.derzhus.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Создание и редактирование цели. goalId == null — новая цель. */
@Composable
fun GoalEditScreen(vm: MainVm, goalId: String?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val existing = goalId?.let { id -> vm.data.goals.firstOrNull { it.id == id } }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var cost by remember { mutableStateOf(if (existing != null) Money.edit(existing.costKop) else "") }
    var saved by remember { mutableStateOf(if (existing != null) Money.edit(existing.savedKop) else "") }
    var main by remember { mutableStateOf(existing?.isMain ?: vm.data.goals.none { it.isMain }) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var more by remember { mutableStateOf(note.isNotBlank()) }
    var touched by remember { mutableStateOf(false) }
    val photos = remember { mutableStateListOf<String>().also { l -> existing?.photos?.let { l.addAll(it) } } }
    val added = remember { mutableStateListOf<String>() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val names = vm.importPhotos(uris)
                photos.addAll(names)
                added.addAll(names)
            }
        }
    }
    val pickPhotos = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    val costKop = Money.parse(cost)
    val savedKop = if (saved.isBlank()) 0L else Money.parse(saved)
    val titleOk = title.isNotBlank()
    val costOk = costKop != null && costKop > 0
    val savedOk = savedKop != null
    val valid = titleOk && costOk && savedOk

    fun cancel() {
        // фото, добавленные в этой сессии, но не сохранённые, удаляем
        vm.deletePhotoFiles(added.filter { it !in (existing?.photos ?: emptyList()) })
        onClose()
    }

    BackHandler { cancel() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(Dim.MinTouch).clickable { cancel() }, contentAlignment = Alignment.Center) {
                AppIcon(R.drawable.ic_arrow_back, tint = TextMain, contentDescription = "Назад")
            }
            Spacer(Modifier.width(4.dp))
            Text(
                if (existing == null) "Новая цель" else "Редактировать цель",
                color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
            )
            Box(
                Modifier.size(Dim.MinTouch).clickable {
                    touched = true
                    if (valid) {
                        vm.deletePhotoFiles(added.filter { it !in photos })
                        vm.saveGoal(existing, title.trim(), costKop!!, savedKop!!, photos.toList(), main, note.trim())
                        onClose()
                    }
                },
                contentAlignment = Alignment.Center
            ) { AppIcon(R.drawable.ic_check, tint = if (valid) Accent else Muted, contentDescription = "Сохранить") }
        }

        Column(Modifier.padding(horizontal = Dim.PagePadding)) {
            // Главное фото
            Box(
                Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Border, RoundedCornerShape(20.dp)).clickable(onClick = pickPhotos),
                contentAlignment = Alignment.Center
            ) {
                GoalImage(photos.firstOrNull()?.let { photoFile(ctx, it) }, Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Surface1.copy(alpha = 0.92f)).padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(R.drawable.ic_camera, tint = AccentDeep, size = 20.dp)
                        Text("  Добавить фото", color = TextMain, fontSize = 14.sp)
                    }
                }
            }

            // Миниатюры
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 10.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                itemsIndexed(photos.toList()) { _, name ->
                    Box(Modifier.size(62.dp)) {
                        GoalImage(photoFile(ctx, name), Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), 20.sp)
                        Box(
                            Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape)
                                .background(Surface1.copy(alpha = 0.92f)).clickable { photos.remove(name) },
                            contentAlignment = Alignment.Center
                        ) { AppIcon(R.drawable.ic_close, tint = TextMain, size = 12.dp, contentDescription = "Убрать фото") }
                    }
                }
                item {
                    Box(
                        Modifier.size(62.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Border, RoundedCornerShape(12.dp))
                            .background(Surface1).clickable(onClick = pickPhotos),
                        contentAlignment = Alignment.Center
                    ) { AppIcon(R.drawable.ic_plus, tint = AccentDeep, contentDescription = "Добавить фото") }
                }
            }

            Spacer(Modifier.height(16.dp))
            AppField("Название цели", title, { title = it }, Modifier.fillMaxWidth(), placeholder = "Например: Мотоцикл мечты", error = touched && !titleOk)
            Spacer(Modifier.height(12.dp))
            AmountField("Стоимость", cost, { cost = it }, Modifier.fillMaxWidth(), placeholder = "Например, 50 000 ₽", error = touched && !costOk)
            Spacer(Modifier.height(12.dp))
            AmountField("Уже накоплено (фактически)", saved, { saved = it }, Modifier.fillMaxWidth(), placeholder = "0", error = touched && !savedOk)
            if (existing != null) {
                Text(
                    "Если изменить сумму, в истории появится корректировка.",
                    color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(Modifier.height(14.dp))
            AppCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(R.drawable.ic_star, size = 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Сделать главной целью", color = TextMain, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    AppSwitch(main) { main = it }
                }
            }

            Spacer(Modifier.height(10.dp))
            AppCard(Modifier.fillMaxWidth(), onClick = { more = !more }, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Дополнительно", color = TextMain, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    AppIcon(R.drawable.ic_chevron_right, tint = Muted, modifier = Modifier.rotate(if (more) 90f else 0f))
                }
            }
            if (more) {
                Spacer(Modifier.height(10.dp))
                AppField("Заметка о цели", note, { note = it }, Modifier.fillMaxWidth(), singleLine = false, placeholder = "Зачем мне это и когда хочу достичь")
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                "Сохранить цель",
                onClick = {
                    touched = true
                    if (valid) {
                        vm.deletePhotoFiles(added.filter { it !in photos })
                        vm.saveGoal(existing, title.trim(), costKop!!, savedKop!!, photos.toList(), main, note.trim())
                        onClose()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
