package org.roborace.lapscounter.service.robofinist.model.bids

import com.fasterxml.jackson.annotation.JsonProperty

data class BidsSearchResponse(
    @param:JsonProperty("data") val data: List<Bid>,
) {
    data class Bid(
        val id: Int,
        val name: String,
        val status: Int,
        val organizations: List<Organization>,
    )

    data class Organization(
        val id: Int,
        val name: String,
    )
}
