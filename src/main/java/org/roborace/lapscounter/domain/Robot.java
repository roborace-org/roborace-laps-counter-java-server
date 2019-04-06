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

    private final List<Long> lapTimes = new ArrayList<>();

    public void incLaps(long raceTime) {
        laps++;
        lapTimes.add(raceTime);
    }

    public void decLaps() {
        laps--;
    }

    public void reset() {
        laps = 0;
        time = 0;
    }

    public long extractLastLapTime() {
        if (lapTimes.isEmpty()) return 0L;
        lapTimes.remove(lapTimes.size() - 1);
        if (lapTimes.isEmpty()) return 0L;
        return lapTimes.get(lapTimes.size() - 1);
    }
}
