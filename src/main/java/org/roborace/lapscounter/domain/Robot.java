package org.roborace.lapscounter.domain;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
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

    public void incLaps() {
        laps++;
    }

    public void decLaps() {
        laps--;
    }

    public void reset() {
        laps = 0;
        time = 0;
    }
}
