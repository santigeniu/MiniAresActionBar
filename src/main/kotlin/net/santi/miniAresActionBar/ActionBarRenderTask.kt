package net.santi.miniAresActionBar

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit

class ActionBarRenderTask(private val plugin: Main) : org.bukkit.scheduler.BukkitRunnable() {

    // Кадры анимации иконки ареса: A..K -> others/ares_anim/0..10.png.
    // A (0.png) - статичный кадр, в анимационный цикл НЕ входит.
    // Порядок проигрывания задан явно (номера png-файлов):
    // 1, 2, 3, 8, 9, 10, 8, 9, 10, 1, 2, 3, 4, 5
    //   B  C  D  I  J  K   I  J  K   B  C  D  E  F
    private val aresFrames = listOf(
        'B', 'C', 'D', 'I', 'J', 'K', 'I', 'J', 'K', 'B', 'C', 'D', 'E', 'F'
    )
    private val aresStaticFrame = 'A' // 0.png

    // Кадры анимации иконки доллара: L..V -> others/dollar_anim/0..10.png.
    // L (0.png) - статичный кадр, в анимационный цикл НЕ входит.
    // Порядок проигрывания задан явно (номера png-файлов):
    // 1, 2, 3, 8, 9, 10, 8, 9, 10, 1, 2, 3, 4, 5
    //   M  N  O  T  U  V   T  U  V   M  N  O  P  Q
    private val dollarFrames = listOf(
        'M', 'N', 'O', 'T', 'U', 'V', 'T', 'U', 'V', 'M', 'N', 'O', 'P', 'Q'
    )
    private val dollarStaticFrame = 'L' // 0.png

    // Кадры анимации иконки получения опыта: a..p -> others/exp_anim/a1..d4.png (16 кадров).
    private val expFrames = ('a'..'p').toList()

    // Статичная иконка уровня: 'z' из actionbar_1.json (font/level_icon.png).
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

            val aresIcon = frameFor(aresFrames, aresStaticFrame, state.aresAnimStart, now, animDurationMs)
            val dollarIcon = frameFor(dollarFrames, dollarStaticFrame, state.dollarAnimStart, now, animDurationMs)

            // Кадр анимации опыта: null, если анимация не идёт.
            val levelExpDuration = levelMetrics.expAnimationDurationMs()
            val animIconChar: Char? =
                if (isAnimating(state.levelIconAnimStart, now, levelExpDuration)) {
                    frameFor(expFrames, expFrames[0], state.levelIconAnimStart, now, levelExpDuration)
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
     * 'z' (иконка уровня) рисуется ВСЕГДА и больше ничем не перекрывается -
     * анимация получения опыта (кадры a..p) теперь рисуется ОТДЕЛЬНО, прямо
     * над шкалой опыта (barChar), а не поверх 'z':
     *
     *   печать barChar                       → курсор +barWidth
     *   posSpace(animOffsetX)                → сдвиг курсора к месту анимации
     *   печать кадра a..p                     → курсор +animIconWidth
     *   negSpace(animIconWidth + animOffsetX) → курсор назад, туда же, где был
     *                                           сразу после печати barChar
     *   negSpace(barWidth + barNudge)         → обычный возврат шкалы
     *
     * animOffsetX настраивается в config.yml (level.anim-icon-offset-x) -
     * им двигаешь анимацию по горизонтали, пока она не встанет ровно над баром.
     * Сам "подъём" над строкой (чтобы было визуально НАД, а не В строке) задаётся
     * ascent'ом кадров a..p в actionbar.json (ресурспак), здесь не трогается.
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
        // Видимая ширина группы "иконка" = ширина 'z'.
        val groupIconWidth = levelMetrics.levelIconWidth()
        val margin = levelMetrics.margin()
        val barNudge = levelMetrics.barNudge()
        val barWidth = levelMetrics.barWidth()
        val digitNudge = levelMetrics.digitNudge()
        val animIconWidth = levelMetrics.animIconWidth()
        val animOffsetX = levelMetrics.animIconOffsetX()

        val totalTextWidth = levelMetrics.textWidth(lvlText) + iconSpacing + groupIconWidth
        val centerOffset = (barWidth - totalTextWidth) / 2.0

        val finalReturn = margin + centerOffset + totalTextWidth - digitNudge

        val builder = Component.text()
            // Шкала опыта.
            .append(Component.text(SpaceGlyphs.posSpace(margin + barNudge)))
            .append(Component.text(barChar.toString()).font(levelMetrics.xpBarFont()))

        // Анимация получения опыта: отдельный слой НАД баром, со своим
        // горизонтальным сдвигом (animOffsetX), курсор возвращается на место.
        //
        // ВАЖНО: тут нужен именно shift(), а не posSpace()/negSpace() напрямую -
        // они внутри берут abs() от ширины и поэтому не умеют сдвигать влево
        // через отрицательное число (posSpace(-15) двигал бы ВПРАВО на 15, а не
        // влево). shift() сам выбирает нужное направление по знаку числа, и тогда
        // при любом знаке animOffsetX суммарное смещение курсора после анимации
        // (printOffset + реальная ширина кадра + returnOffset) честно равно нулю -
        // валюты, бар и цифры уровня при анимации больше не дёргаются.
        if (animIconChar != null) {
            builder
                .append(Component.text(SpaceGlyphs.shift(animOffsetX)))
                .append(Component.text(animIconChar.toString()).font(animIconFont))
                .append(Component.text(SpaceGlyphs.shift(-(animIconWidth + animOffsetX))))
        }

        builder
            .append(Component.text(SpaceGlyphs.negSpace(barWidth + barNudge)))
            // Группа "число уровня + иконка".
            .append(Component.text(SpaceGlyphs.negSpace(digitNudge)))
            .append(Component.text(SpaceGlyphs.posSpace(centerOffset)))
            .append(glyphText(lvlText, digitFont))
            .append(Component.text(SpaceGlyphs.posSpace(iconSpacing)))

        // Статичная 'z' - рисуется как есть, без наложения кадра анимации.
        builder.append(Component.text(levelIconChar.toString()).font(levelIconFont))

        builder.append(Component.text(SpaceGlyphs.negSpace(finalReturn)))

        return builder.build()
    }

    private fun isAnimating(animStart: Long, now: Long, durationMs: Long): Boolean {
        if (animStart < 0) return false
        return now - animStart < durationMs
    }

    private fun frameFor(
        frames: List<Char>,
        staticFrame: Char,
        animStart: Long,
        now: Long,
        durationMs: Long
    ): Char {
        if (animStart < 0) return staticFrame

        val elapsed = now - animStart
        if (elapsed >= durationMs) return staticFrame

        if (durationMs <= 0L || frames.isEmpty()) return staticFrame
        val stepMs = (durationMs / frames.size).coerceAtLeast(1L)
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