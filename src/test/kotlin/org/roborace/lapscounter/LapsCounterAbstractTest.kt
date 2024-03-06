package org.roborace.lapscounter

import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.Awaitility
import org.awaitility.Durations
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.roborace.lapscounter.client.WebsocketClient
import org.roborace.lapscounter.domain.State
import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.api.Message
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.lang.Thread.sleep
import java.net.URI

internal abstract class LapsCounterAbstractTest {
    @Value("\${local.server.port}")
    private val port = 8888

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    protected lateinit var ui: WebsocketClient


    @BeforeEach
    fun setUpAbstract() {
        sleep(50)
        ui = createClient("UI").also {
            givenReadyState(it)
        }

        println("Given state READY")
    }

    @AfterEach
    fun tearDownAbstract() {
        ui.closeClient()
    }

    protected fun createClient(name: String) = WebsocketClient(URI(wsServer(port)), name)

    protected fun createAndInitRobot(name: String, serial: Int) =
        createClient(name).also {
            shouldReceiveState(it, State.READY)
            sendMessage(it, Message(Type.ROBOT_INIT, serial = serial))
            Awaitility.await().until { ui.hasMessageWithType(Type.LAP) }
            Awaitility.await().until { it.hasMessageWithType(Type.LAP) }
        }

    private fun givenReadyState(client: WebsocketClient) = shouldReceiveState(client, State.READY)

    protected fun givenRunningState() {
        sendCommandAndCheckState(State.STEADY)
        sendCommandAndCheckState(State.RUNNING)
    }


    protected fun sendCommandAndCheckState(state: State) {
        sendCommand(state)
        shouldReceiveState(ui, state)
    }

    protected fun sendState() = sendMessage(ui, Message(Type.STATE))

    protected fun sendCommand(state: State) = sendMessage(ui, Message(Type.COMMAND, state = state))

    protected fun sendTimeRequestCommand(raceTimeLimit: Long) =
        sendMessage(ui, Message(Type.TIME, raceTimeLimit = raceTimeLimit))

    protected fun sendMessage(client: WebsocketClient, message: Message) =
        client.sendMessage(objectMapper.writeValueAsString(message))

    protected fun shouldReceiveState(client: WebsocketClient, state: State) {
        shouldReceiveType(client, Type.STATE) {
            assertThat(it.state, equalTo(state))
        }
    }

    protected fun shouldReceiveType(
        client: WebsocketClient,
        type: Type,
        assert: (it: Message) -> Unit = {}
    ) {
        Awaitility.await().untilAsserted {
            client.hasMessageWithType(type)
            assert(client.lastMessage)
        }
    }

    companion object {
        const val FIRST_SERIAL: Int = 100
        const val SECOND_SERIAL: Int = 101

        private fun wsServer(port: Int) = "ws://localhost:$port/ws"
        const val TIME_SEND_INTERVAL: Long = 800L

        @JvmStatic
        @BeforeAll
        fun beforeAllAbstract() {
            Awaitility.setDefaultTimeout(Durations.FIVE_SECONDS)
            Awaitility.setDefaultPollInterval(Durations.ONE_MILLISECOND)
        }
    }
}