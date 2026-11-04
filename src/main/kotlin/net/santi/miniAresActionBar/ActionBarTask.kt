package net.santi.miniAresActionBar

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit

class ActionBarTask(private val plugin: Main) : org.bukkit.scheduler.BukkitRunnable() {

    // Кадры анимации иконки ареса: A..K -> others/ares_anim/0..10.png (шрифт "actionbar")
    private val aresFrames = ('A'..'K').toList()

    // Кадры анимации иконки доллара: L..V -> others/dollar_anim/0..10.png (шрифт "actionbar")
    private val dollarFrames = ('L'..'V').toList()

    private var frame = 0

    override fun run() {
        val namespace = plugin.config.getString("font-namespace", "mineares")!!
        val dollarDigitFontName = plugin.config.getString("dollar-digit-font", "actionbar_1")!!
        val aresDigitFontName = plugin.config.getString("ares-digit-font", "actionbar_2")!!

        val iconFont = Key.key(namespace, "actionbar")
        val dollarDigitFont = Key.key(namespace, dollarDigitFontName)
        val aresDigitFont = Key.key(namespace, aresDigitFontName)

        val aresIcon = aresFrames[frame % aresFrames.size]
        val dollarIcon = dollarFrames[frame % dollarFrames.size]
        frame++

        val onlinePlayers = Bukkit.getOnlinePlayers()
        if (onlinePlayers.isEmpty()) return

        for (player in onlinePlayers) {
            val rawAres = PlaceholderHook.resolve(player, "%economy_ares_raw%")
            val rawDollars = PlaceholderHook.resolve(player, "%economy_dollars_raw%")

            val component = Component.text()
                .append(Component.text(aresIcon.toString()).font(iconFont))
                .append(Component.text(" "))
                .append(glyphText(BalanceFormatter.format(rawAres), aresDigitFont, NamedTextColor.GOLD))
                .append(Component.text("   "))
                .append(Component.text(dollarIcon.toString()).font(iconFont))
                .append(Component.text(" "))
                .append(glyphText(BalanceFormatter.format(rawDollars), dollarDigitFont, NamedTextColor.GREEN))
                .build()

            player.sendActionBar(component)
        }
    }

    private fun glyphText(text: String, font: Key, color: NamedTextColor): Component {
        val builder = Component.text()
        for (c in text) {
            builder.append(Component.text(c.toString(), color).font(font))
        }
        return builder.build()
    }
}