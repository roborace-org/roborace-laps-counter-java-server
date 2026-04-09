package org.roborace.lapscounter.service.robofinist.stat

import org.roborace.lapscounter.service.robofinist.Robofinist
import org.roborace.lapscounter.service.robofinist.model.winners.WinnersSearchResponse


@Suppress("MagicNumber")
fun main46() {

    val robofinist = Robofinist()

    val eventsByTypes = linkedMapOf(
//        "МОЛР" to listOf(1006, 1015, 1031, 1032, 1033, 1143),
//        "КОР" to listOf(990, 1007, 1025, 1026, 1028, 1034),
//        "МОЛР КУБОК" to listOf(1149),
//        "КОР КУБОК" to listOf(1148),
//        "МНРТ" to listOf(1030),
        "МОЛР" to listOf(1246,1255,1283,1328,1433),
        "КОР" to listOf(1210,1270,1303,1349,1408),
        "МОЛР КУБОК" to listOf(1414),
        "КОР КУБОК" to listOf(1413),
        "МНРТ" to listOf(1415)
    )

    val mapResult = mutableMapOf<String, Map<String, Map<Int, Int>>>()

    eventsByTypes.forEach { entry ->
        val championship = entry.key
        val mapForEvents = getMapForEvents(robofinist, eventIds = entry.value)
        println("mapForEvents = ${mapForEvents}")
        mapResult[championship] = mapForEvents
    }

    formatResultMedals(mapResult)


}

private fun getMapForEvents(robofinist: Robofinist, eventIds: List<Int>) =
    eventIds.flatMap { eventId ->
        robofinist.eventProgramsSearch(eventId)
            .values.flatMap { programId ->
                robofinist.getWinners(programId)!!.data
                    .filter { it.place <= 3 }
                    .map { it.toWinnerStat() }
            }
    }
        .groupBy({ e -> e.orgName }) { e -> e.place }
        .mapValues { orgPlaces -> orgPlaces.value.groupingBy { it }.eachCount() }

private fun WinnersSearchResponse.Winner.toWinnerStat() =
    WinnerStat(place, if (bid.organizations.isEmpty()) "Без клуба" else bid.organizations.first().name)


// WINNERS
data class WinnerStat(val place: Int, val orgName: String)


private fun formatResultMedals(stat: MutableMap<String, Map<String, Map<Int, Int>>>) {
    val clubs = stat.values.flatMap { it.keys }.toSet().sorted()
    println("clubs = $clubs")

    val header = mutableListOf("")
    stat.keys.forEach { event ->
        header.add("")
        header.add(event)
        header.add("")
    }
    println(header.joinToString(CSV_SEPARATOR))
    header.clear()
    header.add("")
    stat.keys.forEach { _ ->
        header.add("1")
        header.add("2")
        header.add("3")
    }
    println(header.joinToString(CSV_SEPARATOR))

    clubs.forEach { club ->
        val line = mutableListOf(club)
        stat.values.forEach { event ->
            val clubStat = event[club] ?: mapOf()
            line.add((clubStat[1] ?: 0).toString())
            line.add((clubStat[2] ?: 0).toString())
            line.add((clubStat[3] ?: 0).toString())
        }
        println(line.joinToString(CSV_SEPARATOR))
    }


}
