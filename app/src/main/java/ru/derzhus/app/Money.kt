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

    /**
     * Очистка вводимой суммы: только цифры и одна запятая (точка превращается в запятую),
     * до 2 знаков после запятой, до 9 цифр в целой части (защита от переполнения),
     * без ведущих нулей. Пробелы и любые другие символы (в том числе из буфера) отбрасываются.
     */
    fun sanitize(input: String): String {
        val sb = StringBuilder()
        var comma = false
        var intDigits = 0
        var dec = 0
        for (c in input) {
            when {
                c in '0'..'9' -> if (comma) {
                    if (dec < 2) { sb.append(c); dec++ }
                } else if (intDigits < 9) {
                    sb.append(c); intDigits++
                }
                (c == ',' || c == '.') && !comma -> {
                    comma = true
                    if (sb.isEmpty()) sb.append('0')
                    sb.append(',')
                }
            }
        }
        while (sb.length > 1 && sb[0] == '0' && sb[1] != ',') sb.deleteCharAt(0)
        return sb.toString()
    }

    /** Для подстановки в поле ввода. */
    fun edit(kop: Long): String {
        val r = kop / 100
        val k = kop % 100
        return if (k == 0L) "$r" else "$r,${k.toString().padStart(2, '0')}"
    }
}
