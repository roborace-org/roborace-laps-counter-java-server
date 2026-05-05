package org.roborace.lapscounter.robofinist

import org.roborace.lapscounter.robofinist.model.bid.Bid
import org.roborace.lapscounter.robofinist.model.event.EventDto
import org.roborace.lapscounter.robofinist.model.program.ProgramDto
import org.roborace.lapscounter.robofinist.model.stage.Stage
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/robofinist")
@ConditionalOnProperty("robofinist.enabled")
@CrossOrigin
class RobofinistController(
    private val robofinistService: RobofinistService,
) {

    @GetMapping("/events")
    fun getEvents(): List<EventDto> = robofinistService.getEvents()

    @GetMapping("/events/{eventId}/programs")
    fun getPrograms(@PathVariable eventId: Long): List<ProgramDto> = robofinistService.getPrograms(eventId)

    @GetMapping("/events/{eventId}")
    fun getEvent(@PathVariable eventId: Int) = robofinistService.getEvent(eventId)

    @GetMapping("/programs/{programId}/stages")
    fun getStages(@PathVariable programId: Long): List<Stage> = robofinistService.getStages(programId)

    @GetMapping("/programs/{programId}/bids")
    fun getBids(@PathVariable programId: Long): List<Bid> = robofinistService.getBids(programId)

    @PostMapping("/bids/{bidId}/participated")
    fun markBidParticipated(@PathVariable bidId: Int) = robofinistService.markBidParticipated(bidId)

    @PostMapping("/bids/{bidId}/absence")
    fun markBidAbsence(@PathVariable bidId: Int) = robofinistService.markBidAbsence(bidId)

    @PostMapping("/stages/{stageId}/start")
    fun startStage(@PathVariable stageId: Long) = robofinistService.startStage(stageId)

}
