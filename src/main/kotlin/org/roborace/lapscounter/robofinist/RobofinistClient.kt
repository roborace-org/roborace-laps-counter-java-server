package org.roborace.lapscounter.robofinist

import mu.KotlinLogging
import org.roborace.lapscounter.robofinist.model.BaseRequest
import org.roborace.lapscounter.robofinist.model.bid.BidChangeStatusRequest
import org.roborace.lapscounter.robofinist.model.bid.BidChangeStatusResponse
import org.roborace.lapscounter.robofinist.model.bid.BidResultRequest
import org.roborace.lapscounter.robofinist.model.bid.BidResultResponse
import org.roborace.lapscounter.robofinist.model.bid.BidSearchRequest
import org.roborace.lapscounter.robofinist.model.bid.BidSearchResponse
import org.roborace.lapscounter.robofinist.model.bid.ResultCreateRequest
import org.roborace.lapscounter.robofinist.model.bid.ResultCreateResponse
import org.roborace.lapscounter.robofinist.model.event.EventsSearchRequest
import org.roborace.lapscounter.robofinist.model.event.EventsSearchResponse
import org.roborace.lapscounter.robofinist.model.program.ProgramSearchRequest
import org.roborace.lapscounter.robofinist.model.program.ProgramSearchResponse
import org.roborace.lapscounter.robofinist.model.stage.StageEditRequest
import org.roborace.lapscounter.robofinist.model.stage.StageEditResponse
import org.roborace.lapscounter.robofinist.model.stage.StageSearchRequest
import org.roborace.lapscounter.robofinist.model.stage.StageSearchResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

private val logger = KotlinLogging.logger {}

@Service
@ConditionalOnProperty("robofinist.enabled")
class RobofinistClient(
    private val restTemplate: RestTemplate,
    @param:Value("\${robofinist.token}") private var token: String,
    @param:Value("\${robofinist.url.v2}") val url: String,
) {

    fun getEvents(id: Int? = null, partnerId: Int? = null): EventsSearchResponse =
        execute<EventsSearchResponse>(EventsSearchRequest(id = id, partnerId = partnerId))

    fun getPrograms(eventId: Long): ProgramSearchResponse =
        execute<ProgramSearchResponse>(ProgramSearchRequest(eventId = eventId))

    fun getBids(programId: Long): BidSearchResponse =
        execute<BidSearchResponse>(BidSearchRequest(programId = programId))

    fun getStages(programId: Long): StageSearchResponse =
        execute<StageSearchResponse>(StageSearchRequest(programId = programId))

    fun changeBidStatus(bidId: Int, statusId: Int): BidChangeStatusResponse =
        execute<BidChangeStatusResponse>(BidChangeStatusRequest(bidId = bidId, statusId = statusId))

    fun getBidResults(bidId: Int): BidResultResponse =
        execute<BidResultResponse>(BidResultRequest(bidId = bidId))

    fun editStage(stageId: Long, status: Int): StageEditResponse =
        execute<StageEditResponse>(StageEditRequest(stageId = stageId, status = status))

    fun createResult(stageId: Int, bidId: Int, number: Int, laps: Int, time: Double): ResultCreateResponse =
        execute<ResultCreateResponse>(ResultCreateRequest.create(stageId, bidId, number, laps, time))

    fun disqualifyResult(stageId: Int, bidId: Int, number: Int): ResultCreateResponse =
        execute<ResultCreateResponse>(ResultCreateRequest.disqualify(stageId, bidId, number))

    private inline fun <reified T> execute(request: BaseRequest): T {
        try {
            request.token = token
            val response = restTemplate.postForEntity(url, request, T::class.java)
            logger.debug("Response: {}", response)
            return response.body!!
        } catch (e: Exception) {
            throw RuntimeException("Failed to invoke robofinist api", e)
        }
    }
}
