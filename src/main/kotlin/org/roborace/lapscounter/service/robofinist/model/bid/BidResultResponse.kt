package org.roborace.lapscounter.service.robofinist.model.bid

import com.fasterxml.jackson.annotation.JsonProperty

data class BidResultResponse(
    @param:JsonProperty("data") val data: BidResultData,
) {
    data class BidResultData(
        @param:JsonProperty("programs") val programs: MutableList<BidProgram>,
    ) {
        var teamName: String = ""
    }

    data class BidProgram(
        @param:JsonProperty("id") val id: String,
        @param:JsonProperty("name") val name: String,
        @param:JsonProperty("program_stages") val programStages: List<BidProgramStages>,
    )

    data class BidProgramStages(
        @param:JsonProperty("id") val id: String,
        @param:JsonProperty("name") val name: String,
        @param:JsonProperty("results") val results: Map<String, BidStageResult>?,
        @param:JsonProperty("place") val place: Int,
    )

    data class BidStageResult(
        @param:JsonProperty("id") val id: String,
        @param:JsonProperty("arResults") val arResults: List<Number>,
    )
}