package org.roborace.lapscounter.domain

data class Robot(
    val serial: Int,
    var name: String? = null,
    val num: Int = 0,
    var place: Int = 0,
    var laps: Int = 0,
    var time: Long = 0,
) {
    private val lapTimes: MutableList<Long> = mutableListOf()

    var currentLapStartTime: Long = 0
    var lastLapTime: Long? = null

    var bestLapTime: Long? = null

    var pitStopFinishTime: Long? = null

    fun incLaps(raceTime: Long) {
        laps++
        if (laps > 0) {
            lapTimes.add(raceTime)
            time = raceTime
        }
        lastLapTime = raceTime - currentLapStartTime
        currentLapStartTime = raceTime
        if (bestLapTime == null || lastLapTime!! < bestLapTime!!) {
            bestLapTime = lastLapTime
        }
    }

    fun decLap() {
        laps--
        lapTimes.removeLastOrNull()
        time = lapTimes.lastOrNull() ?: 0L
    }

    fun reset() {
        laps = 0
        time = 0
        lapTimes.clear()
        bestLapTime = null
        lastLapTime = null
        currentLapStartTime = 0
        pitStopFinishTime = null
    }
}
