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

    fun iconWidth(): Double = plugin.config.getDouble("level.icon-width", 6.0)

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
