package org.roborace.lapscounter.service.robofinist.model.partner

import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.service.robofinist.model.BaseRequest

data class EventPartnerAddRequest(
    @get:JsonProperty("event_id") val eventId: Int,
    @get:JsonProperty("org_id") val orgId: Int,
    val type: Int = 3,
    override var token: String? = null,
    override val url: String = "event/partners/add",
) : BaseRequest(token, url)
