package org.roborace.lapscounter.service.robofinist.stat

import org.roborace.lapscounter.service.robofinist.Program
import org.roborace.lapscounter.service.robofinist.Robofinist
import org.roborace.lapscounter.service.robofinist.model.bid.BidResultResponse


@Suppress("MagicNumber")
fun main27() {

    val robofinist = Robofinist()

//    val events = mapOf(1246 to 14, 1255 to 14, 1283 to 16, 1328 to 14, 1433 to 14) // molr
    val events = mapOf(1210 to 14, 1270 to 14, 1303 to 14, 1349 to 14, 1408 to 16) // kor

    val mapResult = getMapForEvents(robofinist, events, Program.ROBORACE_OK)

    println("mapResult = ${mapResult}")

//    formatResultMedals(mapResult, events)

}

private fun getMapForEvents(robofinist: Robofinist, eventIds: Map<Int, Int>, program: Program) =
    eventIds.mapValues { (eventId, trackLength) -> getEventData(robofinist, eventId, trackLength, program) }
//        .groupBy { e -> e.teamName }
//        .map { (teamName, list) ->
//            WinnerRoboraceCup(teamName, list.sumOf { it.points }, list)
//        }.sortedByDescending { it.allPoints }


private fun getEventData(robofinist: Robofinist, eventId: Int, trackLength: Int, program: Program) =
    robofinist.eventProgramsSearch(eventId)
        .filter { it.key == program.text }
        .values
        .flatMap { programId ->

            val bids = robofinist.getBids(programId)!!.data
                .filter { it.status == 6 } // participated
                .mapNotNull { bid ->
                    robofinist.getBidsResults(bid.id)
                        ?.apply {
                            programs.removeIf { pr ->
                                pr.name != program.text
                            }
                            teamName = bid.name
                        }
                }
            val results = calcKvalPoints(bids, eventId, trackLength)
            calcOtborPoints(results, bids)
            calcFinalPoints(results, bids)

            results
        }

private fun calcKvalPoints(bids: List<BidResultResponse.BidResultData>, eventId: Int, trackLength: Int): List<RoboraceBidResult> {
    val results = bids.mapNotNull { bid ->
        val time = bid.programs.firstOrNull()
            ?.programStages
            ?.firstOrNull { it.name == "Квалификация" }
            ?.results
            ?.get("best")
            ?.arResults
            ?.lastOrNull()
        if (time != null)
            RoboraceBidResult(bid.teamName).apply { kvalTime = time.toDouble() }
        else null
    }

    println("eventId = ${eventId}")
    println("results = ${results}")

    val minKvalTime = results.minOf { it.kvalTime }
    val maxKvalTime = trackLength * 2.0
    results.forEach { result ->
        if (result.kvalTime <= maxKvalTime) {
            result.kvalPoints = 10 * (1 - (result.kvalTime - minKvalTime) / (maxKvalTime - minKvalTime))
        }
    }
    return results
}

private fun calcOtborPoints(results: List<RoboraceBidResult>, bids: List<BidResultResponse.BidResultData>) {
    setOtborLaps(results, bids, "Отборочный заезд")
    var maxOtborLaps = results.maxOf { it.otborLaps }
    if (maxOtborLaps == 0) {
        setOtborLaps(results, bids, "Финальный заезд")
        maxOtborLaps = results.maxOf { it.otborLaps }
    }
    if (maxOtborLaps > 0) {
        results
            .filter { it.otborLaps > 0 }
            .forEach { result ->
                result.otborPoints = 10.0 * result.otborLaps / maxOtborLaps
            }
    }
}

private fun setOtborLaps(results: List<RoboraceBidResult>, bids: List<BidResultResponse.BidResultData>, raceName: String) {
    results.forEach { result ->
        val finalData = bids.firstOrNull { it.teamName == result.teamName }!!
            .programs.first()
            .programStages
            .firstOrNull { it.name == raceName }
        finalData?.let {
            it.results!!["best"]?.arResults?.let { numbers ->
                result.otborLaps = numbers.first().toInt()
                result.otborTime = numbers.last().toDouble()
            }
        }
    }
}

private fun calcFinalPoints(results: List<RoboraceBidResult>, bids: List<BidResultResponse.BidResultData>) {
    results.forEach { result ->
        val finalData = bids.firstOrNull { it.teamName == result.teamName }!!
            .programs.first()
            .programStages
            .firstOrNull { it.name == "Финальный заезд" }
        finalData?.let {
            result.finalPlace = it.place
            result.finalPoints = pointsForFinalPlace(it.place)
        }
    }
}

private fun pointsForFinalPlace(place: Int) =
    when (place) {
        1 -> 180
        2 -> 100
        3 -> 60
        4 -> 40
        5 -> 20
        else -> 0
    }

data class RoboraceBidResult(val teamName: String) {
    var kvalTime: Double = 0.0
    var kvalPoints: Double = 0.0

    var otborLaps: Int = 0
    var otborTime: Double = 0.0
    var otborPoints: Double = 0.0

    var finalPlace: Int = 0
    var finalPoints: Int = 0

    fun totalPoints() = Math.round(kvalPoints + otborPoints + finalPoints).toInt()

    override fun toString() = "RoboraceBidResult(teamName='$teamName' totalPoints=${totalPoints()})"
}
