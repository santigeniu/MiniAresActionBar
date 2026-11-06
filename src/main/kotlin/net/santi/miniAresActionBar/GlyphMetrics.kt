package net.santi.miniAresActionBar

/**
 * Оценка ширины блока (иконка + цифры) и ручная калибровка - аналог
 * getWidth() и dolShiftMap/areShiftMap из money.js, но настраиваемая
 * через config.yml, так как без явного "width" в font-json Minecraft сам
 * обрезает прозрачные пиксели и реальная ширина может отличаться.
 */
class GlyphMetrics(private val plugin: Main) {

    fun digitWidth(): Double = plugin.config.getDouble("glyph-widths.digit", 6.0)
    fun dotWidth(): Double = plugin.config.getDouble("glyph-widths.dot", 2.0)
    fun suffixWidth(): Double = plugin.config.getDouble("glyph-widths.suffix", 7.0)

    fun iconWidth(isAres: Boolean): Double {
        val path = if (isAres) "glyph-widths.ares-icon" else "glyph-widths.dollar-icon"
        return plugin.config.getDouble(path, 9.0)
    }

    fun textWidth(text: String): Double {
        var w = 0.0
        for (c in text) {
            w += when {
                c == '.' -> dotWidth()
                // K/M/B - все три суффикса одной ширины (glyph-widths.suffix).
                // Раньше 'B' сюда не попадал и считался как 0 - из-за этого при
                // балансе от миллиарда иконка "съезжала", т.к. blockWidth()
                // для таких длин обычно не калибруется вручную отдельно.
                c == 'K' || c == 'M' || c == 'B' -> suffixWidth()
                c.isDigit() -> digitWidth()
                else -> 0.0
            }
        }
        return w
    }

    /**
     * Полная ширина блока (иконка + цифры) для negSpace() перед печатью.
     * Если в block-width.<currency> задано явное значение под конкретную
     * длину строки цифр - используется оно (прямая калибровка "на глаз",
     * без накопления ошибки от сложения отдельных ширин символов).
     * Иначе - сумма iconWidth + textWidth (оценка по отдельным символам).
     */
    fun blockWidth(isAres: Boolean, digits: String): Double {
        val path = if (isAres) "block-width.ares" else "block-width.dollar"
        val section = plugin.config.getConfigurationSection(path)
        val key = digits.length.toString()
        if (section != null && section.contains(key)) {
            return section.getDouble(key)
        }
        return iconWidth(isAres) + textWidth(digits)
    }

    /**
     * Ручная калибровка (px) по длине строки цифр (без иконки) - подбирается
     * в игре. "default" используется, если для конкретной длины нет отдельного
     * значения.
     */
    fun calibration(isAres: Boolean, textLength: Int): Double {
        val path = if (isAres) "shift-calibration.ares" else "shift-calibration.dollar"
        val section = plugin.config.getConfigurationSection(path) ?: return 0.0
        val key = textLength.toString()
        return if (section.contains(key)) section.getDouble(key) else section.getDouble("default", 0.0)
    }
}