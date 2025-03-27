package org.roborace.lapscounter.service.robofinist.model.winners

import com.fasterxml.jackson.annotation.JsonProperty

data class WinnersSearchResponse(
    @param:JsonProperty("data") val data: List<Winner>,
) {
    data class Winner(
        @param:JsonProperty("bid_id")
        val bidId: Int,
        val place: Int,
        val bid: Bid,
    )

    data class Bid(
        val id: Int,
        val name: String,
        val organizations: List<Organization>,
    )

    data class Organization(
        val name: String,
    )

}
