package org.roborace.lapscounter.service.robofinist.model.result

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.service.robofinist.model.BaseRequest

data class ResultCreateRequest(
//    @get:JsonProperty("stage_id")
    @JsonIgnore
    val stageId: Int,
//    @get:JsonProperty("bid_id")
    @JsonIgnore
    val bidId: Int,

    @get:JsonProperty("number") val number: Int,
    @get:JsonProperty("params") val params: ResultParams,

    override var token: String? = null,
    @JsonIgnore
    override val url: String = "data/referee/events/programs/stages/${stageId}/bids/${bidId}/results",
) : BaseRequest(token, url) {
    data class ResultParams(
        @get:JsonProperty("0") val laps: String,
        @get:JsonProperty("1") val time: Double,
    )
}
