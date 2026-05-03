package org.roborace.lapscounter.robofinist.model.event

import java.time.LocalDate

data class EventDto(
    val id: Int,
    val name: String,
    val date: LocalDate?,
)
