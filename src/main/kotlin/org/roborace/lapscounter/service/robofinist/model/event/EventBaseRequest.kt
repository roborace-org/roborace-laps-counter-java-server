package org.roborace.lapscounter.service.robofinist.model.event

import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.service.robofinist.model.BaseRequest

open class EventBaseRequest(
    @get:JsonProperty("event_id") open val eventId: Int? = null,
    override var token: String?,
    override val url: String = "event/admin/add",
) : BaseRequest(token = token, url = url)
