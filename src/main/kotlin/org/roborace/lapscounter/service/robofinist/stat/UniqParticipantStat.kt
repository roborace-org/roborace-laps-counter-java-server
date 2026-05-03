package org.roborace.lapscounter.service.robofinist.stat

import org.roborace.lapscounter.service.robofinist.Robofinist
import org.roborace.lapscounter.service.robofinist.model.bids.BidsSearchResponse


const val CSV_SEPARATOR = ";"

data class CompetitionStat(
    val program: String,
    val bids: Int,
    val bidsParticipated: Int,
    val orgs: Int,
)

@Suppress("MagicNumber")
fun main4() {

    val robofinist = Robofinist()

    val events = linkedMapOf(
        //        958 to "23/24 Фестиваль науки Минск",
        //        10109 to "23/24 Пинск",
        //        1161 to "23/24 Зубренок",
        //        1154 to "23/24 Жлобин",
        //        1156 to "23/24 Жлобин РТК",

        //        799 to "22/23 Молр 1",
        //        891 to "22/23 Молр 4",
        //        930 to "22/23 Молр 6",
        //        787 to "22/23 Брест",
        //        807 to "22/23 Бобруйск",
        //        819 to "22/23 Минск",
        //        871 to "22/23 Могилев",
        //        875 to "22/23 Пинск",
        //        919 to "22/23 Минск Тибо",
        //        931 to "22/23 Мнрт X",

        1006 to "23/24 Молр 1",
        1015 to "23/24 Молр 2",
        1031 to "23/24 Молр 3",
        1032 to "23/24 Молр 4",
        1033 to "23/24 Молр 5",
        1143 to "23/24 Молр 6",
        990 to "23/24 Брест",
        1007 to "23/24 Могилев",
        1025 to "23/24 Гомель",
        1026 to "23/24 Витебск",
        1028 to "23/24 Гродно",
        1034 to "23/24 Минск",
        1030 to "23/24 Мнрт XI",
    )


    val statParticipants = events.entries.associate { event ->
        val eventPrograms = robofinist.eventProgramsSearch(eventId = event.key)
        val eventStat = eventPrograms.entries.map { entry ->
            competitionStat(robofinist, entry.value, entry.key)
        }
        event.value to eventStat
    }
    println("stat = $statParticipants")

    formatResult(statParticipants, events.values)

}


private val DEFAULT_STAT = CompetitionStat("", 0, 0, 0)

private fun formatResult(stat: Map<String, List<CompetitionStat>>, events: Collection<String>) {
    val categories = stat.flatMap { it.value }.map { it.program }.distinct().sorted()
    println("categories = $categories")

    val header = mutableListOf("")
    events.forEach { event ->
//        header.add("$event bids")
        header.add("$event participated")
//        header.add("$event orgs")
    }
    println(header.joinToString(CSV_SEPARATOR))

    categories.forEach { category ->
        val line = mutableListOf(category)
        events.forEach { event ->
            val compStat = stat[event]?.firstOrNull { cs -> cs.program == category } ?: DEFAULT_STAT
//            line.add(compStat.bids.toString())
            line.add(compStat.bidsParticipated.toString())
//            line.add(compStat.orgs.toString())
        }
        println(line.joinToString(CSV_SEPARATOR))
    }


}

private const val STATUS_PARTICIPATED = 6

private val defaultOrg = BidsSearchResponse.Organization(0, "")

private fun competitionStat(robofinist: Robofinist, programId: Int, programName: String): CompetitionStat {
//    return CompetitionStat(programName, 0, 0, 0)
    val bids = robofinist.getBids(programId = programId)!!
    val bidsCount = bids.data.size
    val bidsParticipatedCount = bids.data.filter { it.status == STATUS_PARTICIPATED }.size
    val orgsCount = bids.data
        .flatMap { it.organizations.ifEmpty { listOf(defaultOrg) } }
        .map { it.id }
        .toSet()
        .count()
    return CompetitionStat(programName, bidsCount, bidsParticipatedCount, orgsCount)
}