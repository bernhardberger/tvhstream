package cz.preclikos.tvhstream.core

import android.view.KeyEvent

object ChannelNavigation {
    private const val MAX_CHANNEL_NUMBER_DIGITS = 3

    fun directionForKeyCode(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_CHANNEL_UP -> 1
        KeyEvent.KEYCODE_CHANNEL_DOWN -> -1
        else -> null
    }

    fun digitForKeyCode(keyCode: Int): Int? = when (keyCode) {
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> keyCode - KeyEvent.KEYCODE_0
        in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 ->
            keyCode - KeyEvent.KEYCODE_NUMPAD_0
        else -> null
    }

    fun appendDigit(current: String, digit: Int): String {
        require(digit in 0..9)
        return if (current.length >= MAX_CHANNEL_NUMBER_DIGITS) {
            digit.toString()
        } else {
            current + digit
        }
    }

    fun idForNumber(orderedIds: List<Int>, enteredNumber: String): Int? {
        val number = enteredNumber.toIntOrNull() ?: return null
        return orderedIds.getOrNull(number - 1)
    }

    fun numberForId(orderedIds: List<Int>, channelId: Int): Int? {
        val index = orderedIds.indexOf(channelId)
        return if (index >= 0) index + 1 else null
    }

    fun adjacentId(
        orderedIds: List<Int>,
        currentId: Int,
        direction: Int,
    ): Int? {
        if (orderedIds.isEmpty()) return null

        val currentIndex = orderedIds.indexOf(currentId)
        if (currentIndex < 0) return orderedIds.first()

        val offset = if (direction < 0) -1 else 1
        return orderedIds[Math.floorMod(currentIndex + offset, orderedIds.size)]
    }
}
