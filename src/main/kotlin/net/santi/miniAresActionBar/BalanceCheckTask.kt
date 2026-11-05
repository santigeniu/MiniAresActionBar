package net.santi.miniAresActionBar

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class BalanceCheckTask(private val plugin: Main) : BukkitRunnable() {

    override fun run() {
        val onlinePlayers = Bukkit.getOnlinePlayers()
        if (onlinePlayers.isEmpty()) return

        val now = System.currentTimeMillis()

        for (player in onlinePlayers) {
            val state = BalanceStateManager.get(player.uniqueId)

            val rawDollars = PlaceholderHook.resolve(player, "%economy_dollars_raw%")
            val rawAres = PlaceholderHook.resolve(player, "%economy_ares_raw%")

            val dollarsValue = rawDollars.replace(",", ".").toDoubleOrNull()
            val aresValue = rawAres.replace(",", ".").toDoubleOrNull()

            if (dollarsValue != null) {
                // lastDollarsRaw.isNaN() == true на первом опросе после захода игрока —
                // анимацию в этот момент не запускаем, просто запоминаем стартовое значение
                if (!state.lastDollarsRaw.isNaN() && dollarsValue > state.lastDollarsRaw) {
                    state.dollarAnimStart = now
                }
                state.lastDollarsRaw = dollarsValue
            }

            if (aresValue != null) {
                if (!state.lastAresRaw.isNaN() && aresValue > state.lastAresRaw) {
                    state.aresAnimStart = now
                }
                state.lastAresRaw = aresValue
            }

            state.dollarsFormatted = BalanceFormatter.format(rawDollars)
            state.aresFormatted = BalanceFormatter.format(rawAres)

            // --- Уровень сервера (раньше - js-expansion level.js в PlaceholderAPI) ---
            val rawLevel = PlaceholderHook.resolve(player, "%lamglevels_level%")
            val rawExp = PlaceholderHook.resolve(player, "%lamglevels_exp_progress%")

            val level = LevelFormatter.parseLevel(rawLevel)
            val exp = LevelFormatter.parseExp(rawExp)

            state.level = level
            state.xpPercent = LevelFormatter.xpPercent(level, exp)
        }
    }
}
