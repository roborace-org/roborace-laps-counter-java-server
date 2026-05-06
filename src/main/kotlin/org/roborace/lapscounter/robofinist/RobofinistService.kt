package org.roborace.lapscounter.robofinist

import mu.KotlinLogging
import org.roborace.lapscounter.robofinist.model.AttemptResult
import org.roborace.lapscounter.robofinist.model.QualificationResult
import org.roborace.lapscounter.robofinist.model.bid.Bid
import org.roborace.lapscounter.robofinist.model.bid.BidStatus
import org.roborace.lapscounter.robofinist.model.event.Event
import org.roborace.lapscounter.robofinist.model.event.EventDto
import org.roborace.lapscounter.robofinist.model.program.ProgramDto
import org.roborace.lapscounter.robofinist.model.stage.Stage
import org.roborace.lapscounter.robofinist.model.stage.StageStatus
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

    fun getStages(programId: Long): List<Stage> = robofinistClient.getStages(programId = programId).data

    fun getBids(programId: Long): List<Bid> =
        robofinistClient.getBids(programId = programId).data
            .sortedBy { bid -> bid.status }

    fun markBidParticipated(bidId: Int) {
        robofinistClient.changeBidStatus(bidId = bidId, statusId = BidStatus.PARTICIPATED.code)
    }

    fun markBidAbsence(bidId: Int) {
        robofinistClient.changeBidStatus(bidId = bidId, statusId = BidStatus.ABSENCE.code)
    }

    fun startStage(stageId: Long) {
        robofinistClient.editStage(stageId = stageId, status = StageStatus.FORMING.code)
        robofinistClient.editStage(stageId = stageId, status = StageStatus.IN_PROGRESS.code)
    }

    fun createResult(stageId: Int, bidId: Int, number: Int, laps: Int, time: Double) {
        robofinistClient.createResult(stageId = stageId, bidId = bidId, number = number, laps = laps, time = time)
    }

    fun disqualifyResult(stageId: Int, bidId: Int, number: Int) {
        robofinistClient.disqualifyResult(stageId = stageId, bidId = bidId, number = number)
    }

    fun getQualificationResults(programId: Long, stageId: Long): List<QualificationResult> {
        val bids = getBids(programId).filter { it.status == BidStatus.PARTICIPATED.code }
        return bids.map { bid ->
            val response = robofinistClient.getBidResults(bid.id)
            val stage = response.data?.programs
                ?.flatMap { it.programStages ?: emptyList() }
                ?.find { it.id == stageId.toInt() }
            
            val attemptsMap = stage?.results?.attempts ?: emptyMap()
            
            fun getAttemptResult(index: Int): AttemptResult {
                val attempt = attemptsMap[index.toString()]
                val disqualified = attempt?.disqualification == 1
                val laps = if (disqualified) null else attempt?.params?.getOrNull(0)?.toInt()
                val time = if (disqualified) null else attempt?.params?.getOrNull(1)
                return AttemptResult(laps, time, disqualified)
            }
            
            val attempts = listOf(getAttemptResult(0), getAttemptResult(1), getAttemptResult(2))
            
            val bestAttempt = attempts
                .filter { !it.disqualified && it.laps != null && it.time != null && it.time > 0 }
                .sortedWith(compareByDescending<AttemptResult> { it.laps }.thenBy { it.time })
                .firstOrNull()
            
            QualificationResult(
                bidId = bid.id,
                name = bid.name,
                best = bestAttempt,
                attempts = attempts,
                place = null
            )
        }.sortedWith(compareByDescending<QualificationResult> { it.best?.laps ?: 0 }.thenBy(nullsLast()) { it.best?.time })
         .mapIndexed { index, result -> result.copy(place = index + 1) }
    }

    companion object {
        private val korRegex = "Куб.+ по образовательной робототехнике".toRegex()
        private val molrRegex = "Минская открытая лига робототехники".toRegex()
    }
}
