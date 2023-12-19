package org.roborace.lapscounter.alice.rest

import org.roborace.lapscounter.alice.domain.Directives
import org.roborace.lapscounter.alice.domain.GetNextResponseDirective
import org.roborace.lapscounter.alice.domain.PrintTextInMessageViewDirective
import org.roborace.lapscounter.alice.domain.WebhookRequest
import org.roborace.lapscounter.alice.domain.WebhookResponse
import org.roborace.lapscounter.alice.service.AliceService
import org.roborace.lapscounter.domain.State
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AliceSkillController {

    @Autowired
    private lateinit var aliceService: AliceService

    @GetMapping
    fun ping() = "Hi"

    @PostMapping("/alice")
    fun process(@RequestBody request: WebhookRequest): WebhookResponse {
        // println("request = $request")
        // println("alice request")

        val answer = aliceService.getUpdates()
        val message = PrintTextInMessageViewDirective(
            messageViewId = UUID.randomUUID(),
            text = answer,
            symbolsPerSecond = 100.0F,
            isEnd = false,
            prefetchAfterMs = if (answer.isEmpty() && aliceService.raceState.get() != State.RUNNING) 2000 else 1000,
        )
        val response: WebhookResponse.Response = WebhookResponse.Response(
            text = answer,
            tts = answer,
            shouldListen = false,
            directives = Directives(
                printTextInMessageView = message,
                getNextResponse = GetNextResponseDirective(UUID.randomUUID()),
            ),
        )

        // println("response = ${response}")

        return WebhookResponse(response)
    }
}
