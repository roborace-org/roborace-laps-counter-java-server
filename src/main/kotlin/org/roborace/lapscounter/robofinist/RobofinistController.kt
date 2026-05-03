package org.roborace.lapscounter.robofinist

import org.roborace.lapscounter.robofinist.model.event.EventDto
import org.roborace.lapscounter.robofinist.model.program.ProgramDto
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

}
