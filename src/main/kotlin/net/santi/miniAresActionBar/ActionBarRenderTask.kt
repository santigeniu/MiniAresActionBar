package net.santi.miniAresActionBar

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit

class ActionBarRenderTask(private val plugin: Main) : org.bukkit.scheduler.BukkitRunnable() {

    // Кадры анимации иконки ареса: A..K -> others/ares_anim/0..10.png. A (0.png) - статичный кадр.
    private val aresFrames = ('A'..'K').toList()

    // Кадры анимации иконки доллара: L..V -> others/dollar_anim/0..10.png. L (0.png) - статичный кадр.
    private val dollarFrames = ('L'..'V').toList()

    private val metrics = GlyphMetrics(plugin)

    override fun run() {
        val onlinePlayers = Bukkit.getOnlinePlayers()
        if (onlinePlayers.isEmpty()) return

        val namespace = plugin.config.getString("font-namespace", "mineares")!!
        val dollarDigitFont = Key.key(namespace, plugin.config.getString("dollar-digit-font", "actionbar_1")!!)
        val aresDigitFont = Key.key(namespace, plugin.config.getString("ares-digit-font", "actionbar_2")!!)
        val iconFont = Key.key(namespace, "actionbar")
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

            val component = Component.text()
                .append(Component.text(leadingPad))
                .append(dollarBlock)
                .append(aresBlock)
                .append(Component.text(trailingPad))
                .build()

            player.sendActionBar(component)
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