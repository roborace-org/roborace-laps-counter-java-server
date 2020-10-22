package org.roborace.lapscounter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roborace.lapscounter.domain.Robot;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.roborace.lapscounter.domain.Type.*;

class FrameProcessorTest {

    private static final int FRAME_0 = 0xAA00;
    private static final int FRAME_1 = 0xAA01;
    private static final int FRAME_2 = 0xAA02;
    private static final List<Integer> FRAMES = asList(FRAME_0, FRAME_1, FRAME_2);
    private FrameProcessor frameProcessor = new FrameProcessor(1000, FRAMES);

    private Robot robot = Robot.builder().serial(50).build();

    @BeforeEach
    void setUp() {
        frameProcessor.robotInit(robot);
    }

    @Test
    void testErrorsByTime() {
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 1), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 0), equalTo(ERROR));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 100), equalTo(ERROR));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 50), equalTo(ERROR));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 999), equalTo(ERROR));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 1001), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 1100), equalTo(ERROR));
    }


    @Test
    void testResetTime() {
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 1000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 2000), equalTo(FRAME));
        frameProcessor.reset();
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 1000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 2000), equalTo(FRAME));
    }

    @Test
    void testResetLaps() {
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 1000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 2000), equalTo(FRAME));
        frameProcessor.reset();
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 3000), equalTo(WRONG_FRAME));
    }


    @Test
    void testWrongFrame() {
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 1000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, 0xBB00, 3000), equalTo(ERROR));
    }

    @Test
    void testDuplicateFrame() {
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 1000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 2000), equalTo(DUPLICATE_FRAME));
    }

    @Test
    void testFrameWrongRotate() {
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 1000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 2000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 3000), equalTo(LAP_MINUS));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 4000), equalTo(WRONG_ROTATION));
    }

    @Test
    void testFrameWrongRotateAfterThreeFrames() {
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 1000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 2000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 3000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 4000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 5000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 6000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 7000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 8000), equalTo(LAP_MINUS));
    }

    @Test
    void testFrameWrongRotateThreeLaps() {
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 1000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 2000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 3000), equalTo(LAP_MINUS));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 4000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 5000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 6000), equalTo(LAP_MINUS));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 7000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 8000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 9000), equalTo(LAP_MINUS));
    }

    @Test
    void testThreeFrames() {
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 0), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 1000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 2000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 3000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 4000), equalTo(LAP));
    }

    @Test
    void testTwoLaps() {
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 1000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 2000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 3000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 4000), equalTo(LAP));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 5000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 6000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 7000), equalTo(LAP));
    }

    @Test
    void testSleepAfterFirstLap() {
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 0), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 1000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 2000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 3000), equalTo(LAP));

        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 4000), equalTo(WRONG_ROTATION));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 5000), equalTo(DUPLICATE_FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 6000), equalTo(DUPLICATE_FRAME));

        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 7000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 8000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 9000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 10000), equalTo(LAP));
    }

    @Test
    void testSkipFrame() {
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 1000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 3000), equalTo(WRONG_ROTATION));
    }

    @Test
    void testLapsTwoRobots() {
        Robot robot2 = Robot.builder().serial(70).build();
        frameProcessor.robotInit(robot2);

        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 1000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 2000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot2, FRAME_0, 2100), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot2, FRAME_1, 3100), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot2, FRAME_2, 4100), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_2, 5100), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot2, FRAME_0, 6200), equalTo(LAP));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 7200), equalTo(LAP));
    }

    @Test
    void testLapsOneFrame() {
        frameProcessor = new FrameProcessor(1000, singletonList(FRAME_0));
        frameProcessor.robotInit(robot);

        assertThat(frameProcessor.checkFrame(robot, FRAME_1, 1000), equalTo(ERROR));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 1000), equalTo(FRAME));
        assertThat(frameProcessor.checkFrame(robot, FRAME_0, 2000), equalTo(LAP));
    }
}