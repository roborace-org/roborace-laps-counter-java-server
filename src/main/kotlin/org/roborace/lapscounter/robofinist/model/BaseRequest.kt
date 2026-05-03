package org.roborace.lapscounter.robofinist.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
open class BaseRequest(
    @get:JsonProperty("token") open var token: String? = null,
    @get:JsonProperty("__url") open val url: String,
)
