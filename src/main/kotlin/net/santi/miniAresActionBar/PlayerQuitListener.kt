package net.santi.miniAresActionBar

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PlayerQuitListener : Listener {

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        BalanceStateManager.remove(event.player.uniqueId)
    }
}