package org.roborace.lapscounter.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Robot {

    private int serial;

    private String name;

    private int place;
    private int laps;
    private int time;

    private long lastLapMillis;

    public void incLaps() {
        laps++;
    }
}
