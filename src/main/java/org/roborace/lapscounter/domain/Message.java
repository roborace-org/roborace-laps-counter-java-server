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
public class Message {
    private Type type;

    private State state;

    private Integer serial;
    private Integer frame;

    private String name;

    private Integer num;
    private Integer laps;
    private Long time;
    private Long raceTimeLimit;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private Long lastLapTime;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private Long bestLapTime;
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private Long pitStopFinishTime;
    private Integer place;

    private String message;

    public Message(Type type) {
        this.type = type;
    }
}
