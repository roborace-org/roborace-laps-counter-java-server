package org.roborace.lapscounter

import org.awaitility.Awaitility
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.lessThan
import org.junit.jupiter.api.Test
import org.roborace.lapscounter.domain.State
import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.api.Message
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import java.lang.Thread.sleep

@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = ["laps.pit-stop-time=1230", "laps.time-send-interval:3000"])
internal class LapsCounterUiIntegrationTest : LapsCounterAbstractTest() {

    @Test
    fun testHappyPathSimple() {
        sendCommandAndCheckState(State.STEADY)

        sendCommandAndCheckState(State.RUNNING)

        sendCommandAndCheckState(State.FINISH)
    }

    @Test
    fun testRestart() {
        sendCommandAndCheckState(State.STEADY)

        sendCommandAndCheckState(State.RUNNING)

        sendCommandAndCheckState(State.FINISH)

        sendCommandAndCheckState(State.READY)
    }

    @Test
    fun testStateUi() {
        sendState()
        shouldReceiveState(ui, State.READY)

        sendCommand(State.STEADY)
        shouldReceiveState(ui, State.STEADY)

        sendState()
        shouldReceiveState(ui, State.STEADY)
    }

    @Test
    fun testSendTime() {
        sendCommandAndCheckState(State.STEADY)

        sendCommandAndCheckState(State.RUNNING)

        shouldReceiveType(ui, Type.TIME) {
            assertThat(it.time, lessThan(100L))
            assertThat(it.raceTimeLimit, `is`(0L))
        }

        sleep(TIME_SEND_INTERVAL - 1000)
        shouldReceiveType(ui, Type.TIME) {
            assertThat(it.time, equalTo(TIME_SEND_INTERVAL))
        }

        sleep(TIME_SEND_INTERVAL - 1000)
        shouldReceiveType(ui, Type.TIME) {
            assertThat(it.time, equalTo(2 * TIME_SEND_INTERVAL))
        }

        sendCommandAndCheckState(State.FINISH)
        shouldReceiveType(ui, Type.TIME) {
            assertThat(it.time, greaterThanOrEqualTo(2 * TIME_SEND_INTERVAL))
            assertThat(it.time, lessThan(2 * TIME_SEND_INTERVAL + 20))
        }
    }

    @Test
    fun testSendRaceTimeLimit() {
        shouldReceiveType(ui, Type.TIME) {
            assertThat(it.time, `is`(0L))
            assertThat(it.raceTimeLimit, `is`(0L))
        }

        sendTimeRequestCommand(3600L)

        shouldReceiveType(ui, Type.TIME) {
            assertThat(it.time, `is`(0L))
            assertThat(it.raceTimeLimit, `is`(3600L))
        }
    }

    @Test
    fun testAutoFinishRaceByTimeLimit() {
        val raceTimeLimit = 3L
        sendTimeRequestCommand(raceTimeLimit)
        givenRunningState()

        shouldReceiveState(ui, State.FINISH)

        shouldReceiveType(ui, Type.TIME) {
            assertThat(it.time, greaterThanOrEqualTo(raceTimeLimit * 1000L))
            assertThat(it.time, `is`(raceTimeLimit * 1000L))
            assertThat(it.raceTimeLimit, `is`(raceTimeLimit))
        }
    }

    @Test
    fun testReceivePitStopFinish() {
        val robot1 = createAndInitRobot("ROBOT1", FIRST_SERIAL)
        givenRunningState()

        sendMessage(robot1, Message(Type.PIT_STOP, serial = FIRST_SERIAL))

        shouldReceiveType(ui, Type.PIT_STOP_FINISH)
    }

    @Test
    fun testWrongCommand() {
        sendMessage(ui, Message(Type.COMMAND))
        shouldReceiveType(ui, Type.ERROR)
    }

    @Test
    fun testWrongOrder() {
        sendCommand(State.RUNNING)
        shouldReceiveType(ui, Type.ERROR)
        sendCommand(State.FINISH)
        shouldReceiveType(ui, Type.ERROR)

        sendCommandAndCheckState(State.STEADY)

        sendCommand(State.FINISH)
        shouldReceiveType(ui, Type.ERROR)

        sendCommandAndCheckState(State.RUNNING)
    }

    @Test
    fun testLaps() {
        val robot1 = createAndInitRobot("ROBOT1", FIRST_SERIAL)
        assertThat(ui.lastMessage.serial, equalTo(FIRST_SERIAL))
        assertThat(robot1.lastMessage.serial, equalTo(FIRST_SERIAL))
        Awaitility.await().until { robot1.hasNoMessages() }


        val robot2 = createAndInitRobot("ROBOT2", SECOND_SERIAL)
        assertThat(ui.lastMessage.serial, equalTo(SECOND_SERIAL))
        assertThat(robot2.lastMessage.serial, equalTo(SECOND_SERIAL))

        //        shouldHasNoMessage(robot1); // TODO should not receive this
        val serials = mutableSetOf(FIRST_SERIAL, SECOND_SERIAL)
        sendMessage(ui, Message(Type.LAPS))
        Awaitility.await().until {
            shouldReceiveType(ui, Type.LAP) {
                assertThat(serials, Matchers.hasItem(it.serial))
                serials.remove(it.serial)
            }
            serials.isEmpty()
        }
    }
}