package ru.derzhus.app

data class Award(
    val id: String,
    val emoji: String,
    val title: String,
    val desc: String,
    val byMoney: Boolean,
    val threshold: Double // дни или рубли
)

object Awards {
    val dayGoals = listOf(
        Award("d1", "🌱", "Первый шаг", "1 день", false, 1.0),
        Award("d3", "💪", "Самое трудное позади", "3 дня", false, 3.0),
        Award("d7", "🔥", "Неделя силы", "7 дней", false, 7.0),
        Award("d14", "⚡", "Две недели", "14 дней", false, 14.0),
        Award("d30", "🌙", "Месяц", "30 дней", false, 30.0),
        Award("d60", "🧭", "Два месяца", "60 дней", false, 60.0),
        Award("d100", "💯", "Сотня", "100 дней", false, 100.0),
        Award("d182", "🏔️", "Полгода", "182 дня", false, 182.0),
        Award("d365", "👑", "Год свободы", "365 дней", false, 365.0)
    )

    val moneyGoals = listOf(
        Award("m500", "🎬", "Поход в кино", "сэкономлено 500 ₽", true, 500.0),
        Award("m1000", "🍕", "Хватило на пиццу", "сэкономлено 1 000 ₽", true, 1000.0),
        Award("m5000", "👟", "Новые кроссовки", "сэкономлено 5 000 ₽", true, 5000.0),
        Award("m15000", "📱", "Новый телефон", "сэкономлено 15 000 ₽", true, 15000.0),
        Award("m50000", "🏖️", "Отпуск у моря", "сэкономлено 50 000 ₽", true, 50000.0)
    )

    val all: List<Award> = dayGoals + moneyGoals

    fun isUnlocked(a: Award, h: Habit, now: Long): Boolean =
        if (a.byMoney) h.pricePerUnit > 0 && Calc.saved(h, now) >= a.threshold
        else Calc.days(h, now) >= a.threshold

    fun unlocked(h: Habit, now: Long): List<Award> = all.filter { isUnlocked(it, h, now) }

    /** Что осталось до награды, для закрытых наград. */
    fun remainingText(a: Award, h: Habit, now: Long): String =
        if (a.byMoney) {
            if (h.pricePerUnit <= 0) "деньги не считаются"
            else "осталось ${Calc.money(a.threshold - Calc.saved(h, now))}"
        } else {
            val left = (a.threshold * Calc.DAY).toLong() - Calc.elapsed(h, now)
            "осталось ${Calc.duration(left)}"
        }
}
