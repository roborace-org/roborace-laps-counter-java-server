package org.roborace.lapscounter

import org.awaitility.Awaitility
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThan
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.roborace.lapscounter.client.WebsocketClient
import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.api.Message
import org.roborace.lapscounter.service.util.Stopwatch
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import java.lang.Thread.sleep
import kotlin.math.min

@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = ["laps.safe-interval=100"])
internal class LapsCounterFrameIntegrationTest : LapsCounterAbstractTest() {
    @Value("\${laps.safe-interval}")
    private val safeInterval: Long = 0

    private lateinit var robot1: WebsocketClient
    private lateinit var robot2: WebsocketClient


    @BeforeEach
    fun setUp() {
        robot1 = createAndInitRobot("ROBOT1", FIRST_SERIAL)
        robot2 = createAndInitRobot("ROBOT2", SECOND_SERIAL)
    }


    @AfterEach
    fun tearDown() {
        robot1.closeClient()
        robot2.closeClient()
    }


    @Test
    fun testFrameIgnoredIfStateNotRunning() {
        Awaitility.await().until { robot1.hasMessageWithType(Type.LAP) }

        sendStartingFrame()

        sleep(safeInterval)

        Assertions.assertFalse(robot1.hasMessageWithType(Type.LAP))
    }

    @Test
    fun testFrameIgnoredFirstSeconds() {
        givenRunningState()

        Awaitility.await().until { robot1.hasMessageWithType(Type.LAP) }

        sendFrame(robot1, FIRST_SERIAL, FRAME_0)
        sendFrame(robot1, FIRST_SERIAL, FRAME_1)
        sendFrame(robot1, FIRST_SERIAL, FRAME_2)

        sleep(safeInterval)

        Assertions.assertFalse(robot1.hasMessageWithType(Type.LAP))
    }

    @Test
    fun testFrameSimple() {
        givenRunningState()

        sendStartingFrame()
        sendAllFrames()

        Awaitility.await().untilAsserted {
            shouldReceiveType(robot1, Type.LAP) {
                assertThat(it.serial, equalTo(FIRST_SERIAL))
                assertThat(it.laps, equalTo(1))
            }
        }

        shouldReceiveType(ui, Type.LAP)
    }

    @Test
    fun testFrameWrongRotation() {
        givenRunningState()

        sendAllFramesWrongRotation()

        Awaitility.await().untilAsserted {
            shouldReceiveType(robot1, Type.LAP) {
                assertThat(it.serial, equalTo(FIRST_SERIAL))
                assertThat(it.laps, equalTo(-1))
            }
        }

        shouldReceiveType(ui, Type.LAP)
    }

    @Test
    fun testLastLapTime() {
        givenRunningState()

        sleep(2 * safeInterval)

        val stopwatch = Stopwatch().start()
        sendStartingFrame()
        sendAllFrames()
        stopwatch.stop()
        val stopwatch2 = Stopwatch().start()
        Awaitility.await().untilAsserted {
            shouldReceiveType(robot1, Type.LAP) {
                assertThat(it.serial, equalTo(FIRST_SERIAL))
                assertThat(it.laps, equalTo(1))
                assertTimeEquals(it.lastLapTime, stopwatch.time())
            }
        }


        sleep(2 * safeInterval)
        sendAllFrames()
        stopwatch2.stop()
        shouldReceiveType(robot1, Type.LAP) {
            assertThat(it.serial, equalTo(FIRST_SERIAL))
            assertThat(it.laps, equalTo(2))
            assertTimeEquals(it.lastLapTime, stopwatch2.time())
        }
    }

    @Test
    fun testBestLapTime() {
        givenRunningState()

        val stopwatch = Stopwatch().start()
        sendStartingFrame()
        sleep(2 * safeInterval)
        sendAllFrames()
        stopwatch.stop()
        val stopwatch2 = Stopwatch().start()
        Awaitility.await().untilAsserted {
            shouldReceiveType(robot1, Type.LAP) {
                assertThat(it.serial, equalTo(FIRST_SERIAL))
                assertThat(it.laps, equalTo(1))
                assertThat(it.lastLapTime, equalTo(it.bestLapTime))
                assertTimeEquals(it.lastLapTime, stopwatch.time())
                assertTimeEquals(it.bestLapTime, stopwatch.time())
            }
        }


        sendAllFrames()
        stopwatch2.stop()
        val stopwatch3 = Stopwatch().start()
        shouldReceiveType(robot1, Type.LAP) {
            assertThat(it.serial, equalTo(FIRST_SERIAL))
            assertThat(it.laps, equalTo(2))
            assertTimeEquals(it.lastLapTime, stopwatch2.time())
            assertTimeEquals(it.bestLapTime, stopwatch2.time())
            assertThat(it.lastLapTime, equalTo(it.bestLapTime))
        }

        sleep(safeInterval)
        sendAllFrames()
        stopwatch3.stop()
        shouldReceiveType(robot1, Type.LAP) {
            assertThat(it.serial, equalTo(FIRST_SERIAL))
            assertThat(it.laps, equalTo(3))
            assertTimeEquals(it.lastLapTime, stopwatch3.time())
            assertTimeEquals(it.bestLapTime, min(stopwatch2.time(), stopwatch3.time()))
            assertThat(it.bestLapTime, lessThan(it.lastLapTime))
        }
    }

    private fun assertTimeEquals(time: Long, expectedTime: Long) {
        assertThat(time, greaterThanOrEqualTo(expectedTime - 100))
        assertThat(time, lessThanOrEqualTo(expectedTime + 100))
    }

    @Test
    fun testFrameOneRobotSeveralLaps() {
        givenRunningState()

        sendStartingFrame()

        for (laps in 1..3) {
            sendAllFrames()

            Awaitility.await().untilAsserted {
                shouldReceiveType(robot1, Type.LAP) {
                    assertThat(it.serial, equalTo(FIRST_SERIAL))
                    assertThat(it.laps, equalTo(laps))
                }
            }
        }
    }

    private fun sendAllFrames() = sendFrames(FRAME_1, FRAME_2, FRAME_0)

    private fun sendAllFramesWrongRotation() = sendFrames(FRAME_2, FRAME_1, FRAME_0)

    private fun sendFrames(vararg frames: Int) =
        frames.forEach {
            sleep(2 * safeInterval)
            sendFrame(robot1, FIRST_SERIAL, it)
        }

    private fun sendStartingFrame() = sendFrame(robot1, FIRST_SERIAL, FRAME_0)

    private fun sendFrame(robot: WebsocketClient, serial: Int, frame: Int) =
        sendMessage(robot, Message(Type.FRAME, serial = serial, frame = frame))

    companion object {
        private const val FRAME_0 = 0xAA00
        private const val FRAME_1 = 0xAA01
        private const val FRAME_2 = 0xAA02
    }
}