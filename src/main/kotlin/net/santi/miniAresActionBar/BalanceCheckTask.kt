package net.santi.miniAresActionBar

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class BalanceCheckTask(private val plugin: Main) : BukkitRunnable() {

    private val levelMetrics = LevelMetrics(plugin)

    private var lastDebugLog = 0L

    private companion object {
        const val LEVEL_PLACEHOLDER = "%lamglevels_level%"
        const val EXP_PLACEHOLDER = "%lamglevels_exp_progress%"
    }

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
            val rawLevel = PlaceholderHook.resolve(player, LEVEL_PLACEHOLDER)
            val rawExp = PlaceholderHook.resolve(player, EXP_PLACEHOLDER)

            val level = LevelFormatter.parseLevel(rawLevel)
            val exp = LevelFormatter.parseExp(rawExp)
            val goal = levelMetrics.levelGoal(level)
            val percent = LevelFormatter.xpPercent(exp, goal)

            state.level = level
            state.xpPercent = percent

            // Диагностика (level.debug: true в config.yml): раз в 5 сек пишет в консоль
            // сырые значения плейсхолдеров и всё, что из них посчиталось, для одного игрока.
            if (levelMetrics.debug() && now - lastDebugLog >= 5000L) {
                lastDebugLog = now
                val levelNote = if (rawLevel == LEVEL_PLACEHOLDER) " (ПЛЕЙСХОЛДЕР НЕ РАСКРЫТ)" else ""
                val expNote = if (rawExp == EXP_PLACEHOLDER) " (ПЛЕЙСХОЛДЕР НЕ РАСКРЫТ)" else ""
                plugin.logger.info(
                    "[level-debug] ${player.name}: " +
                        "level raw='$rawLevel'$levelNote -> $level | " +
                        "exp raw='$rawExp'$expNote -> $exp | " +
                        "goal=$goal | percent=$percent | bar char='${LevelBarGlyphs.barChar(percent)}'"
                )
            }
        }
    }
}
