package org.roborace.lapscounter.domain

enum class Type {
    COMMAND,
    STATE,
    ROBOT_INIT,
    ROBOT_EDIT,
    ROBOT_REMOVE,
    TIME,
    LAPS,
    LAP,
    LAP_MINUS,
    LAP_MAN,
    PIT_STOP,
    PIT_STOP_FINISH,
    FRAME,
    ERROR,
    WRONG_FRAME,
    WRONG_ROTATION,
    DUPLICATE_FRAME
}
