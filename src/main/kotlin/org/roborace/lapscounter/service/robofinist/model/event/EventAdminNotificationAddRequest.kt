package org.roborace.lapscounter.service.robofinist.model.event

import com.fasterxml.jackson.annotation.JsonProperty

data class EventAdminNotificationAddRequest(
    @get:JsonProperty("user_id") val userId: Int,
    @get:JsonProperty("program_id") val programId: Int,
    override val eventId: Int,
    override var token: String? = null,
    override val url: String = "event/admin/notification/add",
) : EventBaseRequest(eventId, token, url)
