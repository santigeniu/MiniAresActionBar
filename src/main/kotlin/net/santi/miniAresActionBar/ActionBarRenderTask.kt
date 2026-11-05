package net.santi.miniAresActionBar

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit

class ActionBarRenderTask(private val plugin: Main) : org.bukkit.scheduler.BukkitRunnable() {

    // Кадры анимации иконки ареса: A..K -> others/ares_anim/0..10.png. A (0.png) - статичный кадр.
    private val aresFrames = ('A'..'K').toList()

    // Кадры анимации иконки доллара: L..V -> others/dollar_anim/0..10.png. L (0.png) - статичный кадр.
    private val dollarFrames = ('L'..'V').toList()

    // Кадры анимации иконки получения опыта: a..p -> others/exp_anim/a1..d4.png (16 кадров).
    private val expFrames = ('a'..'p').toList()

    // Статичная иконка уровня: 'z' из actionbar_1.json (font/level_icon.png).
    // Рисуется ВСЕГДА, кадры a..p накладываются поверх неё во время анимации.
    private val levelIconStaticChar = 'z'

    private val metrics = GlyphMetrics(plugin)
    private val levelMetrics = LevelMetrics(plugin)

    override fun run() {
        val onlinePlayers = Bukkit.getOnlinePlayers()
        if (onlinePlayers.isEmpty()) return

        val namespace = plugin.config.getString("font-namespace", "mineares")!!
        val dollarDigitFont = Key.key(namespace, plugin.config.getString("dollar-digit-font", "actionbar_1")!!)
        val aresDigitFont = Key.key(namespace, plugin.config.getString("ares-digit-font", "actionbar_2")!!)
        val iconFont = Key.key(namespace, "actionbar")
        // 'z' (статичная иконка уровня) лежит в actionbar_1.json
        val levelIconStaticFont = Key.key(namespace, "actionbar_1")
        val animDurationMs = plugin.config.getLong("animation-duration-ms", 2000L)

        val leadingSpaces = plugin.config.getInt("actionbar-align.leading-spaces", 0)
        val trailingSpaces = plugin.config.getInt("actionbar-align.trailing-spaces", 40)

        val leadingPad = if (leadingSpaces > 0) " ".repeat(leadingSpaces) else ""
        val trailingPad = if (trailingSpaces > 0) " ".repeat(trailingSpaces) else ""

        val now = System.currentTimeMillis()

        for (player in onlinePlayers) {
            val state = BalanceStateManager.get(player.uniqueId)

            val aresIcon = frameFor(aresFrames, state.aresAnimStart, now, animDurationMs)
            val dollarIcon = frameFor(dollarFrames, state.dollarAnimStart, now, animDurationMs)

            // Кадр анимации опыта: null, если анимация не идёт.
            val levelExpDuration = levelMetrics.expAnimationDurationMs()
            val animIconChar: Char? =
                if (isAnimating(state.levelIconAnimStart, now, levelExpDuration)) {
                    frameFor(expFrames, state.levelIconAnimStart, now, levelExpDuration)
                } else {
                    null
                }

            val dollarBlock = buildBlock(
                iconChar = dollarIcon,
                digits = state.dollarsFormatted,
                iconFont = iconFont,
                digitFont = dollarDigitFont,
                isAres = false
            )

            val aresBlock = buildBlock(
                iconChar = aresIcon,
                digits = state.aresFormatted,
                iconFont = iconFont,
                digitFont = aresDigitFont,
                isAres = true
            )

            val componentBuilder = Component.text()
                .append(Component.text(leadingPad))
                .append(dollarBlock)
                .append(aresBlock)

            if (levelMetrics.enabled()) {
                componentBuilder.append(
                    buildLevelBlock(
                        level = state.level,
                        xpPercent = state.xpPercent,
                        levelIconChar = levelIconStaticChar,
                        levelIconFont = levelIconStaticFont,
                        animIconChar = animIconChar,
                        animIconFont = iconFont
                    )
                )
            }

            componentBuilder.append(Component.text(trailingPad))

            player.sendActionBar(componentBuilder.build())
        }
    }

    private fun buildBlock(
        iconChar: Char,
        digits: String,
        iconFont: Key,
        digitFont: Key,
        isAres: Boolean
    ): Component {
        val blockWidth = metrics.blockWidth(isAres, digits)
        val calibration = metrics.calibration(isAres, digits.length)

        return Component.text()
            .append(Component.text(SpaceGlyphs.shift(calibration)))
            .append(Component.text(SpaceGlyphs.negSpace(blockWidth)))
            .append(glyphText(digits, digitFont))
            .append(Component.text(iconChar.toString()).font(iconFont))
            .append(Component.text(SpaceGlyphs.negSpace(calibration)))
            .build()
    }

    /**
     * Блок уровня: шкала опыта + число уровня + иконка.
     *
     * 'z' рисуется ВСЕГДА. Во время анимации поверх неё печатается кадр a..p:
     *   печать 'z'                    → курсор +levelIconWidth
     *   negSpace(levelIconWidth)      → курсор назад на X_icon
     *   печать кадра a..p             → курсор +animIconWidth
     *   negSpace(animIconWidth)       → курсор назад на X_icon
     * В итоге группа занимает ровно ширину 'z' (levelIconWidth), курсор не уезжает.
     */
    private fun buildLevelBlock(
        level: Int,
        xpPercent: Double,
        levelIconChar: Char,
        levelIconFont: Key,
        animIconChar: Char?,
        animIconFont: Key
    ): Component {
        val lvlText = level.toString()
        val barChar = LevelBarGlyphs.barChar(xpPercent)
        val digitFont = levelMetrics.digitFont()

        val iconSpacing = levelMetrics.iconSpacing()
        // Видимая ширина группы "иконка" = ширина 'z', т.к. кадр лежит поверх неё.
        val groupIconWidth = levelMetrics.levelIconWidth()
        val levelIconWidth = levelMetrics.levelIconWidth()
        val animIconWidth = levelMetrics.animIconWidth()
        val margin = levelMetrics.margin()
        val barNudge = levelMetrics.barNudge()
        val barWidth = levelMetrics.barWidth()
        val digitNudge = levelMetrics.digitNudge()

        val totalTextWidth = levelMetrics.textWidth(lvlText) + iconSpacing + groupIconWidth
        val centerOffset = (barWidth - totalTextWidth) / 2.0

        val finalReturn = margin + centerOffset + totalTextWidth - digitNudge

        val builder = Component.text()
            // Шкала опыта.
            .append(Component.text(SpaceGlyphs.posSpace(margin + barNudge)))
            .append(Component.text(barChar.toString()).font(levelMetrics.xpBarFont()))
            .append(Component.text(SpaceGlyphs.negSpace(barWidth + barNudge)))
            // Группа "число уровня + иконка".
            .append(Component.text(SpaceGlyphs.negSpace(digitNudge)))
            .append(Component.text(SpaceGlyphs.posSpace(centerOffset)))
            .append(glyphText(lvlText, digitFont))
            .append(Component.text(SpaceGlyphs.posSpace(iconSpacing)))

        // Статичная 'z' рисуется ВСЕГДА.
        builder.append(Component.text(levelIconChar.toString()).font(levelIconFont))

        // Анимация: возврат по реальной ширине 'z' и по реальной ширине кадра.
        // Оба negSpace разные - это ключ к тому, чтобы курсор не уезжал.
        if (animIconChar != null) {
            builder.append(Component.text(SpaceGlyphs.negSpace(levelIconWidth)))
            builder.append(Component.text(animIconChar.toString()).font(animIconFont))
            builder.append(Component.text(SpaceGlyphs.negSpace(animIconWidth)))
        }

        builder.append(Component.text(SpaceGlyphs.negSpace(finalReturn)))

        return builder.build()
    }

    private fun isAnimating(animStart: Long, now: Long, durationMs: Long): Boolean {
        if (animStart < 0) return false
        return now - animStart < durationMs
    }

    private fun frameFor(frames: List<Char>, animStart: Long, now: Long, durationMs: Long): Char {
        if (animStart < 0) return frames[0]

        val elapsed = now - animStart
        if (elapsed >= durationMs) return frames[0]

        val stepMs = durationMs / frames.size
        val index = (elapsed / stepMs).toInt().coerceIn(0, frames.size - 1)
        return frames[index]
    }

    private fun glyphText(text: String, font: Key): Component {
        val builder = Component.text()
        for (c in text) {
            builder.append(Component.text(c.toString()).font(font))
        }
        return builder.build()
    }
}