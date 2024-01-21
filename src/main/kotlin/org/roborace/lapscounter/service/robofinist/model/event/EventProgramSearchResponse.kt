package org.roborace.lapscounter.service.robofinist.model.event

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventProgramSearchResponse(
    @param:JsonProperty("data") val data: List<EventProgramData>,
) {
    data class EventProgramData(
        @param:JsonProperty("id") val id: Int,
        @param:JsonProperty("name") val name: String,
    )
}
