package net.santi.miniAresActionBar

import net.kyori.adventure.key.Key

/**
 * Геометрия и настройки шрифтов блока уровня (шкала опыта + число уровня + иконка).
 * Перенос констант margin/barNudge/barWidth/iconWidth/iconSpacing из старого
 * level.js в config.yml, по образцу GlyphMetrics для долларов/аресов.
 *
 * Иконка уровня ('z') и цифры числа уровня переиспользуют уже существующий
 * шрифт долларовых цифр (level.digit-font, по умолчанию actionbar_1) - там
 * уже лежит и font/level_icon.png под символом 'z', и глифы 0-9.
 * Шкала опыта - новый шрифт level.xp-bar-font (по умолчанию actionbar_3,
 * из прикреплённого actionbar_3.json).
 */
class LevelMetrics(private val plugin: Main) {

    fun enabled(): Boolean = plugin.config.getBoolean("level.enabled", true)

    /** Диагностика: раз в 5 сек пишет в консоль сырые значения плейсхолдеров уровня/опыта. */
    fun debug(): Boolean = plugin.config.getBoolean("level.debug", false)

    fun fontNamespace(): String = plugin.config.getString("level.font-namespace", "minecraft")!!

    fun digitFont(): Key =
        Key.key(fontNamespace(), plugin.config.getString("level.digit-font", "actionbar_1")!!)

    fun xpBarFont(): Key =
        Key.key(fontNamespace(), plugin.config.getString("level.xp-bar-font", "actionbar_3")!!)

    fun iconChar(): Char =
        plugin.config.getString("level.icon-char", "z")!!.firstOrNull() ?: 'z'

    /** Ширина одного глифа цифры уровня в px - аналог "9" в getWidth() из level.js. */
    fun digitWidth(): Double = plugin.config.getDouble("level.digit-width", 9.0)

    fun barWidth(): Double = plugin.config.getDouble("level.bar-width", 30.0)

    fun iconWidth(): Double = plugin.config.getDouble("level.icon-width", 6.0)

    fun iconSpacing(): Double = plugin.config.getDouble("level.icon-spacing", 1.0)

    fun margin(): Double = plugin.config.getDouble("level.margin", -6.0)

    fun barNudge(): Double = plugin.config.getDouble("level.bar-nudge", -1.5)

    /**
     * Сдвиг влево в пикселях группы "число уровня + иконка" ЦЕЛИКОМ, не
     * трогая шкалу опыта. Как и posSpace()/negSpace() из money.js/level.js,
     * знак значения не важен (внутри всегда abs()) - направление всегда
     * "влево", величиной в это число пикселей.
     */
    fun digitNudge(): Double = plugin.config.getDouble("level.digit-nudge", 0.0)

    /** Аналог getWidth(text) из level.js: text.length * digitWidth() - 1. */
    fun textWidth(text: String): Double {
        if (text.isEmpty()) return 0.0
        return text.length * digitWidth() - 1.0
    }

    // --- Реальная формула требования опыта (НЕ формула из старого level.js!) ---
    // Старая формула (level+1)*70 была перенесена из JS-костыля как есть и не
    // совпадает с фактической системой опыта (посчитано по факту: для L>=1
    // требование - L*70, для L=0 - отдельная константа 25, не укладывающаяся
    // в линейный прирост). Вынесено в конфиг на случай, если эти цифры тоже
    // окажутся неточными - тогда не придётся пересобирать плагин.

    /** Прирост требования опыта на уровень, начиная со 2-го уровня (L>=1): goal = L * increment. */
    fun expIncrement(): Double = plugin.config.getDouble("level.exp.increment", 70.0)

    /** Требование опыта для самого первого уровня (L=0) - особый случай, не укладывается в линейный прирост. */
    fun firstLevelGoal(): Double = plugin.config.getDouble("level.exp.first-level-goal", 25.0)

    /** Сколько опыта нужно набрать на уровне level, чтобы перейти на level+1. */
    fun levelGoal(level: Int): Double {
        if (level <= 0) return firstLevelGoal()
        return level * expIncrement()
    }
}
