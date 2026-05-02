package org.roborace.lapscounter.service.robofinist.stat

import org.roborace.lapscounter.service.robofinist.Program
import org.roborace.lapscounter.service.robofinist.Robofinist


@Suppress("MagicNumber")
fun main2() {

    val robofinist = Robofinist()

//    val events = listOf(1246, 1255, 1283, 1328, 1433) // molr
//    val events = listOf(1210, 1270, 1303, 1349, 1408) // kor

    val events = listOf(1510, 1583, 1536, 1649, 1669) // 2026


    val mapResult = getMapForEvents(robofinist, eventIds = events)
//    val mapResult = listOf(
//        WinnerRoboraceCup("as1", 20, listOf(WinnerRoborace(1, "asdsad", 1210))),
//        WinnerRoboraceCup("asdfgv", 15, listOf(WinnerRoborace(2, "___", 1270))),
//    )
    println("mapResult = ${mapResult}")

    formatResultMedals(mapResult, events)

}

private fun getMapForEvents(robofinist: Robofinist, eventIds: List<Int>) =
    eventIds.flatMap { eventId -> getEventData(robofinist, eventId) }
        .groupBy { e -> e.teamName }
        .map { (teamName, list) ->
            WinnerRoboraceCupJr(teamName, list.sumOf { it.points }, list)
        }.sortedByDescending { it.allPoints }


private fun getEventData(robofinist: Robofinist, eventId: Int) =
    robofinist.eventProgramsSearch(eventId)
        .filter { it.key == Program.ROBORACE_OK_JR.text }
        .values
        .flatMap { programId ->
            robofinist.getWinners(programId)!!.data
                .map { WinnerRoboraceJr(it.place, it.bid.name, eventId) }
        }.also { eventResult ->
            val koef = when {
                eventResult.size <= 3 -> 0.8
                eventResult.size >= 9 -> 1.2
                else -> 1.0
            }
            eventResult.forEach { it.points *= koef }
        }


data class WinnerRoboraceJr(val place: Int, val teamName: String, val eventId: Int, var points: Double = calcPointsForPlace(place).toDouble())

private fun calcPointsForPlace(place: Int) =
    when {
        place == 1 -> 10
        place == 2 -> 8
        place < 9 -> 9 - place
        else -> 0
    }

data class WinnerRoboraceCupJr(val teamName: String, val allPoints: Double, val list: List<WinnerRoboraceJr>) {
    var place: Int = 0
}


private fun formatResultMedals(stat: List<WinnerRoboraceCupJr>, events: List<Int>) {
    val header = mutableListOf("")
    events.forEach { event ->
        header.add(event.toString())
        header.add("")
    }
    header.add("place")
    header.add("points")

    println(header.joinToString(CSV_SEPARATOR))
    header.clear()
    header.add("")
    events.forEach { _ ->
        header.add("place")
        header.add("points")
    }
    println(header.joinToString(CSV_SEPARATOR))

    var nextPlace = 1
    stat.forEach { team ->
        val line = mutableListOf(team.teamName)
        events.forEach { eventId ->
            val teamEventResult = team.list.firstOrNull { it.eventId == eventId }
            if (teamEventResult != null) {
                line.add(teamEventResult.place.toString())
                line.add(teamEventResult.points.toString())
            } else {
                line.add("")
                line.add("")
            }
        }
        line.add(nextPlace++.toString())
        line.add(team.allPoints.toString())
        println(line.joinToString(CSV_SEPARATOR))
    }


}
