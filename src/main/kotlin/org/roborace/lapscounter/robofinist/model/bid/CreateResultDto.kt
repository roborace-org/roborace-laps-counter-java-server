package org.roborace.lapscounter.robofinist.model.bid

data class CreateResultDto(
    val stageId: Int,
    val bidId: Int,
    val number: Int,
    val laps: Int,
    val time: Double,
    val disqualified: Boolean = false,
)