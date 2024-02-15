package org.roborace.lapscounter.domain.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.domain.State
import org.roborace.lapscounter.domain.Type

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Message(
    @JsonProperty("type") val type: Type,
    @JsonProperty("state") val state: State? = null,
    @JsonProperty("serial") val serial: Int? = null,
    @JsonProperty("frame") val frame: Int? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("num") val num: Int? = null,
    @JsonProperty("laps") val laps: Int? = null,
    @JsonProperty("time") val time: Long? = null,
    @JsonProperty("raceTimeLimit") val raceTimeLimit: Long? = null,
    @JsonProperty("lastLapTime") var lastLapTime: Long = 0,
    @JsonProperty("bestLapTime") var bestLapTime: Long = 0,
    @JsonProperty("pitStopFinishTime") var pitStopFinishTime: Long? = null,
    @JsonProperty("place") var place: Int? = null,
    @JsonProperty("message") var message: String? = null,
)
