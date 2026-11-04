package net.santi.miniAresActionBar

data class PlayerBalanceState(
    var lastDollarsRaw: Double = Double.NaN,
    var lastAresRaw: Double = Double.NaN,
    var dollarsFormatted: String = "0",
    var aresFormatted: String = "0",
    // -1L значит "анимация не идёт", иначе System.currentTimeMillis() момента старта
    var dollarAnimStart: Long = -1L,
    var aresAnimStart: Long = -1L
)