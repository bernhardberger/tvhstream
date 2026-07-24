package cz.preclikos.tvhstream.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BackNavigationPolicyTest {
    @Test
    fun rootStartDestinationFinishesActivity() {
        assertEquals(
            BackAction.FINISH_ACTIVITY,
            rootBackAction(isStartDestination = true),
        )
    }

    @Test
    fun rootChildDestinationPopsNavigation() {
        assertEquals(
            BackAction.POP_NAVIGATION,
            rootBackAction(isStartDestination = false),
        )
    }

    @Test
    fun nestedStartDestinationReturnsToParentGraph() {
        assertEquals(
            BackAction.RETURN_TO_PARENT,
            nestedBackAction(hasPreviousEntry = false),
        )
    }

    @Test
    fun nestedChildDestinationPopsNestedNavigation() {
        assertEquals(
            BackAction.POP_NAVIGATION,
            nestedBackAction(hasPreviousEntry = true),
        )
    }
}
