package org.roborace.lapscounter.robofinist.model.stage

import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.robofinist.model.BaseRequest

data class StageSearchRequest(
    @get:JsonProperty("program_id") val programId: Long,
) : BaseRequest(url = "event/program/stage/listByProgram")

data class StageSearchResponse(
    val data: List<Stage>,
)

data class StageEditRequest(
    @get:JsonProperty("stage_id") val stageId: Long,
    @get:JsonProperty("status") val status: Int,
) : BaseRequest(url = "event/program/stage/SetStatus")

data class StageEditResponse(
    val code: Int?,
)
