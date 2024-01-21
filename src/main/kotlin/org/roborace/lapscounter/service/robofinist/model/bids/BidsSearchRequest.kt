package org.roborace.lapscounter.service.robofinist.model.bids

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.service.robofinist.model.BaseRequest

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BidsSearchRequest(
    @get:JsonProperty("program_id") val programId: Int,
    @get:JsonProperty("status") val status: String? = null,
    override var token: String? = null,
    override val url: String = "event/program/stage/bids",
) : BaseRequest(token, url)
