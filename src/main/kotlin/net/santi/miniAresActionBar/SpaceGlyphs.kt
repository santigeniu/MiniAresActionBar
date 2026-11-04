package net.santi.miniAresActionBar

import kotlin.math.abs
import kotlin.math.floor

/**
 * Аналог getNegSpace()/getPosSpace()/shift() из money.js.
 * Предполагается, что символы \uF801-\uF808 (сдвиг влево) и \uF821-\uF828
 * (сдвиг вправо), с шириной по степеням двойки (1,2,4,8,16,32,64,128 px),
 * уже определены в ресурспаке (как для money.js) - здесь они используются
 * как обычный текст на дефолтном шрифте, без .font() override.
 */
object SpaceGlyphs {

    private val NEG_PAIRS = listOf(
        128 to '\uF808', 64 to '\uF807', 32 to '\uF806', 16 to '\uF805',
        8 to '\uF804', 4 to '\uF803', 2 to '\uF802', 1 to '\uF801'
    )

    private val POS_PAIRS = listOf(
        128 to '\uF828', 64 to '\uF827', 32 to '\uF826', 16 to '\uF825',
        8 to '\uF824', 4 to '\uF823', 2 to '\uF822', 1 to '\uF821'
    )

    fun negSpace(width: Double): String = buildSpacer(width, NEG_PAIRS)

    fun posSpace(width: Double): String = buildSpacer(width, POS_PAIRS)

    // + вправо, - влево (как shift() в money.js)
    fun shift(px: Double): String = if (px < 0) negSpace(-px) else posSpace(px)

    private fun buildSpacer(width: Double, pairs: List<Pair<Int, Char>>): String {
        var w = floor(abs(width)).toInt()
        val sb = StringBuilder()
        for ((power, char) in pairs) {
            if (w >= power) {
                sb.append(char)
                w -= power
            }
        }
        return sb.toString()
    }
}