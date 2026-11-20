package ru.derzhus.app

data class LevelInfo(val n: Int, val name: String, val fromXp: Int)

object Levels {
    val all = listOf(
        LevelInfo(1, "Новичок", 0),
        LevelInfo(2, "Решимость", 60),
        LevelInfo(3, "Стойкость", 150),
        LevelInfo(4, "Уверенность", 300),
        LevelInfo(5, "Сила воли", 500),
        LevelInfo(6, "Мастер", 800),
        LevelInfo(7, "Железный", 1200),
        LevelInfo(8, "Легенда", 2000)
    )

    fun forXp(xp: Int): LevelInfo = all.last { xp >= it.fromXp }
    fun next(xp: Int): LevelInfo? = all.firstOrNull { it.fromXp > xp }

    /** Полоса как на макете: «340 / 500 XP» — доля от порога следующего уровня. */
    fun bar(xp: Int): Float {
        val nx = next(xp) ?: return 1f
        return (xp.toFloat() / nx.fromXp).coerceIn(0f, 1f)
    }
}

enum class AwardKind { DAYS, MONEY, GOAL }

data class Award(
    val id: String,
    val icon: String,
    val title: String,
    val desc: String,
    val xp: Int,
    val kind: AwardKind,
    val threshold: Long   // дни / копейки / число достигнутых целей
)

/**
 * Игровая система. Всё считается из сохранённых фактов, поэтому начисление детерминировано:
 * повторное открытие приложения ничего не начисляет повторно, а открытые награды остаются навсегда.
 */
object Game {
    val dayAwards = listOf(
        Award("d1", "👣", "Первый шаг", "1 день", 10, AwardKind.DAYS, 1),
        Award("d3", "🌱", "Закрепился", "3 дня", 15, AwardKind.DAYS, 3),
        Award("d7", "🔥", "Неделя", "7 дней", 30, AwardKind.DAYS, 7),
        Award("d14", "⚡", "Две недели", "14 дней", 40, AwardKind.DAYS, 14),
        Award("d30", "⛰️", "Вершина", "30 дней", 60, AwardKind.DAYS, 30),
        Award("d60", "🧭", "Новый курс", "60 дней", 90, AwardKind.DAYS, 60),
        Award("d90", "👑", "Свобода", "90 дней", 120, AwardKind.DAYS, 90),
        Award("d180", "🌅", "Полгода", "180 дней", 160, AwardKind.DAYS, 180),
        Award("d365", "🏆", "Новая глава", "365 дней", 250, AwardKind.DAYS, 365)
    )

    val moneyAwards = listOf(
        Award("m1000", "💵", "Первая тысяча", "отложено 1 000 ₽", 20, AwardKind.MONEY, 100_000),
        Award("m10000", "💰", "Десятка", "отложено 10 000 ₽", 40, AwardKind.MONEY, 1_000_000),
        Award("m50000", "🏦", "Полтинник", "отложено 50 000 ₽", 80, AwardKind.MONEY, 5_000_000),
        Award("m100000", "💎", "Сотка", "отложено 100 000 ₽", 150, AwardKind.MONEY, 10_000_000)
    )

    val goalAwards = listOf(
        Award("g1", "🎯", "Мечта сбылась", "достигнута первая цель", 100, AwardKind.GOAL, 1)
    )

    val awards: List<Award> = dayAwards + moneyAwards + goalAwards

    private fun metric(a: Award, d: AppData, now: Long): Long = when (a.kind) {
        AwardKind.DAYS -> d.habits.maxOfOrNull { Calc.recordDays(it, now) } ?: 0L
        AwardKind.MONEY -> d.totalSavedKop
        AwardKind.GOAL -> d.goals.count { it.achieved || it.achievedAt != null }.toLong()
    }

    fun isEarned(a: Award, d: AppData, now: Long): Boolean = metric(a, d, now) >= a.threshold

    /** Фиксирует новые достижения и достигнутые цели. Идемпотентна. */
    fun sync(d: AppData, now: Long): AppData {
        var goals = d.goals
        if (goals.any { it.achieved && it.achievedAt == null }) {
            goals = goals.map { if (it.achieved && it.achievedAt == null) it.copy(achievedAt = now) else it }
        }
        val base = if (goals === d.goals) d else d.copy(goals = goals)
        val fresh = awards.filter { it.id !in base.unlocked && isEarned(it, base, now) }
        if (fresh.isEmpty()) return base
        return base.copy(unlocked = base.unlocked + fresh.associate { it.id to now })
    }

    /** XP: 5 за каждый день рекорда + награды (один раз) + 50 за каждую достигнутую цель. */
    fun xp(d: AppData, now: Long): Int {
        val daysXp = d.habits.sumOf { Calc.recordDays(it, now) } * 5
        val awardXp = awards.filter { it.id in d.unlocked }.sumOf { it.xp }
        val goalXp = d.goals.count { it.achievedAt != null } * 50
        return (daysXp + awardXp + goalXp).toInt()
    }
}
