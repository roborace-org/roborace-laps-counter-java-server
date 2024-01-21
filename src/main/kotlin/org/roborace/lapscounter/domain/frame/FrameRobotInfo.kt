package org.roborace.lapscounter.domain.frame

data class FrameRobotInfo(
    val frames: MutableList<Int> = mutableListOf(),
    var lastFrameTime: Long = 0,
) {

    val lastFrame: Int?
        get() = frames.lastOrNull()

    fun placeFrame(raceTime: Long, frame: Int, isFinishFrame: Boolean = true) {
        lastFrameTime = raceTime
        if (!isFinishFrame && frames.contains(frame)) {
            removeExtraFrames(frame)
        } else {
            frames.add(frame)
        }
    }

    fun reset() {
        frames.clear()
        lastFrameTime = 0
    }

    private fun removeExtraFrames(frame: Int) {
        while (frames.last() != frame) frames.removeLast()
    }
}
