package org.roborace.lapscounter.service.robofinist.model.partner

import com.fasterxml.jackson.annotation.JsonProperty

data class EventPartnerAddResponse(
    @param:JsonProperty("data") val data: PartnerData,
) {
    data class PartnerData(
        @param:JsonProperty("id") val id: Int,
        @param:JsonProperty("org_id") val orgId: String,
        val type: Int,
    )
}
