package net.santi.miniAresActionBar

import java.util.Locale

/**
 * Форматирует сырое число баланса в компактный вид.
 * Использует только символы, которые есть в кастомном шрифте: 0-9, K, M, B, /, .
 *
 * Округление перенесено 1:1 из старого money.js (format()):
 *   num = num.toFixed(1)
 *   если результат оканчивается на ".0" - отбрасываем дробную часть.
 *
 * Примеры:
 *   4_500_000 -> 4.5     -> "4.5M"
 *   5_010_000 -> 5.01    -> toFixed(1) = "5.0" -> ".0" отброшен -> "5M"
 *   5_051_000 -> 5.051   -> toFixed(1) = "5.1" -> "5.1M"
 *
 * До 1000 дробная часть не нужна - целое число, как и раньше.
 */
object BalanceFormatter {

    fun format(raw: String): String {
        val value = raw.replace(",", ".").toDoubleOrNull() ?: return raw

        return when {
            value >= 1_000_000_000 -> formatScaled(value / 1_000_000_000, "B")
            value >= 1_000_000 -> formatScaled(value / 1_000_000, "M")
            value >= 1_000 -> formatScaled(value / 1_000, "K")
            else -> Math.floor(value).toLong().toString()
        }
    }

    /**
     * scaled - уже делённое на 1000/1000000/1000000000 значение (напр. 4.5 для "4.5M").
     * Округляет до 1 знака после запятой и убирает ".0", если дробная часть нулевая.
     */
    private fun formatScaled(scaled: Double, suffix: String): String {
        val rounded = String.format(Locale.ROOT, "%.1f", scaled)
        val trimmed = if (rounded.endsWith(".0")) rounded.dropLast(2) else rounded
        return trimmed + suffix
    }
}