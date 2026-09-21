package com.fonamp.core.player

import androidx.media3.common.Player

/**
 * Repeat behavior (player Req 3): OFF stops at the queue end, ALL wraps
 * around, ONE loops the current item. Cycles OFF → ALL → ONE → OFF on a
 * single control.
 */
enum class RepeatMode {
    OFF,
    ALL,
    ONE;

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    };

    fun toMedia3(): Int = when (this) {
        OFF -> Player.REPEAT_MODE_OFF
        ALL -> Player.REPEAT_MODE_ALL
        ONE -> Player.REPEAT_MODE_ONE
    }

    companion object {
        fun fromMedia3(mode: Int): RepeatMode = when (mode) {
            Player.REPEAT_MODE_ALL -> ALL
            Player.REPEAT_MODE_ONE -> ONE
            else -> OFF
        }
    }
}
