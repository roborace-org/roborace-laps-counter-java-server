package org.roborace.lapscounter.domain.frame

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FrameRobotInfoTest {
    private lateinit var frameRobotInfo: FrameRobotInfo

    @BeforeEach
    fun setUp() {
        frameRobotInfo = FrameRobotInfo()
    }

    @Test
    fun testInitialState() {
        assertThat(frameRobotInfo.frames, hasSize(0))
        assertThat(frameRobotInfo.lastFrameTime, equalTo(0L))
        assertThat(frameRobotInfo.lastFrame, nullValue())
    }

    @Test
    fun testPlaceFrameFinish() {
        frameRobotInfo.placeFrame(1000L, FRAME_0, isFinishFrame = true)

        assertThat(frameRobotInfo.frames, hasSize(1))
        assertThat(frameRobotInfo.frames[0], equalTo(FRAME_0))
        assertThat(frameRobotInfo.lastFrameTime, equalTo(1000L))
        assertThat(frameRobotInfo.lastFrame, equalTo(FRAME_0))
    }

    @Test
    fun testPlaceFrameNonFinish() {
        frameRobotInfo.placeFrame(1000L, FRAME_0, isFinishFrame = false)

        assertThat(frameRobotInfo.frames, hasSize(1))
        assertThat(frameRobotInfo.frames[0], equalTo(FRAME_0))
        assertThat(frameRobotInfo.lastFrameTime, equalTo(1000L))
        assertThat(frameRobotInfo.lastFrame, equalTo(FRAME_0))
    }

    @Test
    fun testPlaceMultipleFrames() {
        frameRobotInfo.placeFrame(1000L, FRAME_0, isFinishFrame = false)
        frameRobotInfo.placeFrame(2000L, FRAME_1, isFinishFrame = false)
        frameRobotInfo.placeFrame(3000L, FRAME_2, isFinishFrame = false)

        assertThat(frameRobotInfo.frames, hasSize(3))
        assertThat(frameRobotInfo.frames[0], equalTo(FRAME_0))
        assertThat(frameRobotInfo.frames[1], equalTo(FRAME_1))
        assertThat(frameRobotInfo.frames[2], equalTo(FRAME_2))
        assertThat(frameRobotInfo.lastFrameTime, equalTo(3000L))
        assertThat(frameRobotInfo.lastFrame, equalTo(FRAME_2))
    }

    @Test
    fun testPlaceFrameNonFinishWithDuplicate() {
        frameRobotInfo.placeFrame(1000L, FRAME_0, isFinishFrame = false)
        frameRobotInfo.placeFrame(2000L, FRAME_1, isFinishFrame = false)
        frameRobotInfo.placeFrame(3000L, FRAME_2, isFinishFrame = false)
        
        // Place a duplicate frame that already exists - should remove extra frames
        frameRobotInfo.placeFrame(4000L, FRAME_1, isFinishFrame = false)

        assertThat(frameRobotInfo.frames, hasSize(2))
        assertThat(frameRobotInfo.frames[0], equalTo(FRAME_0))
        assertThat(frameRobotInfo.frames[1], equalTo(FRAME_1))
        assertThat(frameRobotInfo.lastFrameTime, equalTo(4000L))
        assertThat(frameRobotInfo.lastFrame, equalTo(FRAME_1))
    }

    @Test
    fun testPlaceFrameNonFinishWithDuplicateFirst() {
        frameRobotInfo.placeFrame(1000L, FRAME_0, isFinishFrame = false)
        frameRobotInfo.placeFrame(2000L, FRAME_1, isFinishFrame = false)
        frameRobotInfo.placeFrame(3000L, FRAME_2, isFinishFrame = false)
        
        // Place the first frame again - should remove all frames after it
        frameRobotInfo.placeFrame(4000L, FRAME_0, isFinishFrame = false)

        assertThat(frameRobotInfo.frames, hasSize(1))
        assertThat(frameRobotInfo.frames[0], equalTo(FRAME_0))
        assertThat(frameRobotInfo.lastFrameTime, equalTo(4000L))
        assertThat(frameRobotInfo.lastFrame, equalTo(FRAME_0))
    }

    @Test
    fun testPlaceFrameFinishWithDuplicate() {
        frameRobotInfo.placeFrame(1000L, FRAME_0, isFinishFrame = false)
        frameRobotInfo.placeFrame(2000L, FRAME_1, isFinishFrame = false)
        
        // Place a duplicate frame as finish frame - should just add it
        frameRobotInfo.placeFrame(3000L, FRAME_1, isFinishFrame = true)

        assertThat(frameRobotInfo.frames, hasSize(3))
        assertThat(frameRobotInfo.frames[0], equalTo(FRAME_0))
        assertThat(frameRobotInfo.frames[1], equalTo(FRAME_1))
        assertThat(frameRobotInfo.frames[2], equalTo(FRAME_1))
        assertThat(frameRobotInfo.lastFrameTime, equalTo(3000L))
        assertThat(frameRobotInfo.lastFrame, equalTo(FRAME_1))
    }

    @Test
    fun testReset() {
        frameRobotInfo.placeFrame(1000L, FRAME_0, isFinishFrame = false)
        frameRobotInfo.placeFrame(2000L, FRAME_1, isFinishFrame = false)
        
        frameRobotInfo.reset()

        assertThat(frameRobotInfo.frames, hasSize(0))
        assertThat(frameRobotInfo.lastFrameTime, equalTo(0L))
        assertThat(frameRobotInfo.lastFrame, nullValue())
    }

    @Test
    fun testRemoveExtraFramesComplex() {
        frameRobotInfo.placeFrame(1000L, FRAME_0, isFinishFrame = false)
        frameRobotInfo.placeFrame(2000L, FRAME_1, isFinishFrame = false)
        frameRobotInfo.placeFrame(3000L, FRAME_2, isFinishFrame = false)
        frameRobotInfo.placeFrame(4000L, FRAME_0, isFinishFrame = false)
        frameRobotInfo.placeFrame(5000L, FRAME_1, isFinishFrame = false)
        
        // At this point we should have: [FRAME_0, FRAME_1, FRAME_2, FRAME_0]
        // because when we placed FRAME_0 the second time, it removed FRAME_2 and FRAME_1 until it found FRAME_0
        // and when we placed FRAME_1, it removed FRAME_0 until it found FRAME_1
        assertThat(frameRobotInfo.frames, hasSize(2))
        assertThat(frameRobotInfo.frames[0], equalTo(FRAME_0))
        assertThat(frameRobotInfo.frames[1], equalTo(FRAME_1))
        
        // Place FRAME_0 again - should remove FRAME_1 until it finds FRAME_0
        frameRobotInfo.placeFrame(6000L, FRAME_0, isFinishFrame = false)

        // Expected result: [FRAME_0] (just removes FRAME_1, finds FRAME_0, doesn't add new one)
        assertThat(frameRobotInfo.frames, hasSize(1))
        assertThat(frameRobotInfo.frames[0], equalTo(FRAME_0))
        assertThat(frameRobotInfo.lastFrameTime, equalTo(6000L))
        assertThat(frameRobotInfo.lastFrame, equalTo(FRAME_0))
    }

    companion object {
        private const val FRAME_0 = 0xAA00
        private const val FRAME_1 = 0xAA01
        private const val FRAME_2 = 0xAA02
    }
}