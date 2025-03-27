package org.roborace.lapscounter.service.robofinist.stat

import org.roborace.lapscounter.service.robofinist.Robofinist
import org.roborace.lapscounter.service.robofinist.model.bid.BidParticipantsResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

val robofinist2 = Robofinist()

@RestController
@RequestMapping("/robofinist")
class ClubParticipantStatByTrainer {

    @GetMapping("/club-participant-stat-by-trainer")
    fun getStat(@RequestParam("eventId") eventId: Int): String {
        return formatResult2(getMapForEvents(eventIds = listOf(eventId)))
    }

    public fun getMapForEvents(eventIds: List<Int>) =
        eventIds.flatMap { eventId -> processEvent(eventId) }
            .groupBy { e -> e.orgName }
            .mapValues { entry -> getClubStat(entry.value) }

    private fun processEvent(eventId: Int): List<ClubEvent> =
        robofinist2.eventProgramsSearch(eventId)
//        .filter { it.key.startsWith("Roborace.") }
            .values.flatMap { programId ->
                robofinist2.getBids(programId)!!.data
                    .filter {
                        it.status in setOf(
//                    0, // черновик
                            1, // на рассмотрении/отправлена
                            2, // требует уточнения
//                    3, // удалена
//                    4, // отклонена
                            5, // принята
                            6, // приняла участие
//                    7, // неявка
                        )
                    }
                    .flatMap { bid -> setClubFromTeacher(robofinist2.getBidParticipants(bid.id)!!.data) }
                    .filter { it.mentor == 0 } // not a teacher

            }
            .groupBy { e -> e.organization?.name?.replace(",", ";") ?: "Без клуба" }
            .map { entry -> getClubEvent(entry.key, entry.value) }

    fun setClubFromTeacher(data: List<BidParticipantsResponse.Participant>): List<BidParticipantsResponse.Participant> {
        print("orgnames = ${data.map { it.organization?.name }}")

        val mentorOrganizations = data.filter { it.mentor != 0 }
            .map { it.organization?.name }
            .distinct()
            .filterNotNull()

        if (mentorOrganizations.isNotEmpty()) {
            val finalList = mutableListOf<BidParticipantsResponse.Participant>()
            mentorOrganizations.forEach { name ->
                val organization = BidParticipantsResponse.Organization(0, name)
                finalList.addAll(
                    data.filter { it.mentor == 0 }.map { it.copy(organization = organization) }
                )
            }
            return finalList
        }

        return data


//        val mentor = data.firstOrNull { partnerNames.contains(it.organization?.name) }
//            ?.also { print("found org = ${it.organization?.name}") }
//            ?: data.firstOrNull { it.mentor != 0 }?.also { print("final org = ${it.organization?.name}") }

//        return if (mentor != null)
//            data.map {
//                it.organization = mentor.organization
//                it
//            } else data
    }

    private fun getClubEvent(orgName: String, list: List<BidParticipantsResponse.Participant>): ClubEvent {
        val uniqParticipants = list.map { it.firstName + it.lastName }.distinct()
        return ClubEvent(
            orgName = orgName,
            uniqParticipants = uniqParticipants,
            seats = uniqParticipants.count(),
            bids = list.size,
        )
    }

    private fun getClubStat(list: List<ClubEvent>): ClubStat2 = ClubStat2(
        uniqParticipants = list.flatMap { it.uniqParticipants }.distinct().count(),
        seats = list.sumOf { it.seats },
        bids = list.sumOf { it.bids },
        names = list.flatMap { it.uniqParticipants }.distinct().joinToString(", "),
    )


    data class ClubStat2(
        val uniqParticipants: Int = 0,
        val seats: Int = 0,
        val bids: Int = 0,
        val names: String,
    )

    data class ClubEvent2(
        val orgName: String = "",
        val uniqParticipants: List<String> = listOf(),
        val seats: Int = 0,
        val bids: Int = 0,
    )


    public fun formatResult2(stat: Map<String, ClubStat2>): String {
        val clubs = stat.keys.sorted()
        val result = StringBuilder()

        val header = mutableListOf("")
        header.add("уникальные участники за год")
        header.add("сумма посадочных мест на этапах")
        header.add("человеко-заявки (с учетом разных категорий 1го человека)")
        result.append(header.joinToString(CSV_SEPARATOR)).append("<br>\n")

        clubs.forEach { club ->
            val line = mutableListOf(club)
            val clubStat = stat[club]!!

            line.add((clubStat.uniqParticipants).toString())
            line.add((clubStat.seats).toString())
            line.add((clubStat.bids).toString())

            result.append(line.joinToString(CSV_SEPARATOR)).append("<br>\n")
        }
        return result.toString()
    }


}

@Suppress("MagicNumber")
fun main123() {

    val eventsByTypes = linkedMapOf(
//        "МОЛР" to listOf(1006, 1015, 1031, 1032, 1033, 1143),
//        "КОР" to listOf(990, 1007, 1025, 1026, 1028, 1034),
//        "МОЛР КУБОК" to listOf(1149),
//        "КОР КУБОК" to listOf(1148),
//        "МНРТ" to listOf(1030),
//        "КОР24/25" to listOf(1210, 1270),
//        "МОЛР24/25" to listOf(1246, 1255),
        "temp" to listOf(1255),
    )

    val stat = ClubParticipantStatByTrainer()
    val mapForEvents = stat.getMapForEvents(eventIds = eventsByTypes.values.flatten())
    println(mapForEvents)
    println(stat.formatResult2(mapForEvents))
}

