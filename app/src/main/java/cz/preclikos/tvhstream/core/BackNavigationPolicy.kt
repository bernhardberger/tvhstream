package cz.preclikos.tvhstream.core

enum class BackAction {
    POP_NAVIGATION,
    RETURN_TO_PARENT,
    FINISH_ACTIVITY,
}

fun rootBackAction(isStartDestination: Boolean): BackAction =
    if (isStartDestination) BackAction.FINISH_ACTIVITY else BackAction.POP_NAVIGATION

fun nestedBackAction(hasPreviousEntry: Boolean): BackAction =
    if (hasPreviousEntry) BackAction.POP_NAVIGATION else BackAction.RETURN_TO_PARENT
