package org.roborace.lapscounter.service;

import org.junit.jupiter.api.Test;
import org.roborace.lapscounter.domain.Robot;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameProcessorTest {

    private static final int FRAME_0 = 0xAA00;
    private static final List<Integer> FRAMES = Collections.singletonList(0xAA00);
    private FrameProcessor frameProcessor = new FrameProcessor(1000, FRAMES);

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


    @Test
    void testWrongFrame() {

        Robot robot = Robot.builder().serial(50).build();
        assertTrue(frameProcessor.checkFrame(robot, FRAME_0, 1000));

        assertFalse(frameProcessor.checkFrame(robot, 0xBB00, 3000));

    }
}