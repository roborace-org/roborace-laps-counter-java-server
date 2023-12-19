package org.roborace.lapscounter.alice.domain

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.util.UUID

@JsonNaming(SnakeCaseStrategy::class)
data class WebhookResponse(
    val response: Response,
    val version: String = "1.0",
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(SnakeCaseStrategy::class)
    data class Response(
        val text: String? = null,
        val tts: String? = null,
        val shouldListen: Boolean = true,
        val directives: Directives? = null,
    )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(SnakeCaseStrategy::class)
data class Directives(
    val startAccountLinking: StartAccountLinking? = null,
    val printTextInMessageView: PrintTextInMessageViewDirective? = null,
    var getNextResponse: GetNextResponseDirective? = null,
) {
    object StartAccountLinking
}

@JsonNaming(SnakeCaseStrategy::class)
data class PrintTextInMessageViewDirective(
    val messageViewId: UUID,
    val text: String,
    val symbolsPerSecond: Float = 0f,
    val isEnd: Boolean = false,
    val shouldRewrite: Boolean = false,
    val prefetchAfterMs: Long = 0,
)

@JsonNaming(SnakeCaseStrategy::class)
data class GetNextResponseDirective(
    @JsonProperty("message_view_id")
    val messageViewId: UUID,
)