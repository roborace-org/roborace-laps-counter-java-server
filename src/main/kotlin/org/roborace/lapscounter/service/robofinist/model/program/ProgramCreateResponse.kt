package org.roborace.lapscounter.service.robofinist.model.program

import com.fasterxml.jackson.annotation.JsonProperty

data class ProgramCreateResponse(
    @param:JsonProperty("data") val data: ProgramCreateData,
) {
    data class ProgramCreateData(
        @param:JsonProperty("id") val id: String,
    )

}
