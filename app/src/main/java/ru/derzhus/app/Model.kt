package ru.derzhus.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max

/** Одна отслеживаемая привычка: «Без сигарет», «Без алкоголя» и т.д. */
data class Habit(
    val id: String,
    val name: String,          // в родительном падеже: «сигарет» -> «Без сигарет»
    val unitLabel: String,     // «сигарет», «порций», «часов»
    val unitsPerDay: Double,   // сколько было в день
    val pricePerUnit: Double,  // цена одной единицы, ₽ (0 — деньги не считаем)
    val startMillis: Long,     // начало серии
    val bestMillis: Long = 0L, // рекорд (самая длинная серия), мс
    val seen: Set<String> = emptySet() // награды, о которых уже поздравили
) {
    val title: String get() = "Без $name"
}

data class Level(val name: String, val fromDays: Long)

object Calc {
    const val DAY = 86_400_000L
    private val ru: Locale = Locale.forLanguageTag("ru-RU")

    val levels = listOf(
        Level("Новичок", 0),
        Level("Стойкий", 7),
        Level("Упорный", 30),
        Level("Железный", 100),
        Level("Легенда", 365)
    )

    fun elapsed(h: Habit, now: Long): Long = max(0L, now - h.startMillis)
    fun days(h: Habit, now: Long): Double = elapsed(h, now) / DAY.toDouble()
    fun fullDays(h: Habit, now: Long): Long = elapsed(h, now) / DAY
    fun saved(h: Habit, now: Long): Double = days(h, now) * h.unitsPerDay * h.pricePerUnit
    fun avoided(h: Habit, now: Long): Double = days(h, now) * h.unitsPerDay

    fun level(fullDays: Long): Level = levels.last { fullDays >= it.fromDays }

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

    fun money(v: Double): String = String.format(ru, "%,.0f ₽", v)

    fun count(v: Double): String = String.format(ru, "%,.0f", v)

    /** «3 дня 4 ч 12 мин» (с секундами, если withSeconds). */
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

object Store {
    private const val PREFS = "derzhus"
    private const val KEY = "habits"

    fun load(ctx: Context): List<Habit> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val seenArr = o.optJSONArray("seen")
                Habit(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    unitLabel = o.getString("unitLabel"),
                    unitsPerDay = o.getDouble("unitsPerDay"),
                    pricePerUnit = o.getDouble("pricePerUnit"),
                    startMillis = o.getLong("startMillis"),
                    bestMillis = o.optLong("bestMillis", 0L),
                    seen = if (seenArr == null) emptySet()
                    else (0 until seenArr.length()).map { seenArr.getString(it) }.toSet()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(ctx: Context, list: List<Habit>) {
        val arr = JSONArray()
        list.forEach { h ->
            arr.put(
                JSONObject()
                    .put("id", h.id)
                    .put("name", h.name)
                    .put("unitLabel", h.unitLabel)
                    .put("unitsPerDay", h.unitsPerDay)
                    .put("pricePerUnit", h.pricePerUnit)
                    .put("startMillis", h.startMillis)
                    .put("bestMillis", h.bestMillis)
                    .put("seen", JSONArray(h.seen.toList()))
            )
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
