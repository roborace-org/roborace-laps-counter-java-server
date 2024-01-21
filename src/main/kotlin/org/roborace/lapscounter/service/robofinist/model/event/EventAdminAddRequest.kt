package org.roborace.lapscounter.service.robofinist.model.event

import com.fasterxml.jackson.annotation.JsonProperty

data class EventAdminAddRequest(
    @get:JsonProperty("user_id") val userId: Int,
    override val eventId: Int,
    override var token: String? = null,
    override val url: String = "event/admin/add",
) : EventBaseRequest(eventId, token, url)
