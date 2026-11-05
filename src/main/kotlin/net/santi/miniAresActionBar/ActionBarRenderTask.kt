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
    // a (a1.png) - статичный кадр, остальные проигрываются при росте опыта.
    private val expFrames = ('a'..'p').toList()

    // Статичная иконка уровня в покое: 'z' из actionbar_1.json (font/level_icon.png).
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

        // --- Сдвиг actionbar влево обычными пробелами ---
        // Ванильный actionbar центрируется по ширине текста. Чтобы визуально
        // увести контент ВЛЕВО, добавляем пробелы В КОНЕЦ строки: они расширяют
        // строку справа, и точка центрирования уезжает вправо, а видимый
        // контент - влево. Если нужно наоборот (вправо) - увеличь leading-spaces.
        val leadingSpaces = plugin.config.getInt("actionbar-align.leading-spaces", 0)
        val trailingSpaces = plugin.config.getInt("actionbar-align.trailing-spaces", 40)

        val leadingPad = if (leadingSpaces > 0) " ".repeat(leadingSpaces) else ""
        val trailingPad = if (trailingSpaces > 0) " ".repeat(trailingSpaces) else ""

        val now = System.currentTimeMillis()

        for (player in onlinePlayers) {
            val state = BalanceStateManager.get(player.uniqueId)

            val aresIcon = frameFor(aresFrames, state.aresAnimStart, now, animDurationMs)
            val dollarIcon = frameFor(dollarFrames, state.dollarAnimStart, now, animDurationMs)

            // Иконка уровня: при росте опыта проигрываем a..p из actionbar.json,
            // в покое рисуем статичный 'z' из actionbar_1.json.
            val levelExpDuration = levelMetrics.expAnimationDurationMs()
            val levelIcon: Char
            val levelIconFont: Key
            if (isAnimating(state.levelIconAnimStart, now, levelExpDuration)) {
                levelIcon = frameFor(expFrames, state.levelIconAnimStart, now, levelExpDuration)
                levelIconFont = iconFont
            } else {
                levelIcon = levelIconStaticChar
                levelIconFont = levelIconStaticFont
            }

            // Доллары печатаются первыми (верхний блок), аресы вторыми (нижний) -
            // вертикальное разведение даёт ascent в actionbar_1/2.json (уже разный),
            // а горизонтальное выравнивание под одну X - метод money.js ниже.
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
                componentBuilder.append(buildLevelBlock(state.level, state.xpPercent, levelIcon, levelIconFont))
            }

            componentBuilder.append(Component.text(trailingPad))

            player.sendActionBar(componentBuilder.build())
        }
    }

    /**
     * Метод из money.js: shift(калибровка) + negSpace(ширинаБлока) + блок + negSpace(калибровка).
     * За счёт этого курсор после блока возвращается ровно в ту же X-точку, с которой блок
     * начал печататься - следующий блок (например ares после dollars) стартует с того же X.
     *
     * Цвет нигде не задаётся - рендерится цветом по умолчанию (белый).
     */
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
     * Блок уровня сервера: шкала опыта + число уровня + иконка.
     * Дословный перенос result() из старого level.js (JS-expansion PlaceholderAPI)
     * на Component-API и SpaceGlyphs/LevelMetrics.
     *
     * Важно: в оригинальном level.js getPosSpace()/getNegSpace() НЕ смотрят на знак
     * входного числа (внутри всегда abs()) - направление сдвига всегда то, которое
     * диктует конкретный вызов (posSpace -> вправо, negSpace -> влево), а не знак
     * переданной величины. Поэтому здесь используются SpaceGlyphs.posSpace()/negSpace()
     * напрямую, а не SpaceGlyphs.shift() (который выбирает направление по знаку).
     *
     * iconChar/iconFont - в покое статичный 'z' из actionbar_1.json, при росте опыта -
     * текущий кадр анимации a..p из actionbar.json (см. run()).
     */
    private fun buildLevelBlock(level: Int, xpPercent: Double, iconChar: Char, iconFont: Key): Component {
        val lvlText = level.toString()
        val barChar = LevelBarGlyphs.barChar(xpPercent)
        val digitFont = levelMetrics.digitFont()

        val iconSpacing = levelMetrics.iconSpacing()
        val iconWidth = levelMetrics.iconWidth()
        val margin = levelMetrics.margin()
        val barNudge = levelMetrics.barNudge()
        val barWidth = levelMetrics.barWidth()
        val digitNudge = levelMetrics.digitNudge()

        val totalTextWidth = levelMetrics.textWidth(lvlText) + iconSpacing + iconWidth
        val centerOffset = (barWidth - totalTextWidth) / 2.0

        // Курсор после печати шкалы и возврата (negSpace(barWidth+barNudge))
        // оказывается на X0+margin (X0 - точка сразу после ares-блока), а не
        // на X0 - это и есть базовая точка, от которой считается centerOffset
        // ниже. Финальный negSpace должен вернуть курсор обратно в X0, поэтому
        // из него нужно вычесть digitNudge - ровно столько, сколько "украл"
        // лишний negSpace(digitNudge) перед группой цифр+иконки.
        val finalReturn = margin + centerOffset + totalTextWidth - digitNudge

        return Component.text()
            // Шкала опыта, со своим локальным сдвигом (barNudge) - digitNudge
            // её НЕ затрагивает, поэтому шкала остаётся на месте.
            .append(Component.text(SpaceGlyphs.posSpace(margin + barNudge)))
            .append(Component.text(barChar.toString()).font(levelMetrics.xpBarFont()))
            .append(Component.text(SpaceGlyphs.negSpace(barWidth + barNudge)))
            // Группа "число уровня + иконка" целиком сдвигается влево на
            // digitNudge, отцентрованная под шкалой величиной centerOffset.
            .append(Component.text(SpaceGlyphs.negSpace(digitNudge)))
            .append(Component.text(SpaceGlyphs.posSpace(centerOffset)))
            .append(glyphText(lvlText, digitFont))
            .append(Component.text(SpaceGlyphs.posSpace(iconSpacing)))
            .append(Component.text(iconChar.toString()).font(iconFont))
            // Полный возврат в ноль для корректной работы следующих блоков
            .append(Component.text(SpaceGlyphs.negSpace(finalReturn)))
            .build()
    }

    /**
     * true, если анимация запущена и ещё не истекла (animStart >= 0 и прошло меньше durationMs).
     */
    private fun isAnimating(animStart: Long, now: Long, durationMs: Long): Boolean {
        if (animStart < 0) return false
        return now - animStart < durationMs
    }

    /**
     * Возвращает кадр для текущего момента: если анимация не запущена или уже истекла -
     * первый кадр (статичная иконка), иначе кадр, соответствующий прошедшей доле времени анимации.
     */
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