package net.santi.miniAresActionBar

/**
 * Форматирует сырое число баланса в компактный вид.
 * Использует только символы, которые есть в кастомном шрифте: 0-9, K, M, B, /, .
 *
 * K/M/B печатаются БЕЗ дробной части (округление до ближайшего целого):
 * 1000 -> "1K", 1500 -> "2K" (округление), 1000000 -> "1M", 1000000000 -> "1B".
 */
object BalanceFormatter {

    fun format(raw: String): String {
        val value = raw.replace(",", ".").toDoubleOrNull() ?: return raw

        return when {
            value >= 1_000_000_000 -> Math.round(value / 1_000_000_000).toString() + "B"
            value >= 1_000_000 -> Math.round(value / 1_000_000).toString() + "M"
            value >= 1_000 -> Math.round(value / 1_000).toString() + "K"
            else -> Math.floor(value).toLong().toString()
        }
    }
}