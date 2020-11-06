package org.roborace.lapscounter.domain;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode(exclude = "lapTimes")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Robot {

    private int serial;

    private String name;

    private int num;
    private int place;
    private int laps;
    private long time;

    private long currentLapStartTime;
    private long lastLapTime;
    private long bestLapTime;

    private final List<Long> lapTimes = new ArrayList<>();

    public void incLaps(long raceTime) {
        laps++;
        if (laps > 0) {
            lapTimes.add(raceTime);
            time = raceTime;
        }
        lastLapTime = raceTime - currentLapStartTime;
        currentLapStartTime = raceTime;
        if (lastLapTime < bestLapTime || bestLapTime == 0) {
            bestLapTime = lastLapTime;
        }
    }

    public void decLaps() {
        laps--;
    }

    public void reset() {
        laps = 0;
        time = currentLapStartTime = lastLapTime = bestLapTime = 0;
        lapTimes.clear();
    }

    public long extractLastLapTime() {
        if (lapTimes.isEmpty()) return 0L;
        lapTimes.remove(lapTimes.size() - 1);
        if (lapTimes.isEmpty()) return 0L;
        return lapTimes.get(lapTimes.size() - 1);
    }
}
