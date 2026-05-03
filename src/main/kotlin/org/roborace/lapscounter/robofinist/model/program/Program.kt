package org.roborace.lapscounter.robofinist.model.program

import com.fasterxml.jackson.annotation.JsonProperty

data class Program(
    @param:JsonProperty("id") val id: Long,
    @param:JsonProperty("name") val name: String,
)
