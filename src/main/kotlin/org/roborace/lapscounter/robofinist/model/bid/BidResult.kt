package org.roborace.lapscounter.robofinist.model.bid

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.roborace.lapscounter.robofinist.model.BaseRequest

data class BidResultRequest(
    @get:JsonProperty("bid_id") val bidId: Int,
) : BaseRequest(url = "event/bid/result/byBidList")

data class BidResultResponse(
    val data: BidResultData?,
)

data class BidResultData(
    val programs: List<BidResultProgram>?,
)

data class BidResultProgram(
    val id: Int,
    val name: String,
    @param:JsonProperty("program_stages") val programStages: List<BidResultStage>?,
)

data class BidResultStage(
    val id: Int,
    val name: String,
    val status: Int?,
    val place: Int?,
    @JsonDeserialize(using = BidResultAttemptsDeserializer::class)
    val results: BidResultAttempts?,
)

data class BidResultAttempts(
    val best: BidResultAttempt? = null,
    val attempts: MutableMap<String, BidResultAttempt> = mutableMapOf()
) {
    @JsonAnySetter
    fun setAttempt(key: String, value: BidResultAttempt) {
        if (key != "best") {
            attempts[key] = value
        }
    }
}

data class BidResultAttempt(
    val id: Int?,
    @param:JsonProperty("program_stage_id") val programStageId: Int?,
    @param:JsonProperty("bid_id") val bidId: Int?,
    val number: Int?,
    val disqualification: Int?,
    val reason: String?,
    val params: List<Double>?,
)

class BidResultAttemptsDeserializer : JsonDeserializer<BidResultAttempts?>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BidResultAttempts? {
        val node: JsonNode = p.codec.readTree(p)
        if (node.isArray) {
            if (node.isEmpty) {
                return null
            }
            val attempts = BidResultAttempts()
            node.forEachIndexed { index, attemptNode ->
                val attempt = p.codec.treeToValue(attemptNode, BidResultAttempt::class.java)
                attempts.attempts[index.toString()] = attempt
            }
            return attempts
        }
        return p.codec.treeToValue(node, BidResultAttempts::class.java)
    }
}
