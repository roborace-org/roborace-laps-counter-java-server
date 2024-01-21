package org.roborace.lapscounter.service.robofinist.model

import com.fasterxml.jackson.annotation.JsonProperty

open class BaseRequest(
    @get:JsonProperty("token") open var token: String? = null,
    @get:JsonProperty("__url") open val url: String,
)
