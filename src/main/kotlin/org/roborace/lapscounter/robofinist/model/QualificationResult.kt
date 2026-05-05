package org.roborace.lapscounter.robofinist.model

data class QualificationResult(
    val bidId: Int,
    val name: String,
    val best: AttemptResult?,
    val attempts: List<AttemptResult>,
    val place: Int?,
)

data class AttemptResult(
    val laps: Int?,
    val time: Double?,
    val disqualified: Boolean,
)
