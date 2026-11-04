package net.santi.miniAresActionBar

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Небольшая обёртка, чтобы не падать, если PlaceholderAPI не установлен на сервере.
 */
object PlaceholderHook {

    val isEnabled: Boolean
        get() = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null

    fun resolve(player: Player, placeholder: String): String {
        if (!isEnabled) return "0"
        return PlaceholderAPI.setPlaceholders(player, placeholder)
    }
}