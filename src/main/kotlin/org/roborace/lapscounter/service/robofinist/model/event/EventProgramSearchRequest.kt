package org.roborace.lapscounter.service.robofinist.model.event

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventProgramSearchRequest(
    @get:JsonProperty("program_id") val programId: Int? = null,
    @get:JsonProperty("showStages") val showStages: Boolean? = null,
    override val eventId: Int? = null,
    override var token: String?,
    override val url: String = "event/program/search",
) : EventBaseRequest(eventId, token, url)
