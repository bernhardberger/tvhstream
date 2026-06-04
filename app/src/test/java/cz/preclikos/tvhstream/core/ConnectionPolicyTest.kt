package cz.preclikos.tvhstream.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionPolicyTest {

    @Test
    fun autoConnect_requiresHostAndPort_only() {
        assertTrue(ConnectionPolicy.isAutoConnectReady("tvh.local", 9982))
        // Unauthenticated server: host + port is enough, no credentials needed.
        assertTrue(ConnectionPolicy.isAutoConnectReady("192.168.1.10", 9982))
    }

    @Test
    fun autoConnect_notReady_whenHostBlankOrPortZero() {
        assertFalse(ConnectionPolicy.isAutoConnectReady("", 9982))
        assertFalse(ConnectionPolicy.isAutoConnectReady("   ", 9982))
        assertFalse(ConnectionPolicy.isAutoConnectReady("tvh.local", 0))
    }

    @Test
    fun authenticate_onlyWhenBothCredentialsPresent() {
        assertTrue(ConnectionPolicy.shouldAuthenticate("user", "pass"))
    }

    @Test
    fun authenticate_skipped_forUnauthenticatedServer() {
        // Issue #3: empty credentials must connect without an auth step.
        assertFalse(ConnectionPolicy.shouldAuthenticate("", ""))
        assertFalse(ConnectionPolicy.shouldAuthenticate(null, null))
        assertFalse(ConnectionPolicy.shouldAuthenticate("user", ""))
        assertFalse(ConnectionPolicy.shouldAuthenticate("", "pass"))
        assertFalse(ConnectionPolicy.shouldAuthenticate("  ", "  "))
    }
}
