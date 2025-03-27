package org.roborace.lapscounter.service.robofinist.model.bid

import com.fasterxml.jackson.annotation.JsonProperty

data class BidParticipantsResponse(
    @param:JsonProperty("data") val data: List<Participant>,
) {
    data class Participant(
        val id: Int,
        val email: String? = "",
        @param:JsonProperty("first_name") val firstName: String? = "",
        @param:JsonProperty("last_name") val lastName: String? = "",
        val mentor: Int = 0,
        var organization: Organization?,
    )

    data class Organization(
        val id: Int,
        val name: String,
    )
}
