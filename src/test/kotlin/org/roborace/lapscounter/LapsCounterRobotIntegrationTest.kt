package org.roborace.lapscounter

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.roborace.lapscounter.client.WebsocketClient
import org.roborace.lapscounter.domain.State
import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.api.Message
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import java.lang.Thread.sleep
import kotlin.random.Random

@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
internal class LapsCounterRobotIntegrationTest : LapsCounterAbstractTest() {
    private lateinit var robot1: WebsocketClient

    @BeforeEach
    fun setUp() {
        sleep(50)
        robot1 = createClient("ROBOT1")
        shouldReceiveState(robot1, State.READY)
    }


    @AfterEach
    fun tearDown() {
        robot1.closeClient()
    }


    @Test
    fun testStateRobot() {
        sendCommandAndCheckState(State.STEADY)
        shouldReceiveState(robot1, State.STEADY)

        sendCommandAndCheckState(State.RUNNING)
        shouldReceiveState(robot1, State.RUNNING)

        sendCommandAndCheckState(State.FINISH)
        shouldReceiveState(robot1, State.FINISH)
    }

    @Test
    fun testRobotInit() {
        sendMessage(robot1, Message(Type.ROBOT_INIT, serial = FIRST_SERIAL))

        shouldReceiveType(robot1, Type.LAP) {
            assertThat(it.serial, equalTo(FIRST_SERIAL))
        }

        shouldReceiveType(ui, Type.LAP) {
            assertThat(it.serial, equalTo(FIRST_SERIAL))
            assertThat(it.name, equalTo("Robot $FIRST_SERIAL"))
        }
    }

    @Test
    fun testRobotInitWithName() {
        sendMessage(robot1, Message(Type.ROBOT_INIT, serial = FIRST_SERIAL, name = "MyCool Name"))

        shouldReceiveType(robot1, Type.LAP) {
            assertThat(it.serial, equalTo(FIRST_SERIAL))
        }

        shouldReceiveType(ui, Type.LAP) {
            assertThat(it.serial, equalTo(FIRST_SERIAL))
            assertThat(it.name, equalTo("MyCool Name"))
        }
    }

    @Test
    fun testRobotEdit() {
        sendMessage(robot1, Message(Type.ROBOT_INIT, serial = FIRST_SERIAL))

        shouldReceiveType(robot1, Type.LAP) {
            assertThat(it.serial, equalTo(FIRST_SERIAL))
        }

        shouldReceiveType(ui, Type.LAP) {
            assertThat(it.serial, equalTo(FIRST_SERIAL))
        }

        val newName = "WINNER ${Random.nextInt(10) + 200}"
        sendMessage(ui, Message(Type.ROBOT_EDIT, serial = FIRST_SERIAL, name = newName))

        shouldReceiveType(ui, Type.LAP) {
            assertThat(it.serial, equalTo(FIRST_SERIAL))
            assertThat(it.name, equalTo(newName))
        }


        sendMessage(ui, Message(Type.LAPS))
        shouldReceiveType(ui, Type.LAP) {
            assertThat(it.serial, equalTo(FIRST_SERIAL))
            assertThat(it.name, equalTo(newName))
        }
    }

    @Test
    fun testMaxRobots() {
        val robots = (1..MAX_ROBOTS)
            .map {
                sleep(10)
                createClient("ROBOT${it + 1}").also { client ->
                    shouldReceiveState(client, State.READY)
                }
            }

        robots.forEach {
            val serial = FIRST_SERIAL + it.name[1].code - '0'.code
            sendMessage(it, Message(Type.ROBOT_INIT, serial = serial))
        }
        shouldReceiveLap(robots)

        sendCommandAndCheckState(State.STEADY)
        robots.forEach { shouldReceiveState(it, State.STEADY) }

        sendCommandAndCheckState(State.RUNNING)
        robots.forEach { shouldReceiveState(it, State.RUNNING) }

        sendCommandAndCheckState(State.FINISH)
        robots.forEach { shouldReceiveState(it, State.FINISH) }


        robots.forEach { it.closeClient() }
    }

    private fun shouldReceiveLap(robots: List<WebsocketClient>) {
        shouldReceiveType(ui, Type.LAP)
        robots.forEach {
            shouldReceiveType(it, Type.LAP)
        }
    }


    companion object {
        private const val MAX_ROBOTS = 8
    }
}