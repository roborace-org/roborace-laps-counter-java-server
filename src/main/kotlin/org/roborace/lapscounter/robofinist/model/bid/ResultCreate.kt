package org.roborace.lapscounter.robofinist.model.bid

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.robofinist.model.BaseRequest

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ResultCreateRequest(
    @get:JsonProperty("stage_id") val stageId: Int,
    @get:JsonProperty("bid_id") val bidId: Int,
    @get:JsonProperty("number") val number: Int,
    @get:JsonProperty("params") val params: Any,
    @get:JsonProperty("disq_action") val disqAction: String? = null,
    @get:JsonProperty("disq_reason") val disqReason: Boolean? = null,
) : BaseRequest(url = "event/referee/programs/stages/bids/results/create") {

    data class ResultParams(
        @get:JsonProperty("0") val laps: String,
        @get:JsonProperty("1") val time: Double,
    )

    companion object {
        fun create(stageId: Int, bidId: Int, number: Int, laps: Int, time: Double) = ResultCreateRequest(
            stageId = stageId,
            bidId = bidId,
            number = number,
            params = ResultParams(laps = laps.toString(), time = time)
        )

        fun disqualify(stageId: Int, bidId: Int, number: Int) = ResultCreateRequest(
            stageId = stageId,
            bidId = bidId,
            number = number,
            params = listOf(0),
            disqAction = "1",
            disqReason = true
        )
    }
}

data class ResultCreateResponse(
    val code: Int?,
)
