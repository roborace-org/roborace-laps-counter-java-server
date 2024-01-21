package org.roborace.lapscounter.service

import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.frame.FrameRobotInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class FrameProcessor(
    @param:Value("\${laps.safe-interval}") private val safeInterval: Long,
    @param:Value("\${laps.frames}") private val frames: List<Int>
) {
    private val frameInfoBySerialMap: MutableMap<Int, FrameRobotInfo> = mutableMapOf()

    private val reversedFrames: List<Int> = frames.sortedDescending()


    fun robotInit(serial: Int) {
        frameInfoBySerialMap[serial] = FrameRobotInfo()
    }

    fun robotRemove(serial: Int) = frameInfoBySerialMap.remove(serial)

    fun reset() = frameInfoBySerialMap.values.forEach { it.reset() }

    fun checkFrame(serial: Int, frame: Int, raceTime: Long) =
        getFrameResult(serial, frame, raceTime).also {
            log.info("Frame result: {}, {}, robot: {}", it, frame, serial)
        }

    fun isStartFrame(frame: Int) = frames[0] == frame

    private fun getFrameResult(serial: Int, frame: Int, raceTime: Long): Type {
        if (!frames.contains(frame)) {
            log.warn("Frame not found: {}, robot: {}", frame, serial)
            return Type.ERROR
        }

        val frameRobotInfo = frameInfoBySerialMap[serial]

        return when {
            frameRobotInfo == null ->
                Type.ERROR.also { log.warn("Robot [{}] is not init", serial) }

            isTooQuick(raceTime, frameRobotInfo.lastFrameTime) && frameRobotInfo.frames.isNotEmpty() ->
                Type.ERROR.also {
                    log.warn("Frame is not counted (too quick): {}, robot: {}", frame, serial)
                }

            else -> checkFrame(frameRobotInfo, frame, raceTime)
        }
    }

    private fun checkFrame(frameRobotInfo: FrameRobotInfo, frame: Int, raceTime: Long): Type {
        val lastFrame = frameRobotInfo.lastFrame
        val robotFrames = frameRobotInfo.frames
        val isFinishFrame = frame == frames[0]

        frameRobotInfo.placeFrame(raceTime, frame, isFinishFrame)

        if (isFinishFrame) {
            val allFrames = allFrames(robotFrames)
            val wrongDirection = allFramesWrongDirection(robotFrames)

            robotFrames.clear()
            frameRobotInfo.placeFrame(raceTime, frame)

            when {
                allFrames -> return Type.LAP
                wrongDirection -> return Type.LAP_MINUS
            }
        }

        return when {
            isNextRobotFrame(frame, lastFrame) -> Type.FRAME
            isLastRobotFrame(frame, lastFrame) -> Type.DUPLICATE_FRAME
            isPreviousRobotFrame(frame, lastFrame) -> Type.WRONG_ROTATION
            else -> Type.WRONG_FRAME
        }
    }

    private fun isNextRobotFrame(frame: Int, lastFrame: Int?) =
        frame == getExpectedNextFrame(lastFrame)

    private fun isLastRobotFrame(frame: Int, lastFrame: Int?) =
        frame == lastFrame

    private fun isPreviousRobotFrame(frame: Int, lastFrame: Int?) =
        frame == getExpectedPrevFrame(lastFrame)

    private fun getExpectedNextFrame(lastFrame: Int?) =
        lastFrame?.let {
            val lastFrameIndex = frames.indexOf(lastFrame)
            val nextIndex = (lastFrameIndex + 1) % frames.size
            frames[nextIndex]
        } ?: frames.first()

    private fun getExpectedPrevFrame(lastFrame: Int?): Int =
        lastFrame?.let {
            val lastFrameIndex = frames.indexOf(lastFrame)
            val prevIndex = (lastFrameIndex - 1 + frames.size) % frames.size
            return frames[prevIndex]
        } ?: frames.last()

    private fun allFrames(robotFrames: List<Int>) = hasSubsequence(robotFrames, frames)

    private fun allFramesWrongDirection(robotFrames: List<Int>) =
        hasSubsequence(robotFrames, reversedFrames)

    private fun hasSubsequence(robotFrames: List<Int>, search: List<Int>): Boolean {
        val robotFramesIterator = robotFrames.iterator()
        val searchIterator = search.iterator()

        while (searchIterator.hasNext())
            if (!find(searchIterator.next(), robotFramesIterator)) return false

        val hasFinishAndStart = robotFrames.size >= 2
        return hasFinishAndStart
    }

    private fun find(search: Int, iterator: Iterator<Int>): Boolean {
        while (iterator.hasNext()) {
            if (iterator.next() == search) return true
        }
        return false
    }

    private fun isTooQuick(raceTime: Long, lastFrameTime: Long) =
        raceTime < lastFrameTime + safeInterval

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FrameProcessor::class.java)
    }
}
