package org.roborace.lapscounter.service.robofinist.stat

import org.roborace.lapscounter.service.robofinist.Robofinist
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/robofinist")
class EventBids {
    val robofinist2 = Robofinist()

    @GetMapping("/event-bids")
    fun getStat(@RequestParam("eventId") eventId: Int) = formatResult(getBids(eventId))

    private fun getBids(eventId: Int) =
        robofinist2.eventProgramsSearch(eventId)
            .filter { it.key.startsWith("Roborace") }
            .flatMap { (program, programId) ->
                robofinist2.getBids(programId)!!.data
                    .map { bid -> ProgramAndBid(program, bid.name) }
            }


    data class ProgramAndBid(
        val programName: String,
        val bidName: String,
    )

    fun formatResult(stat: List<ProgramAndBid>): String {
        val result = StringBuilder()

        stat.groupBy { it.programName }
            .forEach { (programName, bids) ->

                val line = mutableListOf(programName)
                line.addAll(bids.map { it.bidName })

                result.append(line.joinToString("\n")).append("\n\n")
            }
        return result.toString()
    }

}

fun mainE() {
    println(EventBids().getStat(1246))
}