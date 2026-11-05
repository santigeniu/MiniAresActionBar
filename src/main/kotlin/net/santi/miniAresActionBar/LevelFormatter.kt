package net.santi.miniAresActionBar

/**
 * Парсит сырые плейсхолдеры системы уровней (%lamglevels_level% /
 * %lamglevels_exp_progress%) и считает процент заполнения шкалы опыта.
 *
 * Сама формула цели (сколько опыта нужно на уровне) - в LevelMetrics.levelGoal().
 */
object LevelFormatter {

    // Цветовые коды (§a, &a и т.п.), которые может вернуть плейсхолдер
    private val COLOR_CODES = Regex("[§&][0-9a-fk-orA-FK-OR]")

    private val NUMBER = Regex("-?\\d+(?:[.,]\\d+)?")

    /**
     * Аналог parseFloat()/parseInt() из старого level.js: берёт ПЕРВОЕ число в строке
     * и игнорирует остальной текст ("35/70" -> 35, "12.5%" -> 12.5, "§a42 XP" -> 42).
     *
     * При переносе в Kotlin я сначала использовал строгий toDoubleOrNull(), который на
     * любой такой строке возвращает null -> 0 (JS-версия молча проглатывала хвост).
     */
    fun extractNumber(raw: String): Double? {
        val cleaned = COLOR_CODES.replace(raw, "")
        val match = NUMBER.find(cleaned) ?: return null
        return match.value.replace(",", ".").toDoubleOrNull()
    }

    fun parseLevel(raw: String): Int = extractNumber(raw)?.toInt() ?: 0

    fun parseExp(raw: String): Double = extractNumber(raw) ?: 0.0

    fun xpPercent(exp: Double, goal: Double): Double {
        if (goal <= 0.0) return 0.0

        var percent = (exp / goal) * 100.0
        if (percent.isNaN() || percent < 0.0) percent = 0.0
        if (percent > 100.0) percent = 100.0
        return percent
    }
}
