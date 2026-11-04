package net.santi.miniAresActionBar

import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {

    private var checkTask: BalanceCheckTask? = null
    private var renderTask: ActionBarRenderTask? = null

    override fun onEnable() {
        saveDefaultConfig()

        if (!PlaceholderHook.isEnabled) {
            logger.warning("PlaceholderAPI не найден — плейсхолдеры баланса не будут работать (будет выводиться \"0\").")
        }

        server.pluginManager.registerEvents(PlayerQuitListener(), this)

        val checkInterval = config.getLong("balance-check-interval-ticks", 20L)
        val renderInterval = config.getLong("render-interval-ticks", 2L)

        checkTask = BalanceCheckTask(this)
        checkTask?.runTaskTimer(this, 0L, checkInterval)

        renderTask = ActionBarRenderTask(this)
        renderTask?.runTaskTimer(this, 0L, renderInterval)
    }

    override fun onDisable() {
        checkTask?.cancel()
        renderTask?.cancel()
        checkTask = null
        renderTask = null
    }
}