package net.santi.miniAresActionBar

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BalanceStateManager {

    private val states = ConcurrentHashMap<UUID, PlayerBalanceState>()

    fun get(uuid: UUID): PlayerBalanceState =
        states.computeIfAbsent(uuid) { PlayerBalanceState() }

    fun remove(uuid: UUID) {
        states.remove(uuid)
    }
}