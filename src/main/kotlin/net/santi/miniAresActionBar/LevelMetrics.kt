package net.santi.miniAresActionBar

import net.kyori.adventure.key.Key

class LevelMetrics(private val plugin: Main) {

    fun enabled(): Boolean = plugin.config.getBoolean("level.enabled", true)

    fun debug(): Boolean = plugin.config.getBoolean("level.debug", false)

    fun fontNamespace(): String = plugin.config.getString("level.font-namespace", "minecraft")!!

    fun digitFont(): Key =
        Key.key(fontNamespace(), plugin.config.getString("level.digit-font", "actionbar_1")!!)

    fun xpBarFont(): Key =
        Key.key(fontNamespace(), plugin.config.getString("level.xp-bar-font", "actionbar_3")!!)

    fun expAnimationDurationMs(): Long = plugin.config.getLong("level.exp-animation-duration-ms", 2000L)

    fun digitWidth(): Double = plugin.config.getDouble("level.digit-width", 9.0)

    fun barWidth(): Double = plugin.config.getDouble("level.bar-width", 30.0)

    // Ширина статичной иконки уровня 'z' (font/level_icon.png) в px.
    fun levelIconWidth(): Double = plugin.config.getDouble("level.level-icon-width", 7.0)

    // Ширина кадра анимации a..p (others/exp_anim/*.png) в px.
    // На неё возвращается курсор после печати кадра поверх шкалы опыта,
    // чтобы анимация не сдвигала остальной текст actionbar.
    fun animIconWidth(): Double = plugin.config.getDouble("level.anim-icon-width", 2.0)

    // Горизонтальный сдвиг (px) анимации получения опыта (кадры a..p) относительно
    // начала шкалы опыта (xp-bar). Раньше кадр анимации накладывался на иконку
    // уровня ('z'), теперь рисуется НАД шкалой/баром - двигай это значение в
    // игре (config.yml: level.anim-icon-offset-x), чтобы поставить иконку в
    // нужное место над баром по горизонтали.
    fun animIconOffsetX(): Double = plugin.config.getDouble("level.anim-icon-offset-x", 0.0)

    // Старое значение оставлено для совместимости с buildLevelBlock(), где оно
    // используется как ширина группы "иконка" в расчётах centerOffset. По умолчанию
    // теперь берётся из level-icon-width (видимая группа = ширина 'z').
    fun iconWidth(): Double =
        if (plugin.config.contains("level.icon-width")) {
            plugin.config.getDouble("level.icon-width")
        } else {
            levelIconWidth()
        }

    fun iconSpacing(): Double = plugin.config.getDouble("level.icon-spacing", 1.0)

    fun margin(): Double = plugin.config.getDouble("level.margin", -6.0)

    fun barNudge(): Double = plugin.config.getDouble("level.bar-nudge", -1.5)

    fun digitNudge(): Double = plugin.config.getDouble("level.digit-nudge", 0.0)

    fun textWidth(text: String): Double {
        if (text.isEmpty()) return 0.0
        return text.length * digitWidth() - 1.0
    }

    fun expIncrement(): Double = plugin.config.getDouble("level.exp.increment", 70.0)

    fun firstLevelGoal(): Double = plugin.config.getDouble("level.exp.first-level-goal", 25.0)

    fun levelGoal(level: Int): Double {
        if (level <= 0) return firstLevelGoal()
        return level * expIncrement()
    }
}