package ru.derzhus.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.util.UUID

private data class Preset(
    val label: String,
    val name: String,       // «без ...» в родительном падеже
    val unit: String,
    val units: String,
    val priceLabel: String,
    val price: String,
    val pack: Int           // сколько единиц в указанной цене (пачка = 20 сигарет)
)

private val presets = listOf(
    Preset("Алкоголь", "алкоголя", "порций", "1", "Цена одной порции, ₽", "300", 1),
    Preset("Сигареты", "сигарет", "сигарет", "10", "Цена пачки, ₽ (20 шт.)", "200", 20),
    Preset("Сладкое", "сладкого", "порций", "2", "Цена одной порции, ₽", "100", 1),
    Preset("Соцсети", "соцсетей", "часов", "3", "Цена часа, ₽ (можно 0)", "0", 1),
    Preset("Своё", "", "раз", "1", "Цена за раз, ₽ (можно 0)", "0", 1)
)

/** Добавление привычки: алкоголь, сигареты, сладкое, соцсети или своя. */
@Composable
fun AddHabitScreen(canCancel: Boolean, onCancel: () -> Unit, onSave: (Habit) -> Unit) {
    var preset by remember { mutableStateOf(presets[0]) }
    var name by remember { mutableStateOf(presets[0].name) }
    var units by remember { mutableStateOf(presets[0].units) }
    var price by remember { mutableStateOf(presets[0].price) }
    var start by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val unitsVal = units.replace(',', '.').toDoubleOrNull()
    val priceKop = Money.parse(price)
    val valid = name.isNotBlank() && unitsVal != null && unitsVal > 0 && priceKop != null

    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Новая привычка", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "От чего ты отказываешься? Мы посчитаем дни, расчётную экономию и поддержим наградами.",
            color = Muted, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { p ->
                Pill(p.label, selected = p == preset, onClick = {
                    preset = p; name = p.name; units = p.units; price = p.price
                })
            }
        }

        Spacer(Modifier.height(16.dp))
        AppField("Без чего (например: сигарет)", name, { name = it }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        AppField(
            "Сколько было в день, в среднем (${preset.unit})", units, { units = it },
            Modifier.fillMaxWidth(), keyboardType = KeyboardType.Decimal
        )
        Spacer(Modifier.height(12.dp))
        AppField(
            preset.priceLabel, price, { price = it },
            Modifier.fillMaxWidth(), suffix = "₽", keyboardType = KeyboardType.Decimal
        )

        Text("Когда начал(а)", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        StartTimeField(start) { start = it }

        PrimaryButton(
            "Начать отсчёт",
            onClick = {
                onSave(
                    Habit(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        unitLabel = preset.unit,
                        unitsPerDay = unitsVal!!,
                        pricePerUnit = priceKop!! / 100.0 / preset.pack,
                        startMillis = minOf(start, System.currentTimeMillis())
                    )
                )
            },
            enabled = valid,
            modifier = Modifier.padding(top = 28.dp).fillMaxWidth()
        )
        if (canCancel) {
            TextButton(onClick = onCancel, modifier = Modifier.padding(top = 4.dp)) {
                Text("Отмена", color = Muted)
            }
        }
    }
}
