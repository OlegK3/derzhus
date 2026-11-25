package ru.derzhus.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale

/** Столбец диаграммы: подпись и сумма реальных пополнений (копейки). */
data class StatBucket(val label: String, val sumKop: Long)

data class StatPeriod(val buckets: List<StatBucket>, val totalKop: Long, val previousKop: Long?)

/**
 * Расчёт статистики только из реальных данных: пополнения целей (kind == "deposit").
 * Расчётная экономия сюда не входит, она предположительна.
 */
object StatsCalc {
    private val weekdays = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
    private val months = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")

    fun deposits(data: AppData): List<Deposit> =
        data.goals.flatMap { it.history }.filter { it.kind == "deposit" && it.amountKop > 0 }

    private fun startOfDay(ts: Long): Calendar = Calendar.getInstance().apply {
        timeInMillis = ts
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    private fun startOfMonth(ts: Long): Calendar = startOfDay(ts).apply { set(Calendar.DAY_OF_MONTH, 1) }

    private fun sum(list: List<Deposit>, from: Long, to: Long) =
        list.filter { it.ts in from until to }.sumOf { it.amountKop }

    /** period: 0 — неделя (7 дней), 1 — месяц (4 недели), 2 — год (12 месяцев), 3 — всё время (по месяцам). */
    fun compute(data: AppData, period: Int, now: Long): StatPeriod {
        val list = deposits(data)
        val today = startOfDay(now)
        val bounds = ArrayList<Pair<Long, Long>>()
        val labels = ArrayList<String>()
        when (period) {
            0, 1 -> {
                val step = if (period == 0) 1 else 7
                val count = if (period == 0) 7 else 4
                for (i in count - 1 downTo 0) {
                    val from = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -(i + 1) * step + 1) }
                    val to = (from.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, step) }
                    bounds += from.timeInMillis to to.timeInMillis
                    labels += if (period == 0) weekdays[from.get(Calendar.DAY_OF_WEEK) - 1]
                    else String.format(Locale.US, "%02d.%02d", from.get(Calendar.DAY_OF_MONTH), from.get(Calendar.MONTH) + 1)
                }
            }
            else -> {
                var count = 12
                if (period == 3) {
                    val first = list.minOfOrNull { it.ts } ?: now
                    val f = startOfMonth(first)
                    val n = startOfMonth(now)
                    val diff = (n.get(Calendar.YEAR) - f.get(Calendar.YEAR)) * 12 + n.get(Calendar.MONTH) - f.get(Calendar.MONTH) + 1
                    count = diff.coerceIn(1, 12)
                }
                val thisMonth = startOfMonth(now)
                for (i in count - 1 downTo 0) {
                    val from = (thisMonth.clone() as Calendar).apply { add(Calendar.MONTH, -i) }
                    val to = (from.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    bounds += from.timeInMillis to to.timeInMillis
                    labels += months[from.get(Calendar.MONTH)]
                }
            }
        }
        val buckets = bounds.mapIndexed { i, (a, b) -> StatBucket(labels[i], sum(list, a, b)) }
        val total = if (period == 3) list.sumOf { it.amountKop } else buckets.sumOf { it.sumKop }
        // Предыдущий такой же отрезок — для сравнения (кроме «Всё время»)
        val prev = if (period == 3) null else {
            val start = bounds.first().first
            val len = bounds.last().second - start
            sum(list, start - len, start)
        }
        return StatPeriod(buckets, total, prev)
    }

    /** Изменение к прошлому периоду в процентах; null — сравнивать не с чем. */
    fun changePercent(total: Long, previous: Long?): Int? =
        if (previous == null || previous <= 0L) null else ((total - previous) * 100 / previous).toInt()
}

@Composable
fun StatsScreen(data: AppData, now: Long, onBack: () -> Unit) {
    var period by rememberSaveable { mutableIntStateOf(0) }
    val stat = StatsCalc.compute(data, period, now)
    val change = StatsCalc.changePercent(stat.totalKop, stat.previousKop)
    val habit = data.mainHabit
    val days = habit?.let { Calc.fullDays(it, now) } ?: 0L
    val estimated = habit?.let { Calc.savedKop(it, now) } ?: 0L

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackHeader("Статистика", onBack)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dim.PagePadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Неделя", "Месяц", "Год", "Всё время").forEachIndexed { i, t ->
                Pill(t, period == i, { period = i }, Modifier.weight(1f))
            }
        }
        Column(Modifier.padding(Dim.PagePadding), verticalArrangement = Arrangement.spacedBy(Dim.CardGap)) {
            AppCard(Modifier.fillMaxWidth()) {
                Text("Отложено на мечты", style = AppType.Section)
                Text(Money.rub(stat.totalKop), style = AppType.Metric, modifier = Modifier.padding(top = 4.dp))
                when {
                    stat.totalKop == 0L -> Text(
                        "За этот период пополнений нет. Добавьте накопление в карточке цели.",
                        style = AppType.Caption, modifier = Modifier.padding(top = 2.dp)
                    )
                    change != null -> Text(
                        (if (change >= 0) "+" else "") + "$change% к прошлому периоду",
                        color = if (change >= 0) Accent else Danger, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    else -> Unit
                }
                Spacer(Modifier.height(12.dp))
                BarChart(stat.buckets)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dim.CardGap)) {
                StatCard(R.drawable.ic_calendar, "$days", habit?.let { "${Calc.dayWord(days)} без ${it.name}" } ?: "дней серии", Modifier.weight(1f))
                StatCard(R.drawable.ic_wallet, Money.rub(data.totalSavedKop), "всего отложено", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dim.CardGap)) {
                StatCard(R.drawable.ic_coins, Money.rub(estimated), "расчётная экономия", Modifier.weight(1f))
                StatCard(R.drawable.ic_achievements, "${data.unlocked.size}", "наград получено", Modifier.weight(1f))
            }
            Text(
                "Диаграмма показывает реальные пополнения целей. Расчётная экономия предположительна и в неё не входит.",
                style = AppType.Caption
            )
        }
    }
}

@Composable
private fun StatCard(icon: Int, value: String, label: String, modifier: Modifier) {
    AppCard(modifier, padding = 14.dp) {
        IconBadge(icon, size = 36.dp)
        Text(value, style = AppType.Section, modifier = Modifier.padding(top = 8.dp), maxLines = 1)
        Text(label, style = AppType.Caption)
    }
}

@Composable
private fun BarChart(buckets: List<StatBucket>) {
    val max = buckets.maxOfOrNull { it.sumKop } ?: 0L
    val desc = "Диаграмма пополнений: " + buckets.joinToString("; ") { "${it.label} ${Money.rub(it.sumKop)}" }
    val labelSize = if (buckets.size > 7) 10.sp else 12.sp
    Column(Modifier.fillMaxWidth().semantics { contentDescription = desc }) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val n = buckets.size
            val slot = size.width / n
            val barW = slot * 0.55f
            drawLine(Border, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
            buckets.forEachIndexed { i, b ->
                val h = if (max <= 0L) 0f else (b.sumKop.toFloat() / max) * (size.height - 4.dp.toPx())
                val bar = if (b.sumKop > 0L) h.coerceAtLeast(4.dp.toPx()) else 3.dp.toPx()
                drawRoundRect(
                    color = if (b.sumKop > 0L && b.sumKop == max) AccentDeep else if (b.sumKop > 0L) AccentLight else Border,
                    topLeft = Offset(i * slot + (slot - barW) / 2, size.height - bar),
                    size = Size(barW, bar),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            buckets.forEach {
                Text(
                    it.label, color = Muted, fontSize = labelSize, textAlign = TextAlign.Center,
                    maxLines = 1, modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
