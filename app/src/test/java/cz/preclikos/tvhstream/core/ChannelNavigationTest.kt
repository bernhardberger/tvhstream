package cz.preclikos.tvhstream.core

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelNavigationTest {

    private val channels = listOf(10, 20, 30)
    private val numberedChannels = listOf<Pair<Int, Int?>>(10 to 7, 20 to 12, 30 to 103)

    @Test
    fun next_returnsFollowingChannel() {
        assertEquals(20, ChannelNavigation.adjacentId(channels, 10, 1))
    }

    @Test
    fun previous_returnsPrecedingChannel() {
        assertEquals(20, ChannelNavigation.adjacentId(channels, 30, -1))
    }

    @Test
    fun next_wrapsAfterLastChannel() {
        assertEquals(10, ChannelNavigation.adjacentId(channels, 30, 1))
    }

    @Test
    fun previous_wrapsBeforeFirstChannel() {
        assertEquals(30, ChannelNavigation.adjacentId(channels, 10, -1))
    }

    @Test
    fun staleCurrentChannel_fallsBackToFirstCurrentChannel() {
        assertEquals(10, ChannelNavigation.adjacentId(channels, 99, 1))
        assertEquals(10, ChannelNavigation.adjacentId(channels, 99, -1))
    }

    @Test
    fun emptyChannelList_hasNoAdjacentChannel() {
        assertNull(ChannelNavigation.adjacentId(emptyList(), 10, 1))
    }

    @Test
    fun channelKeys_mapToNavigationDirection() {
        assertEquals(1, ChannelNavigation.directionForKeyCode(KeyEvent.KEYCODE_CHANNEL_UP))
        assertEquals(-1, ChannelNavigation.directionForKeyCode(KeyEvent.KEYCODE_CHANNEL_DOWN))
    }

    @Test
    fun unrelatedKey_hasNoNavigationDirection() {
        assertNull(ChannelNavigation.directionForKeyCode(KeyEvent.KEYCODE_DPAD_UP))
    }

    @Test
    fun numberKeys_mapToDigits() {
        assertEquals(0, ChannelNavigation.digitForKeyCode(KeyEvent.KEYCODE_0))
        assertEquals(5, ChannelNavigation.digitForKeyCode(KeyEvent.KEYCODE_5))
        assertEquals(9, ChannelNavigation.digitForKeyCode(KeyEvent.KEYCODE_9))
        assertEquals(0, ChannelNavigation.digitForKeyCode(KeyEvent.KEYCODE_NUMPAD_0))
        assertEquals(5, ChannelNavigation.digitForKeyCode(KeyEvent.KEYCODE_NUMPAD_5))
        assertEquals(9, ChannelNavigation.digitForKeyCode(KeyEvent.KEYCODE_NUMPAD_9))
    }

    @Test
    fun unrelatedKey_hasNoDigit() {
        assertNull(ChannelNavigation.digitForKeyCode(KeyEvent.KEYCODE_DPAD_UP))
    }

    @Test
    fun digits_appendUntilThreeDigitLimit() {
        assertEquals("1", ChannelNavigation.appendDigit("", 1))
        assertEquals("12", ChannelNavigation.appendDigit("1", 2))
        assertEquals("123", ChannelNavigation.appendDigit("12", 3))
    }

    @Test
    fun digitAfterThreeDigits_startsNewEntry() {
        assertEquals("4", ChannelNavigation.appendDigit("123", 4))
    }

    @Test
    fun enteredNumber_selectsMatchingTvheadendChannelNumber() {
        assertEquals(10, ChannelNavigation.idForNumber(numberedChannels, "7"))
        assertEquals(20, ChannelNavigation.idForNumber(numberedChannels, "012"))
        assertEquals(30, ChannelNavigation.idForNumber(numberedChannels, "103"))
    }

    @Test
    fun invalidEnteredNumber_hasNoChannel() {
        assertNull(ChannelNavigation.idForNumber(numberedChannels, "0"))
        assertNull(ChannelNavigation.idForNumber(numberedChannels, "1"))
        assertNull(ChannelNavigation.idForNumber(numberedChannels, ""))
    }
}
