package org.roborace.lapscounter.service.robofinist.stat

import org.roborace.lapscounter.service.robofinist.Robofinist
import org.roborace.lapscounter.service.robofinist.model.bid.BidParticipantsResponse

val robofinist = Robofinist()

@Suppress("MagicNumber")
fun main1() {

    val eventsByTypes = linkedMapOf(
//        "МОЛР" to listOf(1006, 1015, 1031, 1032, 1033, 1143),
//        "КОР" to listOf(990, 1007, 1025, 1026, 1028, 1034),
//        "МОЛР КУБОК" to listOf(1149),
//        "КОР КУБОК" to listOf(1148),
//        "МНРТ" to listOf(1030),
//        "КОР24/25" to listOf(1210),
        "МОЛР24/25" to listOf(1246),
    )

    val mapForEvents = getMapForEvents(eventIds = eventsByTypes.values.flatten())

    formatResult(mapForEvents)
}

fun getMapForEvents(eventIds: List<Int>) =
    eventIds.flatMap { eventId -> processEvent(eventId) }
        .groupBy { e -> e.orgName }
        .mapValues { entry -> getClubStat(entry.value) }

private fun processEvent(eventId: Int): List<ClubEvent> =
    robofinist.eventProgramsSearch(eventId)
//        .filter { it.key.startsWith("Roborace.") }
        .values.flatMap { programId ->
            robofinist.getBids(programId)!!.data
                .filter { it.status == 6 } // participated
                .flatMap { bid ->
                    robofinist.getBidParticipants(bid.id)!!.data
                }
                .filter { it.mentor == 0 } // not a teacher
        }
        .groupBy { e -> e.organization?.name?.replace(",", ";") ?: "Без клуба" }
        .map { entry -> getClubEvent(entry.key, entry.value) }

private fun getClubEvent(orgName: String, list: List<BidParticipantsResponse.Participant>): ClubEvent {
    val uniqParticipants = list.map { it.firstName + it.lastName }.distinct()
    return ClubEvent(
        orgName = orgName,
        uniqParticipants = uniqParticipants,
        seats = uniqParticipants.count(),
        bids = list.size,
    )
}

private fun getClubStat(list: List<ClubEvent>): ClubStat = ClubStat(
    uniqParticipants = list.flatMap { it.uniqParticipants }.distinct().count(),
    seats = list.sumOf { it.seats },
    bids = list.sumOf { it.bids },
)


data class ClubStat(
    val uniqParticipants: Int = 0,
    val seats: Int = 0,
    val bids: Int = 0,
)

data class ClubEvent(
    val orgName: String = "",
    val uniqParticipants: List<String> = listOf(),
    val seats: Int = 0,
    val bids: Int = 0,
)


private fun formatResult(stat: Map<String, ClubStat>) {
    val clubs = stat.keys.sorted()
    println("clubs = $clubs")

    val header = mutableListOf("")
    header.add("уникальные участники за год")
    header.add("сумма посадочных мест на этапах")
    header.add("человеко-заявки (с учетом разных категорий 1го человека)")
    println(header.joinToString(CSV_SEPARATOR))

    clubs.forEach { club ->
        val line = mutableListOf(club)
        val clubStat = stat[club]!!

        line.add((clubStat.uniqParticipants).toString())
        line.add((clubStat.seats).toString())
        line.add((clubStat.bids).toString())


        println(line.joinToString(CSV_SEPARATOR))
    }


}
