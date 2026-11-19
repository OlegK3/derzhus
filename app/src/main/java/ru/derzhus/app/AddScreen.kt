@file:OptIn(ExperimentalMaterial3Api::class)

package ru.derzhus.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

private data class Preset(
    val label: String,
    val name: String,       // «без ...» в родительном падеже
    val unit: String,
    val units: String,
    val priceLabel: String,
    val price: String,
    val pack: Int           // сколько единиц в цене (пачка = 20 сигарет)
)

private val presets = listOf(
    Preset("Сигареты", "сигарет", "сигарет", "10", "Цена пачки, ₽ (20 шт.)", "200", 20),
    Preset("Алкоголь", "алкоголя", "порций", "1", "Цена одной порции, ₽", "300", 1),
    Preset("Сладкое", "сладкого", "порций", "2", "Цена одной порции, ₽", "100", 1),
    Preset("Соцсети", "соцсетей", "часов", "3", "Цена часа, ₽ (можно 0)", "0", 1),
    Preset("Своё", "", "раз", "1", "Цена за раз, ₽ (можно 0)", "0", 1)
)

private val ruLocale: Locale = Locale.forLanguageTag("ru-RU")

private fun setDate(base: Long, utcMidnight: Long): Long {
    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMidnight }
    return Calendar.getInstance().apply {
        timeInMillis = base
        set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun setTime(base: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = base
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun localDateAsUtcMidnight(millis: Long): Long {
    val l = Calendar.getInstance().apply { timeInMillis = millis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

@Composable
fun AddScreen(canCancel: Boolean, onCancel: () -> Unit, onSave: (Habit) -> Unit) {
    var preset by remember { mutableStateOf(presets[0]) }
    var name by remember { mutableStateOf(presets[0].name) }
    var units by remember { mutableStateOf(presets[0].units) }
    var price by remember { mutableStateOf(presets[0].price) }
    var start by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    val unitsVal = units.replace(',', '.').toDoubleOrNull()
    val priceVal = price.replace(',', '.').toDoubleOrNull()
    val valid = name.isNotBlank() && unitsVal != null && unitsVal > 0 && priceVal != null && priceVal >= 0

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Новая цель", color = Accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "От чего ты отказываешься? Мы посчитаем, сколько ты держишься и сколько сэкономил.",
            color = Muted, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { p ->
                FilterChip(
                    selected = p == preset,
                    onClick = {
                        preset = p; name = p.name; units = p.units; price = p.price
                    },
                    label = { Text(p.label) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it }, singleLine = true,
            label = { Text("Без чего (например: сигарет)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = units, onValueChange = { units = it }, singleLine = true,
            label = { Text("Сколько было в день, в среднем (${preset.unit})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = price, onValueChange = { price = it }, singleLine = true,
            label = { Text(preset.priceLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Когда начал(а)", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                Text(SimpleDateFormat("d MMMM yyyy", ruLocale).format(Date(start)))
            }
            OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                Text(SimpleDateFormat("HH:mm", ruLocale).format(Date(start)))
            }
        }

        Button(
            onClick = {
                onSave(
                    Habit(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        unitLabel = preset.unit,
                        unitsPerDay = unitsVal!!,
                        pricePerUnit = priceVal!! / preset.pack,
                        startMillis = minOf(start, System.currentTimeMillis())
                    )
                )
            },
            enabled = valid,
            modifier = Modifier.padding(top = 28.dp).fillMaxWidth().height(54.dp)
        ) { Text("Начать отсчёт", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }

        if (canCancel) {
            TextButton(onClick = onCancel, modifier = Modifier.padding(top = 4.dp)) {
                Text("Отмена", color = Muted)
            }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = localDateAsUtcMidnight(start),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis() + Calc.DAY
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { start = minOf(setDate(start, it), System.currentTimeMillis()) }
                    showDate = false
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Отмена") } }
        ) { DatePicker(state = state) }
    }

    if (showTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        val state = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Время") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    start = minOf(setTime(start, state.hour, state.minute), System.currentTimeMillis())
                    showTime = false
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Отмена") } }
        )
    }
}
