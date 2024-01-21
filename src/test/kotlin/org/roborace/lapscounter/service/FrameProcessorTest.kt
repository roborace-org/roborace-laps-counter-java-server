package org.roborace.lapscounter.service

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.roborace.lapscounter.domain.Robot
import org.roborace.lapscounter.domain.Type

internal class FrameProcessorTest {
    private var frameProcessor = FrameProcessor(safeInterval = 1000, frames = FRAMES)

    private val robot: Robot = Robot(serial = 50)

    @BeforeEach
    fun setUp() {
        frameProcessor.robotInit(robot.serial)
    }

    @Test
    fun testErrorsByTime() {
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 1), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 0), equalTo(Type.ERROR))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 100), equalTo(Type.ERROR))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 50), equalTo(Type.ERROR))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 999), equalTo(Type.ERROR))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 1001), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 1100), equalTo(Type.ERROR))
    }


    @Test
    fun testResetTime() {
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 1000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 2000), equalTo(Type.FRAME))
        frameProcessor.reset()
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 1000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 2000), equalTo(Type.FRAME))
    }

    @Test
    fun testResetLaps() {
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 1000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 2000), equalTo(Type.FRAME))
        frameProcessor.reset()
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 3000), equalTo(Type.WRONG_FRAME))
    }


    @Test
    fun testWrongFrame() {
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 1000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, 0xBB00, 3000), equalTo(Type.ERROR))
    }

    @Test
    fun testDuplicateFrame() {
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 1000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 2000), equalTo(Type.DUPLICATE_FRAME))
    }

    @Test
    fun testFrameWrongRotate() {
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 1000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 2000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 3000), equalTo(Type.LAP_MINUS))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 4000), equalTo(Type.WRONG_ROTATION))
    }

    @Test
    fun testFrameWrongRotateAfterThreeFrames() {
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 1000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 2000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 3000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 4000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 5000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 6000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 7000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 8000), equalTo(Type.LAP_MINUS))
    }

    @Test
    fun testFrameWrongRotateThreeLaps() {
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 1000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 2000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 3000), equalTo(Type.LAP_MINUS))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 4000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 5000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 6000), equalTo(Type.LAP_MINUS))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 7000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 8000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 9000), equalTo(Type.LAP_MINUS))
    }

    @Test
    fun testThreeFrames() {
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 0), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 1000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 2000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 3000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 4000), equalTo(Type.LAP))
    }

    @Test
    fun testTwoLaps() {
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 1000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 2000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 3000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 4000), equalTo(Type.LAP))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 5000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 6000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 7000), equalTo(Type.LAP))
    }

    @Test
    fun testSleepAfterFirstLap() {
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 0), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 1000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 2000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 3000), equalTo(Type.LAP))

        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 4000), equalTo(Type.WRONG_ROTATION))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 5000), equalTo(Type.DUPLICATE_FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 6000), equalTo(Type.DUPLICATE_FRAME))

        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 7000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 8000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 9000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 10000), equalTo(Type.LAP))
    }

    @Test
    fun testSkipFrame() {
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 1000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 3000), equalTo(Type.WRONG_ROTATION))
    }

    @Test
    fun testLapsTwoRobots() {
        val robot2 = Robot(serial = 70)
        frameProcessor.robotInit(robot2.serial)

        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 1000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 2000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot2.serial, FRAME_0, 2100), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot2.serial, FRAME_1, 3100), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot2.serial, FRAME_2, 4100), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_2, 5100), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot2.serial, FRAME_0, 6200), equalTo(Type.LAP))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 7200), equalTo(Type.LAP))
    }

    @Test
    fun testLapsOneFrame() {
        frameProcessor = FrameProcessor(1000, listOf(FRAME_0))
        frameProcessor.robotInit(robot.serial)

        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_1, 1000), equalTo(Type.ERROR))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 1000), equalTo(Type.FRAME))
        assertThat(frameProcessor.checkFrame(robot.serial, FRAME_0, 2000), equalTo(Type.LAP))
    }

    companion object {
        private const val FRAME_0 = 0xAA00
        private const val FRAME_1 = 0xAA01
        private const val FRAME_2 = 0xAA02
        private val FRAMES = listOf(FRAME_0, FRAME_1, FRAME_2)
    }
}