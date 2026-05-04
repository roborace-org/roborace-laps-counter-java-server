package org.roborace.lapscounter.robofinist.model.bid

import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.robofinist.model.BaseRequest


data class BidSearchRequest(
    @get:JsonProperty("program_id") val programId: Long,
) : BaseRequest(url = "event/program/stage/bids")

data class BidSearchResponse(
    @param:JsonProperty("data") val data: List<Bid>,
)

data class BidChangeStatusRequest(
    @get:JsonProperty("bid_id") val bidId: Int,
    @get:JsonProperty("status_id") val statusId: Int,
) : BaseRequest(url = "event/admin/bid/ChangeStatus")

data class BidChangeStatusResponse(
    val code: Int?,
)
