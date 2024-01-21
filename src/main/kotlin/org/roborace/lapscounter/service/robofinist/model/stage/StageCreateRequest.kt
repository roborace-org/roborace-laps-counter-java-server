package org.roborace.lapscounter.service.robofinist.model.stage

import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.service.robofinist.model.BaseRequest

data class StageCreateRequest(
    @get:JsonProperty("program_id") val programId: Int,
    @get:JsonProperty("name") val name: String,
    @get:JsonProperty("type_start") val typeStart: Int,
    @get:JsonProperty("type_final") val typeFinal: Int,
    @get:JsonProperty("quota") val quota: Int = 0,
    @get:JsonProperty("count_single") val countSingle: Int = 0,
    @get:JsonProperty("hidden_results") val hiddenResults: Boolean = false,
    @get:JsonProperty("formula") val formula: String? = null,
    @get:JsonProperty("challonge_type") val challongeType: String? = null,
    @get:JsonProperty("judging_type") val judgingType: String? = null,
    override var token: String? = null,
    override val url: String = "event/program/stage/add",
) : BaseRequest(token, url)
