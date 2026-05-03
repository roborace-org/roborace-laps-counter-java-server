package org.roborace.lapscounter.robofinist

import mu.KotlinLogging
import org.roborace.lapscounter.robofinist.model.bid.Bid
import org.roborace.lapscounter.robofinist.model.event.Event
import org.roborace.lapscounter.robofinist.model.event.EventDto
import org.roborace.lapscounter.robofinist.model.program.ProgramDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

@Service
@ConditionalOnProperty("robofinist.enabled")
class RobofinistService(
    private val robofinistClient: RobofinistClient,
    @param:Value("\${robofinist.partnerId}") val partnerId: Int,
) {

    fun getEvents(): List<EventDto> {
        val today = LocalDate.now()
        return robofinistClient.getEvents(partnerId = partnerId).data
            .sortedBy { event ->
                event.beginAt?.let { parseDate(it)?.let { date -> abs(date.toEpochDay() - today.toEpochDay()) } }
                    ?: Long.MAX_VALUE
            }
            .map {
                EventDto(
                    it.id,
                    "#${it.id} ${shortEventName(it.name)}",
                    it.beginAt?.let { date -> parseDate(date) })
            }
    }

    private fun parseDate(dateStr: String): LocalDate? = runCatching {
        LocalDate.parse(dateStr.substringBefore(" "), DateTimeFormatter.ISO_LOCAL_DATE)
    }.getOrNull()


    private fun shortEventName(name: String): String = name
        .replace(korRegex, "КОР")
        .replace(molrRegex, "МОЛР")

    fun getEvent(id: Int): Event? = robofinistClient.getEvents(id = id).data.firstOrNull()

    fun getPrograms(eventId: Long): List<ProgramDto> =
        robofinistClient.getPrograms(eventId = eventId).data
            .filter { it.name.startsWith("Roborace") }
            .map { ProgramDto(it.id, it.name) }

    fun getBids(programId: Long): List<Bid> = robofinistClient.getBids(programId = programId).data


    companion object {
        private val korRegex = "Куб.+ по образовательной робототехнике".toRegex()
        private val molrRegex = "Минская открытая лига робототехники".toRegex()
    }
}
