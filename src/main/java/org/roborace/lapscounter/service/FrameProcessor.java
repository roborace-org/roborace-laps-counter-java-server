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
    }

    public void robotInit(Robot robot) {
        frameInfoBySerialMap.put(robot.getSerial(), new FrameRobotInfo());
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

        if (isTooQuick(raceTime, frameRobotInfo.getLastFrameTime())) {
            LOG.warn("Frame is not counted (too quick): {}, robot: {}", frame, robot.getSerial());
            return Type.ERROR;
        }

        LOG.info("Frame is counted: {}, robot: {}", frame, robot.getSerial());
        frameRobotInfo.updateInfo(raceTime, frame);

        if (allFrames(frameRobotInfo.getFrames())) {
            robot.incLaps();
            robot.setTime(raceTime);
            LOG.info("Lap is counted: {}", robot);
            return Type.LAP;
        }

        return Type.FRAME;
    }

    private boolean allFrames(List<Integer> robotFrames) {

        int countFrames = 0;
        for (Integer robotFrame : robotFrames) {
            if (robotFrame.equals(frames.get(countFrames))) {
                countFrames++;
                if (frames.size() == countFrames) {
                    robotFrames.clear();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTooQuick(long raceTime, long lastFrameTime) {
        return raceTime < lastFrameTime + safeInterval;
    }

    public void reset() {
        frameInfoBySerialMap.values().forEach(FrameRobotInfo::reset);
    }
}
