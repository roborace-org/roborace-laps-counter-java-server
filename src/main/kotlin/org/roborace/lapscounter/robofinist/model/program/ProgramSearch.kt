package org.roborace.lapscounter.robofinist.model.program

import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.robofinist.model.BaseRequest

data class ProgramSearchRequest(
    @get:JsonProperty("event_id") val eventId: Long? = null,
) : BaseRequest(url = "event/program/search")

data class ProgramSearchResponse(
    @param:JsonProperty("data") val data: List<Program>,
)
