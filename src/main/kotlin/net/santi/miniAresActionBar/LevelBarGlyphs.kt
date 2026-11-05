package net.santi.miniAresActionBar

/**
 * Кадр иконки шкалы опыта уровня - аналог getXpBar()/String.fromCharCode(0xE300 + evenP)
 * из старого level.js (JS-экспаншн PlaceholderAPI).
 *
 * Новый шрифт из ресурспака (actionbar_3.json) хранит 51 кадр шкалы
 * (font/xp/0.png .. font/xp/100.png, шаг 2) на символах 'A'..'Z','a'..'y' -
 * специально БЕЗ 'z', т.к. 'z' в actionbar_1.json уже занят под иконку уровня
 * (font/level_icon.png), чтобы оба шрифта можно было использовать рядом без коллизий.
 */
object LevelBarGlyphs {

    // A..Z (26) + a..y (25) = 51 символ = 51 кадр на значения процента 0,2,4,...,100
    private val FRAMES: List<Char> = ('A'..'Z') + ('a'..'y')

    /**
     * percent - процент заполнения шкалы опыта (0..100, дробный).
     * Берётся чётное значение вниз (как evenP в level.js), чтобы шаг совпадал
     * с шагом в 2, с которым нарезаны кадры в ресурспаке.
     */
    fun barChar(percent: Double): Char {
        var p = percent
        if (p.isNaN() || p < 0.0) p = 0.0
        if (p > 100.0) p = 100.0

        val floored = Math.floor(p).toInt()
        val even = if (floored % 2 == 0) floored else floored - 1
        val index = (even / 2).coerceIn(0, FRAMES.size - 1)
        return FRAMES[index]
    }
}
