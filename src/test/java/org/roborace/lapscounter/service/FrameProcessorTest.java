package org.roborace.lapscounter.service;

import org.junit.jupiter.api.Test;
import org.roborace.lapscounter.domain.Robot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameProcessorTest {

    public static final int FRAME_0 = 0xAA00;
    private FrameProcessor frameProcessor = new FrameProcessor(1000);

    @Test
    void checkFrame() {
        Robot robot = Robot.builder().serial(50).build();

        assertFalse(frameProcessor.checkFrame(robot, FRAME_0, 0));
        assertFalse(frameProcessor.checkFrame(robot, FRAME_0, 100));
        assertFalse(frameProcessor.checkFrame(robot, FRAME_0, 50));
        assertFalse(frameProcessor.checkFrame(robot, FRAME_0, 999));
        assertTrue(frameProcessor.checkFrame(robot, FRAME_0, 1001));
        assertFalse(frameProcessor.checkFrame(robot, FRAME_0, 1100));
    }


    @Test
    void reset() {

        Robot robot = Robot.builder().serial(50).build();
        assertTrue(frameProcessor.checkFrame(robot, FRAME_0, 1000));

        frameProcessor.reset();

        assertTrue(frameProcessor.checkFrame(robot, FRAME_0, 1000));

    }
}