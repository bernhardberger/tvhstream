package cz.preclikos.tvhstream.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiSettingsTest {

    @Test
    fun `epg menu is shown when no preference has been saved`() {
        assertTrue(resolveEpgMenuVisibility(null))
    }

    @Test
    fun `saved epg menu preference is respected`() {
        assertFalse(resolveEpgMenuVisibility(false))
        assertTrue(resolveEpgMenuVisibility(true))
    }
}
