package org.roborace.lapscounter.service.robofinist.model.winners

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.service.robofinist.model.BaseRequest

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WinnersSearchRequest(
    @get:JsonProperty("program_id") val programId: Int,
    override var token: String? = null,
    override val url: String = "event/admin/program/winners/list",
) : BaseRequest(token, url)
