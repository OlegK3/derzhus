package ru.derzhus.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    private val day = Calc.DAY

    private fun habit(start: Long = 0L, best: Long = 0L) = Habit(
        id = "h1", name = "алкоголя", unitLabel = "порций",
        unitsPerDay = 1.0, pricePerUnit = 650.0, startMillis = start, bestMillis = best
    )

    private fun norm(s: String) = s.replace(' ', ' ')

    @Test
    fun moneyFormatAndParse() {
        assertEquals("250 000 ₽", norm(Money.rub(25_000_000)))
        assertEquals("1 234,50 ₽", norm(Money.rub(123_450)))
        assertEquals(1_000_000L, Money.parse("10 000"))
        assertEquals(150_050L, Money.parse("1500,5"))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("-5"))
        assertNull(Money.parse(""))
    }

    @Test
    fun estimatedSavingsAreExactInKopecks() {
        // 24 дня по 650 ₽ = 15 600 ₽
        assertEquals(1_560_000L, Calc.savedKop(habit(), 24 * day))
        // половина суток даёт половину дневной суммы
        assertEquals(32_500L, Calc.savedKop(habit(), day / 2))
    }

    @Test
    fun estimatedSavingsAndRealSavingsAreIndependent() {
        val goal = Goal(
            "g1", "Мотоцикл", 25_000_000L, emptyList(), 0L, true, null,
            listOf(Deposit("d1", 0L, 1_000_000L, "deposit", "Отложил с зарплаты")), ""
        )
        val data = AppData(habits = listOf(habit()), goals = listOf(goal))
        assertEquals(1_560_000L, Calc.savedKop(habit(), 24 * day))
        assertEquals(1_000_000L, data.totalSavedKop)
    }

    @Test
    fun goalProgressAndForecast() {
        val goal = Goal(
            "g1", "Мотоцикл", 25_000_000L, emptyList(), 0L, true, null,
            listOf(Deposit("d1", 0L, 4_500_000L, "initial", "")), ""
        )
        assertEquals(18, goal.percent)
        assertEquals(20_500_000L, goal.remainingKop)
        val data = AppData(habits = listOf(habit()), goals = listOf(goal))
        val pace = Calc.paceKop(data)
        assertEquals(65_000L, pace)
        assertEquals(316L, Calc.daysToGoal(goal, pace))
    }

    @Test
    fun noForecastWhenPaceUnknown() {
        val goal = Goal("g1", "X", 100_000L, emptyList(), 0L, true, null, emptyList(), "")
        val off = AppData(habits = listOf(habit()), settings = Settings(paceMode = "off"))
        assertNull(Calc.paceKop(off))
        assertNull(Calc.daysToGoal(goal, Calc.paceKop(off)))
    }

    @Test
    fun adjustmentsKeepHistoryAndSumCorrectly() {
        val goal = Goal(
            "g1", "X", 1_000_000L, emptyList(), 0L, false, null,
            listOf(
                Deposit("1", 0L, 500_000L, "deposit", ""),
                Deposit("2", 1L, -100_000L, "adjust", "Корректировка")
            ), ""
        )
        assertEquals(400_000L, goal.savedKop)
        assertEquals(2, goal.history.size)
    }

    @Test
    fun xpIsDeterministicAndIdempotent() {
        val now = 24 * day
        val data = AppData(habits = listOf(habit()))
        val once = Game.sync(data, now)
        val twice = Game.sync(once, now)
        assertEquals(once, twice)
        // 24 дня * 5 XP + награды за 1, 3, 7 и 14 дней (10 + 15 + 30 + 40)
        assertEquals(215, Game.xp(twice, now))
        assertEquals(Game.xp(once, now), Game.xp(twice, now))
    }

    @Test
    fun relapseKeepsRecordAndXp() {
        val start = 0L
        val now = 24 * day
        val h = habit(start)
        val reset = h.copy(startMillis = now, bestMillis = Calc.recordMs(h, now))
        assertEquals(24L, Calc.recordDays(reset, now))
        assertEquals(0L, Calc.fullDays(reset, now))
        val data = Game.sync(AppData(habits = listOf(h)), now)
        val after = Game.sync(data.copy(habits = listOf(reset)), now)
        assertTrue(Game.xp(after, now) >= Game.xp(data, now))
    }

    @Test
    fun jsonRoundTrip() {
        val goal = Goal(
            "g1", "Мотоцикл", 25_000_000L, listOf("a.jpg", "b.jpg"), 5L, true, null,
            listOf(Deposit("d1", 1L, 100L, "deposit", "тест")), "заметка"
        )
        val data = Game.sync(
            AppData(
                habits = listOf(habit(start = 1_000L, best = 5_000L)),
                goals = listOf(goal),
                settings = Settings(userName = "Олег", paceMode = "custom", customPaceKop = 50_000L, interests = setOf("bike"))
            ),
            40 * day
        )
        val back = Json.fromJson(JSONObject(Json.toJson(data).toString()))
        assertEquals(data, back)
    }

    @Test
    fun pluralRussian() {
        assertEquals("день", Calc.dayWord(1))
        assertEquals("дня", Calc.dayWord(24))
        assertEquals("дней", Calc.dayWord(11))
        assertEquals("дней", Calc.dayWord(316))
    }
}
