package org.roborace.lapscounter.alice.domain

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(SnakeCaseStrategy::class)
data class WebhookRequest(
    val meta: WebhookRequestMeta,
    val request: Request,
    val session: Session,
) {
    @JsonNaming(SnakeCaseStrategy::class)
    data class Request(
        val command: String?,
        val originalUtterance: String?,
        val type: String,
    )
}

@JsonNaming(SnakeCaseStrategy::class)
data class WebhookRequestMeta(
    val interfaces: Map<String, Any>,
) {
    fun supportsPrintText() = interfaces.contains("supports_print_text_in_message_view")
}

@JsonNaming(SnakeCaseStrategy::class)
data class Session(
    val new: Boolean,
)