package ru.derzhus.app

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Привычка: «Без сигарет», «Без алкоголя»… */
data class Habit(
    val id: String,
    val name: String,          // в родительном падеже: «сигарет» -> «Без сигарет»
    val unitLabel: String,     // «сигарет», «порций», «часов»
    val unitsPerDay: Double,   // сколько было в день
    val pricePerUnit: Double,  // цена одной единицы, ₽ (0 — деньги не считаем)
    val startMillis: Long,     // начало текущей серии
    val bestMillis: Long = 0L  // рекорд (самая длинная серия), мс
) {
    val title: String get() = "Без $name"

    /** Расчётная экономия за сутки, в копейках. */
    val perDayKop: Long
        get() = BigDecimal.valueOf(unitsPerDay)
            .multiply(BigDecimal.valueOf(pricePerUnit))
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
}

/** Запись в истории накоплений цели. */
data class Deposit(
    val id: String,
    val ts: Long,
    val amountKop: Long,   // пополнение > 0, корректировка может быть < 0
    val kind: String,      // deposit | adjust | initial
    val comment: String
)

/** Финансовая цель («мечта»). */
data class Goal(
    val id: String,
    val title: String,
    val costKop: Long,
    val photos: List<String>,     // имена файлов в filesDir/photos
    val createdAt: Long,
    val isMain: Boolean,
    val achievedAt: Long?,
    val history: List<Deposit>,
    val note: String = ""
) {
    /** Фактически накоплено = сумма истории. Не путать с расчётной экономией привычки. */
    val savedKop: Long get() = max(0L, history.sumOf { it.amountKop })
    val remainingKop: Long get() = max(0L, costKop - savedKop)
    val achieved: Boolean get() = costKop > 0 && savedKop >= costKop
    val percent: Int
        get() = if (costKop <= 0) 0 else min(100L, savedKop * 100 / costKop).toInt()
    val progress: Float
        get() = if (costKop <= 0) 0f else (savedKop.toDouble() / costKop).toFloat().coerceIn(0f, 1f)
}

data class Settings(
    val userName: String = "Мой путь",
    val avatar: String? = null,
    val mainHabitId: String? = null,
    val paceMode: String = "estimate",   // estimate | custom | off
    val customPaceKop: Long = 0L,
    val remindersOn: Boolean = true,
    val reminderHour: Int = 9,
    val interests: Set<String> = emptySet(),
    val distraction: String = "",            // личный способ отвлечься (только на устройстве)
    val distractionAsked: Boolean = false     // шаг «Что поможет отвлечься?» пройден или пропущен
)

data class AppData(
    val habits: List<Habit> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val settings: Settings = Settings(),
    val unlocked: Map<String, Long> = emptyMap(),   // id награды -> когда открыта (навсегда)
    val celebrated: Set<String> = emptySet()        // о каких наградах уже поздравили
) {
    val mainHabit: Habit?
        get() = habits.firstOrNull { it.id == settings.mainHabitId } ?: habits.firstOrNull()

    val mainGoal: Goal?
        get() = goals.firstOrNull { it.isMain }
            ?: goals.firstOrNull { !it.achieved }
            ?: goals.firstOrNull()

    val totalSavedKop: Long get() = goals.sumOf { it.savedKop }
}

object Calc {
    const val DAY = 86_400_000L
    private val ru: Locale = Locale.forLanguageTag("ru-RU")

    fun elapsed(h: Habit, now: Long): Long = max(0L, now - h.startMillis)
    fun days(h: Habit, now: Long): Double = elapsed(h, now) / DAY.toDouble()
    fun fullDays(h: Habit, now: Long): Long = elapsed(h, now) / DAY
    fun recordMs(h: Habit, now: Long): Long = max(h.bestMillis, elapsed(h, now))
    fun recordDays(h: Habit, now: Long): Long = recordMs(h, now) / DAY

    /** Расчётная экономия в копейках: целочисленно, без накопления ошибок. */
    fun savedKop(h: Habit, now: Long): Long {
        val e = elapsed(h, now)
        val perDay = h.perDayKop
        return (e / DAY) * perDay + (e % DAY) * perDay / DAY
    }

    fun avoided(h: Habit, now: Long): Double = days(h, now) * h.unitsPerDay

    fun ceilDiv(a: Long, b: Long): Long = (a + b - 1) / b

    /** Темп накоплений (коп./день) по выбранной модели. null — темп неизвестен, прогноз не показываем. */
    fun paceKop(d: AppData): Long? = when (d.settings.paceMode) {
        "estimate" -> d.mainHabit?.perDayKop?.takeIf { it > 0 }
        "custom" -> d.settings.customPaceKop.takeIf { it > 0 }
        else -> null
    }

    fun daysToGoal(goal: Goal, paceKop: Long?): Long? = when {
        goal.achieved -> 0L
        paceKop == null || paceKop <= 0 -> null
        else -> ceilDiv(goal.remainingKop, paceKop)
    }

    fun plural(n: Long, one: String, few: String, many: String): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            m10 == 1L && m100 != 11L -> one
            m10 in 2L..4L && m100 !in 12L..14L -> few
            else -> many
        }
    }

    fun dayWord(n: Long) = plural(n, "день", "дня", "дней")
    fun daysText(n: Long) = "$n ${dayWord(n)}"

    fun count(v: Double): String = String.format(ru, "%,.0f", v)

    /** «3 дня 4 ч 12 мин» (с секундами, если withSeconds и нет полных суток). */
    fun duration(ms: Long, withSeconds: Boolean = false): String {
        val d = ms / DAY
        val h = ms / 3_600_000 % 24
        val m = ms / 60_000 % 60
        val s = ms / 1000 % 60
        val parts = mutableListOf<String>()
        if (d > 0) parts += daysText(d)
        if (d > 0 || h > 0) parts += "$h ч"
        parts += "$m мин"
        if (withSeconds && d == 0L) parts += "$s с"
        return parts.joinToString(" ")
    }
}
