package net.santi.miniAresActionBar

/**
 * Парсит сырые плейсхолдеры системы уровней (%lamglevels_level% /
 * %lamglevels_exp_progress%) и считает процент заполнения шкалы опыта.
 *
 * Формула цели уровня (level + 1) * 70 - перенесена как есть из старого
 * level.js (getXpBar()).
 */
object LevelFormatter {

    fun parseLevel(raw: String): Int =
        raw.replace(",", ".").toDoubleOrNull()?.toInt() ?: 0

    fun parseExp(raw: String): Double =
        raw.replace(",", ".").toDoubleOrNull() ?: 0.0

    fun xpPercent(level: Int, exp: Double): Double {
        val goal = (level + 1) * 70.0
        if (goal <= 0.0) return 0.0

        var percent = (exp / goal) * 100.0
        if (percent.isNaN() || percent < 0.0) percent = 0.0
        if (percent > 100.0) percent = 100.0
        return percent
    }
}
