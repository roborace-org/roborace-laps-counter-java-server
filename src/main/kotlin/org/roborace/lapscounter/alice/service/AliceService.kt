package org.roborace.lapscounter.alice.service

import org.roborace.lapscounter.alice.service.AliceNlgService.lapsIncPhrase
import org.roborace.lapscounter.alice.service.AliceNlgService.lapsPhrase
import org.roborace.lapscounter.alice.service.AliceNlgService.numberFemalePhrase
import org.roborace.lapscounter.alice.service.AliceNlgService.numberItPhrase
import org.roborace.lapscounter.alice.service.AliceNlgService.numberMalePhrase
import org.roborace.lapscounter.client.WebsocketClient
import org.roborace.lapscounter.domain.State
import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.api.Message
import org.roborace.lapscounter.service.util.Stopwatch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.net.URI
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Service
class AliceService(
    @Value("\${alice.timeout:2000}") private val timeout: Int,
    @Value("\${alice.fresh.seconds:5}") private val freshSeconds: Long,
) {

    @Autowired
    private lateinit var webServerAppCtxt: ServletWebServerApplicationContext

    @EventListener(ApplicationReadyEvent::class)
    fun aliceWebsocketClient() {
        aliceWebsocketClient = WebsocketClient(URI(WS_SERVER.format(webServerAppCtxt.webServer.port)), "Alice")
    }

    private var aliceWebsocketClient: WebsocketClient? = null

    companion object {
        private const val WS_SERVER = "ws://localhost:%d/ws"
    }

    internal val raceState: AtomicReference<State> = AtomicReference(State.READY)
    private val robots: ConcurrentHashMap<Int, Message> = ConcurrentHashMap()

    fun getUpdates(): String =
        aliceWebsocketClient?.let { client ->
            if (client.hasNoMessages()) {
                val stopwatch = Stopwatch()
                stopwatch.start()
                while (client.hasNoMessages()) {
                    Thread.sleep(10)
                    if (stopwatch.time() > timeout) return ""
                }
            }

            StringBuffer().apply {
                while (client.hasFreshMessage(freshSeconds)) {
                    client.pollFreshMessage(freshSeconds)?.let { message ->
                        processMessage(message)?.let { textResponse ->
                            append(textResponse).append('\n')
                        }
                    }
                    if (length > 50) break
                }
            }.trim().toString()
        } ?: "Ошибка инициализации"

    private fun processMessage(message: Message): String? = when (message.type) {
        Type.LAP_MAN -> "${numberMalePhrase(message.serial!!)} ${lapsIncPhrase(message.laps!!)}"

        Type.PIT_STOP -> "${numberMalePhrase(message.serial!!)} пит стоп"

        Type.PIT_STOP_FINISH -> "${numberMalePhrase(message.serial!!)} пит стоп окончен"

        // Type.COMMAND -> commandPhrase(message.state)

        Type.STATE -> processState(message)

        Type.TIME -> processTime(message)

        Type.ROBOT_REMOVE -> {
            robots.clear()
            null
        }

        Type.LAP -> {
            if (raceState.get() == State.READY) {
                // if (robots.put(message.serial, message) == null) {
                "${numberFemalePhrase(message.serial!!)} позиция — ${message.name}"
                // } else null
            } else if (raceState.get() == State.RUNNING) {
                "${numberItPhrase(message.place!!)} место — ${message.name}, ${message.laps} ${lapsPhrase(message.laps!!)}"
            } else null
        }

        else -> null
    }

    private var saidMinutes = 0L

    private fun processTime(message: Message): String? =
        message.time?.let {
            val duration = Duration.ofSeconds(it / 1000)
            val minutes = duration.toMinutes()
            if (minutes > 0 && saidMinutes != minutes) {
                saidMinutes = minutes
                if (message.raceTimeLimit != 0L) {
                    "${AliceNlgService.pastMinutesPhrase(minutes)} из ${message.raceTimeLimit!! / 60} минут"
                } else {
                    AliceNlgService.pastMinutesPhrase(minutes)
                }
            }
            else null
        }

    private fun processState(message: Message): String? =
        if (raceState.getAndSet(message.state) == message.state) null
        else
            when (message.state) {
                // State.READY -> answerForRobotStartPositions()
                State.READY -> "На старт"
                State.STEADY -> "Внимание"
                State.RUNNING -> "Марш"
                State.FINISH -> answerForRobotFinishPositions()
                else -> null
            }

    private fun answerForRobotStartPositions(): String =
        StringBuffer("Порядок роботов в заезде: ").apply {
            robots.values.sortedBy { it.serial }.forEach {
                append(numberFemalePhrase(it.serial!!))
                append(" позиция — ")
                append(it.name)
                append(". ")
            }
        }.toString()

    private fun answerForRobotFinishPositions(): String =
        StringBuffer("Гонка окончена! Места в заезде: ").apply {
            robots.values.sortedBy { it.place }.forEach {
                append(numberItPhrase(it.place!!))
                append(" место — ")
                append(it.name)
                append(", ")
                append(it.laps)
                append(' ')
                append(lapsPhrase(it.laps!!))
                append(". ")
            }
        }.toString()
}
