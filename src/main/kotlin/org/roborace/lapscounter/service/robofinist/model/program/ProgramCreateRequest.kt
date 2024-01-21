package org.roborace.lapscounter.service.robofinist.model.program

import com.fasterxml.jackson.annotation.JsonProperty
import org.roborace.lapscounter.service.robofinist.model.event.EventBaseRequest

data class ProgramCreateRequest(
    @get:JsonProperty("is_tournament") val isTournament: Int = 1,
    @get:JsonProperty("regulations_id") val regulationId: Int,
    @get:JsonProperty("name") val name: String,
    @get:JsonProperty("description") val description: String,
    @get:JsonProperty("max_robot") val maxRobot: Int = 1,
    @get:JsonProperty("min_participant") val minParticipant: Int = 1,
    @get:JsonProperty("max_participant") val maxParticipant: Int = 2,
    @get:JsonProperty("check_unique_participant") val checkUniqueParticipant: Int = 1,
    @get:JsonProperty("sort") val sort: Int = 0,
    @get:JsonProperty("arRequiredRobotsFields")
    val arRequiredRobotsFields: List<String> = listOf(),

    override val eventId: Int,
    override var token: String? = null,
    override val url: String = "event/program/create",
) : EventBaseRequest(eventId, token, url) {
    @get:JsonProperty("name_en")
    val nameEn: String = ""

    @get:JsonProperty("description_en")
    val descriptionEn: String = ""

}
