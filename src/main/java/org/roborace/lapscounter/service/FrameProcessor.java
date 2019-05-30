package org.roborace.lapscounter.service;

import org.roborace.lapscounter.domain.FrameRobotInfo;
import org.roborace.lapscounter.domain.Robot;
import org.roborace.lapscounter.domain.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FrameProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FrameProcessor.class);

    private final Map<Integer, FrameRobotInfo> frameInfoBySerialMap = new HashMap<>();

    private final long safeInterval;
    private final List<Integer> frames;

    public FrameProcessor(@Value("${laps.safe-interval}") long safeInterval,
                          @Value("${laps.frames}") List<Integer> frames) {
        this.safeInterval = safeInterval;
        this.frames = frames;
        if (frames.isEmpty()) {
            throw new RuntimeException("Frames are not detected");
        }
    }

    public void robotInit(Robot robot) {
        frameInfoBySerialMap.put(robot.getSerial(), new FrameRobotInfo());
    }

    public void robotRemove(Robot robot) {
        frameInfoBySerialMap.remove(robot.getSerial());
    }

    public void reset() {
        frameInfoBySerialMap.values().forEach(FrameRobotInfo::reset);
    }

    public Type checkFrame(Robot robot, Integer frame, long raceTime) {

        if (!frames.contains(frame)) {
            LOG.warn("Frame not found: {}, robot: {}", frame, robot.getSerial());
            return Type.ERROR;
        }

        FrameRobotInfo frameRobotInfo = frameInfoBySerialMap.get(robot.getSerial());

        if (frameRobotInfo == null) {
            LOG.warn("Robot [{}] is not init", robot.getSerial());
            return Type.ERROR;
        }

        List<Integer> robotFrames = frameRobotInfo.getFrames();

        if (isTooQuick(raceTime, frameRobotInfo.getLastFrameTime()) && !robotFrames.isEmpty()) {
            LOG.warn("Frame is not counted (too quick): {}, robot: {}", frame, robot.getSerial());
            return Type.ERROR;
        }

        if (isNextRobotFrame(frame, robotFrames)) {
            LOG.info("Frame is counted: {}, robot: {}", frame, robot.getSerial());
            frameRobotInfo.updateInfo(raceTime, frame);
        } else {
            LOG.info("Frame is wrong: {}, robot: {}", frame, robot.getSerial());
            if (frame.equals(frames.get(0))) {
                robotFrames.clear();
                robotFrames.add(frames.get(0));
            }
            return Type.WRONG_FRAME;
        }

        if (allFrames(robotFrames)) {
            LOG.info("Lap is counted: {}", robot);
            robotFrames.clear();
            robotFrames.add(frames.get(0));
            return Type.LAP;
        }

        return Type.FRAME;
    }

    private boolean isNextRobotFrame(Integer frame, List<Integer> robotFrames) {
        return frame.equals(expectedNextFrame(getLastRobotFrame(robotFrames)));
    }

    private Integer getLastRobotFrame(List<Integer> robotFrames) {
        if (robotFrames.isEmpty()) {
            return null;
        }
        return robotFrames.get(robotFrames.size() - 1);
    }

    private Integer expectedNextFrame(Integer lastFrame) {
        if (lastFrame == null) {
            return frames.get(0);
        }

        boolean find = false;
        for (Integer frame : frames) {
            if (find) {
                return frame;
            }
            if (frame.equals(lastFrame)) {
                find = true;
            }
        }
        return frames.get(0);
    }

    private boolean allFrames(List<Integer> robotFrames) {
        return robotFrames.size() == frames.size() + 1;
    }

    private boolean isTooQuick(long raceTime, long lastFrameTime) {
        return raceTime < lastFrameTime + safeInterval;
    }
}
