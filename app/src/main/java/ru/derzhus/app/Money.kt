package ru.derzhus.app

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/** Деньги хранятся в копейках (Long): без ошибок округления. */
object Money {
    private const val NB = ' '

    fun group(n: Long): String =
        n.toString().reversed().chunked(3).joinToString(NB.toString()).reversed()

    /** 250 000 ₽ или 1 234,50 ₽ */
    fun rub(kop: Long): String {
        val a = abs(kop)
        val r = a / 100
        val k = a % 100
        val num = if (k == 0L) group(r) else group(r) + "," + k.toString().padStart(2, '0')
        return (if (kop < 0) "−" else "") + num + NB + "₽"
    }

    /** +500 ₽ / −200 ₽ */
    fun signed(kop: Long): String = (if (kop >= 0) "+" else "") + rub(kop)

    /** Разбор пользовательского ввода: «10 000», «1500,5», «250000.00». null — если некорректно. */
    fun parse(text: String): Long? {
        val t = text.trim().replace(" ", "").replace(" ", "").replace(',', '.')
        if (t.isEmpty()) return null
        return try {
            val bd = BigDecimal(t).setScale(2, RoundingMode.HALF_UP)
            if (bd.signum() < 0) null else bd.movePointRight(2).longValueExact()
        } catch (e: Exception) {
            null
        }
    }

    /** Для подстановки в поле ввода. */
    fun edit(kop: Long): String {
        val r = kop / 100
        val k = kop % 100
        return if (k == 0L) "$r" else "$r,${k.toString().padStart(2, '0')}"
    }
}
