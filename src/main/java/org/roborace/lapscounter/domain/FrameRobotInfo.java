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

    public void updateInfo(long raceTime, Integer frame) {
        setLastFrameTime(raceTime);
        frames.add(frame);
    }

    public void reset() {
        setLastFrameTime(0);
        frames.clear();
    }
}
