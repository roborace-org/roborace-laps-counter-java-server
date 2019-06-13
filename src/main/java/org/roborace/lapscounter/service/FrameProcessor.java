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
        Type frameResult = getFrameResult(robot, frame, raceTime);
        LOG.info("Frame result: {}, {}, robot: {}", frameResult, frame, robot.getSerial());
        return frameResult;
    }

    private Type getFrameResult(Robot robot, Integer frame, long raceTime) {
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

        if (allFrames(frame, robotFrames)) {
            robotFrames.clear();
            frameRobotInfo.placeFrame(raceTime, frame);
            return Type.LAP;
        }

        if (isNextRobotFrame(frame, frameRobotInfo.getLastFrame())) {
            frameRobotInfo.placeFrame(raceTime, frame);
            return Type.FRAME;
        }

        if (isLastRobotFrame(frame, frameRobotInfo.getLastFrame())) {
            return Type.DUPLICATE_FRAME;
        }

        if (isPreviousRobotFrame(frame, frameRobotInfo.getLastFrame())) {
            frameRobotInfo.removeLastFrame(frame);
            return Type.WRONG_ROTATION;
        }

        return Type.WRONG_FRAME;

    }

    private boolean isNextRobotFrame(Integer frame, Integer lastFrame) {
        return frame.equals(getExpectedNextFrame(lastFrame));
    }

    private boolean isLastRobotFrame(Integer frame, Integer lastFrame) {
        return frame.equals(lastFrame);
    }

    private boolean isPreviousRobotFrame(Integer frame, Integer lastFrame) {
        return frame.equals(getExpectedPrevFrame(lastFrame));
    }

    private Integer getExpectedNextFrame(Integer lastFrame) {
        int lastFrameIndex = frames.indexOf(lastFrame);
        return frames.get((lastFrameIndex + 1) % frames.size());
    }

    private Integer getExpectedPrevFrame(Integer lastFrame) {
        int lastFrameIndex = frames.indexOf(lastFrame);
        return frames.get((lastFrameIndex - 1 + frames.size()) % frames.size());
    }

    private boolean allFrames(Integer frame, List<Integer> robotFrames) {
        return robotFrames.size() == frames.size() && frame.equals(frames.get(0));
    }

    private boolean isTooQuick(long raceTime, long lastFrameTime) {
        return raceTime < lastFrameTime + safeInterval;
    }
}
