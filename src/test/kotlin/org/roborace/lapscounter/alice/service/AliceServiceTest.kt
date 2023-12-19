package org.roborace.lapscounter.alice.service

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.roborace.lapscounter.domain.State
import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.api.Message
import java.time.Duration

class AliceServiceTest {

    private val aliceService: AliceService = AliceService(
        timeout = 2000,
        freshSeconds = freshSeconds,
    )

    @Test
    fun testEmptyListOfMessages() {
        assertThat(aliceService.getUpdates(), `is`(emptyString()))
    }

    @Test
    fun testRemoveNonFreshMessage() {
        // aliceService.addNewMessages(listOf(Message(Type.STATE, state = State.READY)))

        Thread.sleep(Duration.ofSeconds(freshSeconds))

        assertThat(aliceService.getUpdates(), `is`(emptyString()))
    }

    @Test
    fun testRemoveNonFreshMessageOnUpdate() {
        // aliceService.addNewMessages(listOf(Message(Type.COMMAND, state = State.READY)))
        // Thread.sleep(Duration.ofSeconds(freshSeconds))
        //
        // aliceService.addNewMessages(listOf(Message(Type.COMMAND, state = State.STEADY)))

        assertThat(aliceService.getUpdates(), `is`("внимание"))
    }

    @Test
    fun testLapManMessage() {
        val lapManInc = Message(Type.LAP_MAN, serial = 1, laps = 1)
        val lapManDec = Message(Type.LAP_MAN, serial = 2, laps = -1)
        // aliceService.addNewMessages(listOf(lapManInc, lapManDec))

        assertThat(aliceService.getUpdates(), `is`("первый круг\nвторой минус"))
    }

    @Test
    fun testPitStopMessage() {
        val message = Message(Type.PIT_STOP, serial = 3)
        // aliceService.addNewMessages(listOf(message))

        assertThat(aliceService.getUpdates(), `is`("третий пит стоп"))
    }

    @Test
    fun testPitStopFinishMessage() {
        val message = Message(Type.PIT_STOP_FINISH, serial = 4)
        // aliceService.addNewMessages(listOf(message))

        assertThat(aliceService.getUpdates(), `is`("четвертый пит стоп окончен"))
    }

    @Test
    fun testStateReadyMessage() {
        val firstRobot = Message(Type.LAP, serial = 1, name = "first")
        val secondRobot = Message(Type.LAP, serial = 2, name = "second")
        val stateMessage = Message(Type.STATE, state = State.READY)
        // aliceService.addNewMessages(listOf(firstRobot, secondRobot, stateMessage))

        assertThat(
            aliceService.getUpdates(),
            `is`("Порядок роботов в заезде: первая позиция — first. вторая позиция — second.")
        )
    }

    @Test
    fun testStateReadyMessageAfterRemove() {
        val firstRobot = Message(Type.LAP, serial = 1, name = "first")
        val secondRobot = Message(Type.LAP, serial = 2, name = "second")
        val removeRobot = Message(Type.ROBOT_REMOVE, serial = 1)
        val firstRobotNew = Message(Type.LAP, serial = 1, name = "first-new")
        val stateMessage = Message(Type.STATE, state = State.READY)
        // aliceService.addNewMessages(listOf(firstRobot, secondRobot, removeRobot, firstRobotNew, stateMessage))

        assertThat(
            aliceService.getUpdates(),
            `is`("Порядок роботов в заезде: первая позиция — first-new.")
        )
    }

    @Test
    fun testStateFinishMessage() {
        val firstRobot = Message(Type.LAP, serial = 1, name = "first", laps = 3, place = 2)
        val secondRobot = Message(Type.LAP, serial = 2, name = "second", laps = 10, place = 1)
        val stateMessage = Message(Type.STATE, state = State.FINISH)
        // aliceService.addNewMessages(listOf(firstRobot, secondRobot, stateMessage))

        assertThat(
            aliceService.getUpdates(),
            `is`("Места в заезде: первое место — second, 10 кругов. второе место — first, 3 круга.")
        )
    }

    companion object {
        private const val freshSeconds = 1L
    }
}