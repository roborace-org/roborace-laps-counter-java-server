package org.roborace.lapscounter.service.robofinist.model.bid

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.service.robofinist.model.BaseRequest

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BidParticipantsRequest(
    @get:JsonProperty("bid_id") val bidId: Int,
    override var token: String? = null,
    override val url: String = "event/admin/bid/participant/list",
) : BaseRequest(token, url)
