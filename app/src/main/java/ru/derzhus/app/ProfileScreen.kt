package ru.derzhus.app

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ProfileDialog { None, Name, Calc, Remind, Backup, About, Distraction }

/** Экран «Профиль»: имя, статистика, настройки расчёта, напоминания, резервная копия. */
@Composable
fun ProfileScreen(vm: MainVm, now: Long, onAddHabit: () -> Unit, onStats: () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val data = vm.data
    val habit = data.mainHabit
    val xp = Game.xp(data, now)

    var dialog by remember { mutableStateOf(ProfileDialog.None) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    fun toast(text: String) = Toast.makeText(ctx, text, Toast.LENGTH_LONG).show()
    fun backupName() = "derzhus-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".zip"

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) scope.launch {
            toast(if (vm.exportTo(uri)) "Файл сохранён" else "Не удалось сохранить файл")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingImport = uri }
    val avatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) scope.launch {
            val name = vm.importPhotos(listOf(uri)).firstOrNull()
            if (name != null) {
                val old = vm.data.settings.avatar
                vm.updateSettings { it.copy(avatar = name) }
                if (old != null) vm.deletePhotoFiles(listOf(old))
            } else {
                toast("Не удалось загрузить фото")
            }
        }
    }

    val days = habit?.let { Calc.fullDays(it, now) } ?: 0L
    val goalsCount = data.goals.size.toLong()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        ScreenTitle("Профиль")

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            GoalImage(
                data.settings.avatar?.let { photoFile(ctx, it) },
                Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .border(2.dp, Accent, CircleShape)
                    .clickableNoRipple {
                        avatarLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(data.settings.userName, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (habit != null) "${habit.title} с ${formatDate(habit.startMillis)}" else "Добавьте первую привычку",
                    color = Muted, fontSize = 14.sp
                )
            }
            Box(Modifier.size(Dim.MinTouch).clickableNoRipple { dialog = ProfileDialog.Name }, contentAlignment = Alignment.Center) {
                AppIcon(R.drawable.ic_edit, tint = TextMain, contentDescription = "Изменить имя")
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(Dim.PagePadding),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap)
        ) {
            MiniStat("$days", "${Calc.dayWord(days)} серии", Modifier.weight(1f))
            MiniStat("$xp", "XP · ур. ${Levels.forXp(xp).n}", Modifier.weight(1f))
            MiniStat("$goalsCount", Calc.plural(goalsCount, "цель", "цели", "целей"), Modifier.weight(1f))
        }

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingRow(
                R.drawable.ic_settings,
                "Параметры расчёта",
                if (habit != null) "Расходы на ${habit.name}: ${Money.rub(habit.perDayKop)} / день" else "Привычки и прогноз накоплений"
            ) { dialog = ProfileDialog.Calc }
            SettingRow(
                R.drawable.ic_bell,
                "Напоминания",
                if (data.settings.remindersOn) "Каждый день в ${data.settings.reminderHour}:00" else "Выключены"
            ) { dialog = ProfileDialog.Remind }
            SettingRow(R.drawable.ic_chart, "Статистика", "Накопления по неделям, месяцам и годам") { onStats() }
            SettingRow(
                R.drawable.ic_list, "Мой способ отвлечься",
                data.settings.distraction.ifBlank { "Не задан. Нажми, чтобы добавить" }.replace('\n', ' ')
            ) { dialog = ProfileDialog.Distraction }
            SettingRow(R.drawable.ic_cloud, "Резервная копия", "Сохранить или восстановить данные") {
                dialog = ProfileDialog.Backup
            }
            SettingRow(R.drawable.ic_export, "Экспорт данных", "Выгрузить прогресс и фото (файл)") {
                exportLauncher.launch(backupName())
            }
            SettingRow(R.drawable.ic_import, "Импорт данных", "Загрузить сохранённый файл") {
                importLauncher.launch(arrayOf("*/*"))
            }
            SettingRow(R.drawable.ic_info, "О приложении", "Версия и принципы работы") {
                dialog = ProfileDialog.About
            }
        }
    }

    // ---------- Диалоги ----------

    when (dialog) {
        ProfileDialog.Name -> {
            var name by remember { mutableStateOf(data.settings.userName) }
            AppDialog({ dialog = ProfileDialog.None }, "Ваше имя") {
                AppField("Имя", name, { name = it.take(30) })
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Сохранить", {
                    vm.updateSettings { it.copy(userName = name.trim().ifEmpty { "Мой путь" }) }
                    dialog = ProfileDialog.None
                }, Modifier.fillMaxWidth())
            }
        }

        ProfileDialog.Calc -> AppDialog({ dialog = ProfileDialog.None }, "Параметры расчёта") {
            CalcSettings(vm, now) {
                dialog = ProfileDialog.None
                onAddHabit()
            }
        }

        ProfileDialog.Remind -> AppDialog({ dialog = ProfileDialog.None }, "Напоминания") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ежедневная поддержка", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Тёплое сообщение и новые награды", color = Muted, fontSize = 13.sp)
                }
                AppSwitch(data.settings.remindersOn) { on -> vm.updateSettings { it.copy(remindersOn = on) } }
            }
            if (data.settings.remindersOn) {
                Text("Время", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 8, 9, 10, 12, 15, 18, 20, 21).forEach { h ->
                        Pill("$h:00", data.settings.reminderHour == h, {
                            vm.updateSettings { it.copy(reminderHour = h) }
                        })
                    }
                }
                Text(
                    "Если уведомления не приходят, проверьте разрешение для приложения в настройках телефона.",
                    color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        ProfileDialog.Backup -> AppDialog({ dialog = ProfileDialog.None }, "Резервная копия") {
            Text(
                "Файл копии содержит привычки, мечты, историю накоплений и фотографии. " +
                    "Храните его в облаке или отправьте себе в мессенджер: при переустановке без копии данные пропадут, " +
                    "потому что приложение хранит их только на этом телефоне.",
                color = Muted, fontSize = 14.sp, lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Сохранить копию", {
                dialog = ProfileDialog.None
                exportLauncher.launch(backupName())
            }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            GhostButton("Восстановить из копии", {
                dialog = ProfileDialog.None
                importLauncher.launch(arrayOf("*/*"))
            }, Modifier.fillMaxWidth())
        }

        ProfileDialog.About -> AppDialog({ dialog = ProfileDialog.None }, "Новая жизнь") {
            Text("Счётчик дней без вредных привычек: сигареты, алкоголь, сладкое, соцсети или ваша цель.", color = TextMain, fontSize = 15.sp, lineHeight = 21.sp)
            Text(
                "Приложение работает без интернета, без регистрации и без сервера. Все данные и фото хранятся только на вашем телефоне. " +
                    "Расчётная экономия и реально отложенные деньги считаются отдельно.",
                color = Muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 10.dp)
            )
            Text("Версия 2.0 · ru.derzhus.app", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp))
        }

        ProfileDialog.Distraction -> {
            var text by remember { mutableStateOf(data.settings.distraction) }
            AppDialog({ dialog = ProfileDialog.None }, "Мой способ отвлечься") {
                Text(
                    "Показывается в разделе «Тянет? Нужна помощь». Хранится только на этом телефоне.",
                    style = AppType.Caption
                )
                Spacer(Modifier.height(10.dp))
                AppField(
                    "Что помогает отвлечься", text, { text = it.take(DISTRACTION_MAX) },
                    Modifier.fillMaxWidth(), singleLine = false,
                    placeholder = "Например, прогуляться, позвонить другу, послушать музыку…"
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Сохранить", {
                    vm.updateSettings { it.copy(distraction = text.trim(), distractionAsked = true) }
                    dialog = ProfileDialog.None
                }, Modifier.fillMaxWidth())
                if (data.settings.distraction.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    GhostButton("Удалить запись", {
                        vm.updateSettings { it.copy(distraction = "") }
                        dialog = ProfileDialog.None
                    }, Modifier.fillMaxWidth(), color = Danger)
                }
            }
        }

        ProfileDialog.None -> Unit
    }

    val uri = pendingImport
    if (uri != null) {
        AppDialog({ pendingImport = null }, "Восстановить данные?") {
            Text(
                "Текущие привычки, мечты и фото на этом телефоне будут заменены данными из файла.",
                color = Muted, fontSize = 15.sp, lineHeight = 21.sp
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Восстановить", {
                pendingImport = null
                scope.launch {
                    toast(if (vm.importFrom(uri)) "Данные восстановлены" else "Файл не подходит")
                }
            }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            GhostButton("Отмена", { pendingImport = null }, Modifier.fillMaxWidth(), color = Muted)
        }
    }
}

@Composable
private fun CalcSettings(vm: MainVm, now: Long, onAddHabit: () -> Unit) {
    val data = vm.data

    Text(
        "Расчётная экономия считается по этим параметрам. Это предполагаемая сумма, а не отложенные деньги.",
        color = Muted, fontSize = 13.sp, lineHeight = 18.sp
    )
    data.habits.forEach { h ->
        HabitEditor(vm, h, now)
    }

    Text("Прогноз достижения целей", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("По экономии", data.settings.paceMode == "estimate", { vm.updateSettings { it.copy(paceMode = "estimate") } })
        Pill("Свой темп", data.settings.paceMode == "custom", { vm.updateSettings { it.copy(paceMode = "custom") } })
        Pill("Без прогноза", data.settings.paceMode == "off", { vm.updateSettings { it.copy(paceMode = "off") } })
    }
    if (data.settings.paceMode == "custom") {
        var pace by remember { mutableStateOf(Money.edit(data.settings.customPaceKop)) }
        Spacer(Modifier.height(8.dp))
        AppField(
            "Откладываю в день", pace,
            {
                pace = it
                Money.parse(it)?.let { kop -> vm.updateSettings { s -> s.copy(customPaceKop = kop) } }
            },
            suffix = "₽", keyboardType = KeyboardType.Decimal,
            error = Money.parse(pace) == null
        )
    }
    Text(
        when (data.settings.paceMode) {
            "estimate" -> "Срок считается так, будто вы откладываете всю расчётную экономию."
            "custom" -> "Срок считается по сумме, которую вы действительно откладываете каждый день."
            else -> "Срок достижения цели не показывается."
        },
        color = Muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp)
    )

    Spacer(Modifier.height(16.dp))
    GhostButton("Добавить привычку", onAddHabit, Modifier.fillMaxWidth(), color = Accent)
}

@Composable
private fun HabitEditor(vm: MainVm, h: Habit, now: Long) {
    var units by remember(h.id) { mutableStateOf(trimNumber(h.unitsPerDay)) }
    var cost by remember(h.id) { mutableStateOf(Money.edit(h.perDayKop)) }
    var askDelete by remember(h.id) { mutableStateOf(false) }
    val unitsVal = units.replace(',', '.').toDoubleOrNull()
    val costVal = Money.parse(cost)
    val valid = unitsVal != null && unitsVal > 0 && costVal != null

    AppCard(Modifier.fillMaxWidth().padding(top = 12.dp), padding = 14.dp) {
        Text(h.title, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        AppField("Сколько в день (${h.unitLabel})", units, { units = it }, keyboardType = KeyboardType.Decimal, error = unitsVal == null || unitsVal <= 0)
        Spacer(Modifier.height(8.dp))
        AppField("Расходы в день", cost, { cost = it }, suffix = "₽", keyboardType = KeyboardType.Decimal, error = costVal == null)
        Text("Начало серии", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
        StartTimeField(h.startMillis) { vm.updateHabit(h.copy(startMillis = minOf(it, now))) }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Сохранить", {
                if (valid) vm.setHabitCosts(h.id, unitsVal!!, costVal!!)
            }, Modifier.weight(1f), enabled = valid)
            GhostButton("Удалить", { askDelete = true }, Modifier.weight(1f), color = Danger)
        }
    }

    if (askDelete) {
        AppDialog({ askDelete = false }, "Удалить привычку?") {
            Text("«${h.title}» и её статистика будут удалены. Мечты и накопления останутся.", color = Muted, fontSize = 15.sp)
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Удалить", { askDelete = false; vm.deleteHabit(h.id) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            GhostButton("Отмена", { askDelete = false }, Modifier.fillMaxWidth(), color = Muted)
        }
    }
}

private fun trimNumber(v: Double): String {
    val s = v.toString().replace('.', ',')
    return if (s.endsWith(",0")) s.dropLast(2) else s
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    AppCard(modifier, padding = 12.dp) {
        Text(value, color = AccentDeep, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SettingRow(@DrawableRes icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, size = 40.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, fontSize = 13.sp)
            }
            AppIcon(R.drawable.ic_chevron_right, tint = Muted)
        }
    }
}
