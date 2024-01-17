package org.roborace.lapscounter.service;

import org.roborace.lapscounter.domain.FrameRobotInfo;
import org.roborace.lapscounter.domain.Robot;
import org.roborace.lapscounter.domain.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FrameProcessor {

    public static final Logger log = LoggerFactory.getLogger(FrameProcessor.class);

    private final Map<Integer, FrameRobotInfo> frameInfoBySerialMap = new HashMap<>();

    private final long safeInterval;
    private final List<Integer> frames;
    private final List<Integer> reversedFrames;

    public FrameProcessor(@Value("${laps.safe-interval}") long safeInterval,
                          @Value("${laps.frames}") List<Integer> frames) {
        this.safeInterval = safeInterval;
        this.frames = frames;
        if (frames.isEmpty()) {
            throw new RuntimeException("Frames are not detected");
        }
        reversedFrames = frames.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
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
        log.info("Frame result: {}, {}, robot: {}", frameResult, frame, robot.getSerial());
        return frameResult;
    }

    public boolean isStartFrame(Integer frame) {
        return frames.get(0).equals(frame);
    }

    private Type getFrameResult(Robot robot, Integer frame, long raceTime) {
        if (!frames.contains(frame)) {
            log.warn("Frame not found: {}, robot: {}", frame, robot.getSerial());
            return Type.ERROR;
        }

        FrameRobotInfo frameRobotInfo = frameInfoBySerialMap.get(robot.getSerial());

        if (frameRobotInfo == null) {
            log.warn("Robot [{}] is not init", robot.getSerial());
            return Type.ERROR;
        }

        List<Integer> robotFrames = frameRobotInfo.getFrames();
        Integer lastFrame = frameRobotInfo.getLastFrame();

        if (isTooQuick(raceTime, frameRobotInfo.getLastFrameTime()) && !robotFrames.isEmpty()) {
            log.warn("Frame is not counted (too quick): {}, robot: {}", frame, robot.getSerial());
            return Type.ERROR;
        }

        boolean isFinishFrame = frame.equals(frames.get(0));

        frameRobotInfo.placeFrame(raceTime, frame, isFinishFrame);

        if (isFinishFrame) {

            if (allFrames(robotFrames)) {
                robotFrames.clear();
                frameRobotInfo.placeFrame(raceTime, frame, true);
                return Type.LAP;
            }

            if (allFramesWrongDirection(robotFrames)) {
                robotFrames.clear();
                frameRobotInfo.placeFrame(raceTime, frame, true);
                return Type.LAP_MINUS;
            }

            robotFrames.clear();
            frameRobotInfo.placeFrame(raceTime, frame, true);
        }

        if (isNextRobotFrame(frame, lastFrame)) {
            return Type.FRAME;
        }

        if (isLastRobotFrame(frame, lastFrame)) {
            return Type.DUPLICATE_FRAME;
        }

        if (isPreviousRobotFrame(frame, lastFrame)) {
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
        int nextIndex = (lastFrameIndex + 1) % frames.size();
        return frames.get(nextIndex);
    }

    private Integer getExpectedPrevFrame(Integer lastFrame) {
        int lastFrameIndex = frames.indexOf(lastFrame);
        if (lastFrameIndex == -1) lastFrameIndex = 0;
        int prevIndex = (lastFrameIndex - 1 + frames.size()) % frames.size();
        return frames.get(prevIndex);
    }

    private boolean allFrames(List<Integer> robotFrames) {
        return hasSubsequence(robotFrames, frames);
    }

    private boolean allFramesWrongDirection(List<Integer> robotFrames) {
        return hasSubsequence(robotFrames, reversedFrames);
    }

    private boolean hasSubsequence(List<Integer> robotFrames, List<Integer> search) {
        int i = 0;
        for (Integer frame : search) {
            boolean found = false;
            for (int j = i; j < robotFrames.size(); j++) {
                if (frame.equals(robotFrames.get(j))) {
                    i++;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return robotFrames.size() >= 2;
    }

    private boolean isTooQuick(long raceTime, long lastFrameTime) {
        return raceTime < lastFrameTime + safeInterval;
    }
}
