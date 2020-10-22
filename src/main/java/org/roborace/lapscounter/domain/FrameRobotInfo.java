package org.roborace.lapscounter.domain;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrameRobotInfo {
    private final List<Integer> frames = new ArrayList<>();
    private long lastFrameTime;
    private Integer lastFrame;

    public void placeFrame(long raceTime, Integer frame, boolean isFinishFrame) {
        setLastFrameTime(raceTime);
        if (!isFinishFrame && frames.contains(frame)) {
            removeExtraFrames(frame);
        } else {
            frames.add(frame);
        }
        lastFrame = frame;
    }

    public void removeLastFrame(Integer frame) {
        if (!frames.isEmpty()) {
            frames.remove(frames.size() - 1);
        }
        lastFrame = frame;
    }

    public void reset() {
        setLastFrameTime(0);
        frames.clear();
        lastFrame = null;
    }

    private void removeExtraFrames(Integer frame) {
        int i = frames.indexOf(frame);
        int sizeToLeave = i + 1;
        while (frames.size() > sizeToLeave) {
            frames.remove(sizeToLeave);
        }
    }
}
