package org.roborace.lapscounter.service.robofinist.stat

import org.roborace.lapscounter.service.robofinist.Program
import org.roborace.lapscounter.service.robofinist.Robofinist
import kotlin.collections.associateWith


@Suppress("MagicNumber")
fun main2() {

    val robofinist = Robofinist()

//    val events = listOf(1246, 1255, 1283, 1328, 1433) // molr
//    val events = listOf(1210, 1270, 1303, 1349, 1408) // kor

    val events = listOf(1510, 1583, 1536, 1649, 1669) // 2026

    val fullJrStat = FullJrStat(events)
    fullJrStat.fillGameData(robofinist)

    formatResultMedals(fullJrStat)

}

data class FullJrStat(
    val events: MutableMap<Int, Double>
) {
    val allParticipants: MutableList<WinnerRoboraceJr> = mutableListOf()
    lateinit var list: List<WinnerRoboraceCupJr>

    constructor(eventIds: List<Int>) : this(eventIds.associateWith { 0.0 }.toMutableMap())

    fun fillGameData(robofinist: Robofinist) {
        events.keys.forEach { eventId ->
            val participants = getParticipantsOfEvent(robofinist, eventId)
            val koef = koefForSize(participants.size)
            participants.forEach { it.pointsKoef = it.points * koef }
            events[eventId] = koef
            allParticipants.addAll(participants)
        }

        list = allParticipants
            .groupBy { e -> e.teamName }
            .map { (teamName, list) -> WinnerRoboraceCupJr(teamName, list) }
            .sortedByDescending { it.allPoints }

        var place = 1
        list.forEach { it.place = place++ }
    }

    private fun getParticipantsOfEvent(robofinist: Robofinist, eventId: Int): List<WinnerRoboraceJr> {
        val programs = robofinist.eventProgramsSearch(eventId)
            .filter { it.key == Program.ROBORACE_OK_JR.text }
            .values
        if (programs.isEmpty() || programs.size > 1) {
            println("Programs not found ot more than 1 for event ${eventId} programs = ${programs}")
            return emptyList()
        }
        val programId = programs.first()

        return robofinist.getWinners(programId)!!.data
            .map { WinnerRoboraceJr(it.place, it.bid.name, eventId) }
    }

    private fun koefForSize(size: Int): Double = when {
        size <= 3 -> 0.8
        size >= 9 -> 1.2
        else -> 1.0
    }
}


data class WinnerRoboraceJr(
    val place: Int,
    val teamName: String,
    val eventId: Int,
    val points: Int = calcPointsForPlace(place),
) {
    var pointsKoef: Double = 0.0

    companion object {
        private fun calcPointsForPlace(place: Int) =
            when {
                place == 1 -> 10
                place == 2 -> 8
                place < 9 -> 9 - place
                else -> 0
            }
    }
}


data class WinnerRoboraceCupJr(
    val teamName: String,
    val list: List<WinnerRoboraceJr>,
    val allPoints: Double = list.sumOf { it.pointsKoef },
) {
    var place: Int = 0
}


private fun formatResultMedals(stat: FullJrStat) {
    val header = mutableListOf("")
    stat.events.forEach { event ->
        header.add(event.key.toString())
        header.add(event.value.toString())
        header.add("")
    }
    header.add("")
    header.add("")

    println(header.joinToString(CSV_SEPARATOR))
    header.clear()
    header.add("")
    stat.events.forEach { _ ->
        header.add("place")
        header.add("points")
        header.add("kpoints")
    }
    header.add("place")
    header.add("total points")
    println(header.joinToString(CSV_SEPARATOR))

    stat.list.forEach { team ->
        val line = mutableListOf(team.teamName)
        stat.events.keys.forEach { eventId ->
            val teamEventResult = team.list.firstOrNull { it.eventId == eventId }
            if (teamEventResult != null) {
                line.add(teamEventResult.place.toString())
                line.add(teamEventResult.points.toString())
                line.add("%,.2f".format(teamEventResult.pointsKoef))
            } else {
                line.add("")
                line.add("")
                line.add("")
            }
        }
        line.add(team.place.toString())
        line.add("%,.2f".format(team.allPoints))
        println(line.joinToString(CSV_SEPARATOR))
    }


}
