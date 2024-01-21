package org.roborace.lapscounter.service

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.lessThan
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.roborace.lapscounter.domain.LapsCounterException
import org.roborace.lapscounter.domain.Robot
import org.roborace.lapscounter.domain.State
import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.api.Message
import org.roborace.lapscounter.domain.api.MessageResult
import org.roborace.lapscounter.domain.api.ResponseType
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockKExtension::class)
internal class LapsCounterServiceTest {
    @MockK
    private lateinit var frameProcessor: FrameProcessor

    @MockK
    private lateinit var lapsCounterScheduler: LapsCounterScheduler

    @InjectMockKs
    private lateinit var lapsCounterService: LapsCounterService

    private var messageArgumentCaptor = slot<Message>()
//    private var raceTimeArgumentCaptor = slot<Long>()


    private var messageResult: MessageResult? = null
    private var messages: List<Message> = emptyList()


    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(lapsCounterService, "pitStopTime", PIT_STOP_TEST_TIME)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(frameProcessor, lapsCounterScheduler)
    }

    @Test
    fun testInitialState() {
        val state = Message(Type.STATE)

        whenHandleMessage(state)

        assertThatMessageResultHasTypeAndMessages(ResponseType.SINGLE, 1)
        assertThatMessageHasState(messages[0], State.READY)
    }

    @Test
    fun testCommandSteady() {
        val command = aCommand(State.STEADY)
        whenHandleMessage(command)

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2)
        assertThatMessageHasState(messages[0], State.STEADY)
    }

    @Test
    fun testSameCommandException() {
        assertThrows(LapsCounterException::class.java) {
            val command = aCommand(State.READY)
            lapsCounterService.handleMessage(command)
        }
    }

    @Test
    fun testCommandRunning() {
        givenRaceState(State.STEADY)
        justRun { lapsCounterScheduler.addSchedulerForFinishRace(5000) }

        lapsCounterService.handleMessage(Message(Type.TIME, raceTimeLimit = 5L))

        whenHandleMessage(aCommand(State.RUNNING))

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2)
        assertThatMessageHasState(messages[0], State.RUNNING)
        assertThatMessageHasTime(messages[1])

        verify { lapsCounterScheduler.addSchedulerForFinishRace(5000L) }
    }

    @Test
    fun testCommandFinish() {
        givenRaceState(State.RUNNING)

        justRun { lapsCounterScheduler.resetSchedulers() }
        whenHandleMessage(aCommand(State.FINISH))

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2)
        assertThatMessageHasState(messages[0], State.FINISH)
        assertThatMessageHasTime(messages[1])

        verify { lapsCounterScheduler.resetSchedulers() }
    }

    @Test
    fun testRobotInit() {
        justRun { frameProcessor.robotInit(101) }

        whenHandleMessage(Message(Type.ROBOT_INIT, serial = 101))

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1)
        assertThatMessageHasLap(messages[0], 101)

        verify { frameProcessor.robotInit(101) }
    }

    @Test
    fun testSecondRobotInit() {
        givenRobotInits(101)

        justRun { frameProcessor.robotInit(102) }
        whenHandleMessage(Message(Type.ROBOT_INIT, serial = 102))

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1)
        assertThatMessageHasLap(messages[0], 102)

        verify { frameProcessor.robotInit(101) }
        verify { frameProcessor.robotInit(102) }
    }

    @Test
    fun testRobotEdit() {
        givenRobotInits(101)

        whenHandleMessage(Message(Type.ROBOT_EDIT, serial = 101, name = "Winner"))

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1)
        assertThatMessageHasLap(messages[0], 101)
        assertThat(messages[0].name, equalTo("Winner"))

        verify { frameProcessor.robotInit(101) }
    }

    @Test
    fun testRobotRemoveSingle() {
        givenRobotInits(101)

        every { frameProcessor.robotRemove(101) } returns null
        whenHandleMessage(Message(Type.ROBOT_REMOVE, serial = 101))

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1)
        assertThat(messages[0].type, equalTo(Type.ROBOT_REMOVE))
        assertThat(messages[0].serial, equalTo(101))

        val laps = Message(Type.LAPS)
        whenHandleMessage(laps)
        assertThatMessageResultHasTypeAndMessages(ResponseType.SINGLE, 0)

        verify { frameProcessor.robotInit(101) }
        verify { frameProcessor.robotRemove(101) }
    }

    @Test
    fun testTime() {
        whenHandleMessage(Message(Type.TIME))

        assertThatMessageResultHasTypeAndMessages(ResponseType.SINGLE, 1)
        assertThatMessageHasTime(messages[0])
    }

    @Test
    fun testLaps() {
        givenRobotInits(101, 102)

        val laps = Message(Type.LAPS)
        whenHandleMessage(laps)

        assertThatMessageResultHasTypeAndMessages(ResponseType.SINGLE, 2)
        assertThatMessageHasLap(messages[0], 101)
        assertThatMessageHasLap(messages[1], 102)

        verify(exactly = 1) { frameProcessor.robotInit(101) }
        verify { frameProcessor.robotInit(102) }
    }

    @Test
    fun testLapManSimple() {
        givenRobotInits(101, 102)
        givenRaceState(State.RUNNING)

        val laps = Message(Type.LAP_MAN, serial = 101, laps = 1)
        whenHandleMessage(laps)

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1)
        assertThatMessageHasLapWithLapsCount(messages[0], 101, 1, 1)

        verify { frameProcessor.robotInit(101) }
        verify { frameProcessor.robotInit(102) }
    }

    @Test
    fun testLapMan() {
        givenRobotInits(101, 102)
        givenRaceState(State.RUNNING)

        whenHandleMessage(Message(Type.LAP_MAN, serial = 102, laps = 1))

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2)
        assertThatMessageHasLapWithLapsCount(messages[0], 102, 1, 1)
        assertThatMessageHasLapWithLapsCount(messages[1], 101, 0, 2)

        verify { frameProcessor.robotInit(101) }
        verify { frameProcessor.robotInit(102) }
    }

    @Test
    fun testLapManDec() {
        givenRobotInits(101, 102)
        givenRaceState(State.RUNNING)

        Thread.sleep(100)
        whenHandleMessage(Message(Type.LAP_MAN, serial = 102, laps = 1))
        assertThatMessageHasLapWithLapsCount(messages[0], 102, 1, 1)
        assertThatMessageHasLapWithLapsCount(messages[1], 101, 0, 2)

        Thread.sleep(50)
        whenHandleMessage(Message(Type.LAP_MAN, serial = 102, laps = -1))

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2)
        assertThatMessageHasLapWithLapsCount(messages[0], 101, 0, 1)
        assertThatMessageHasLapWithLapsCount(messages[1], 102, 0, 2)
        assertThat(messages[1].time, equalTo(0L))

        Thread.sleep(50)
        whenHandleMessage(Message(Type.LAP_MAN, serial = 102, laps = -1))

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1)
        assertThatMessageHasLapWithLapsCount(messages[0], 102, -1, 2)
        assertThat(messages[0].time, equalTo(0L))

        verify { frameProcessor.robotInit(101) }
        verify { frameProcessor.robotInit(102) }
    }

    @Test
    fun testLapManDecCheckTime() {
        givenRobotInits(102)
        givenRaceState(State.RUNNING)

        Thread.sleep(100)
        val lapsInc = Message(Type.LAP_MAN, serial = 102, laps = 1)
        whenHandleMessage(lapsInc)
        val firstLapTime = messageResult!!.messages[0].time
        Thread.sleep(50)
        whenHandleMessage(lapsInc)
        Thread.sleep(50)
        val lapsDec = Message(Type.LAP_MAN, serial = 102, laps = -1)
        whenHandleMessage(lapsDec)

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1)
        assertThatMessageHasLapWithLapsCount(messages[0], 102, 1, 1)
        assertThat(messages[0].time, equalTo(firstLapTime))

        verify { frameProcessor.robotInit(102) }
    }

    @Test
    fun testLapManDecNegativeLapsHasZeroTime() {
        givenRobotInits(102)
        givenRaceState(State.RUNNING)

        Thread.sleep(100)
        val lapsInc = Message(Type.LAP_MAN, serial = 102, laps = 1)
        val lapsDec = Message(Type.LAP_MAN, serial = 102, laps = -1)

        whenHandleMessage(lapsDec)
        whenHandleMessage(lapsDec)
        whenHandleMessage(lapsInc)

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1)
        assertThatMessageHasLapWithLapsCount(messages[0], 102, -1, 1)
        assertThat(messages[0].time, equalTo(0L))

        verify { frameProcessor.robotInit(102) }
    }

    @Test
    fun testFrame() {
        givenRobotInits(101, 102)
        givenRaceState(State.RUNNING)

        every { frameProcessor.checkFrame(101, FRAME, any()) } returns Type.FRAME
        every { frameProcessor.isStartFrame(FRAME) } returns true

        whenHandleMessage(aFrameMessage(101))

        assertThatMessageResultHasTypeAndMessages(ResponseType.SINGLE, 1)
        assertThatMessageHasFrame(messages[0])

        verify { frameProcessor.checkFrame(101, FRAME, any()) }
        verify { frameProcessor.isStartFrame(FRAME) }
        verify { frameProcessor.robotInit(101) }
        verify { frameProcessor.robotInit(102) }
    }

    @Test
    fun testFrameLap() {
        givenRobotInits(101, 102)
        givenRaceState(State.RUNNING)
        every { frameProcessor.checkFrame(101, FRAME, any()) } returns Type.LAP

        whenHandleMessage(aFrameMessage(101))

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1)
        assertThatMessageHasLapWithLapsCount(messages[0], 101, 1, 1)

        verify { frameProcessor.robotInit(101) }
        verify { frameProcessor.robotInit(102) }
        verify { frameProcessor.checkFrame(101, FRAME, any()) }
    }

    @Test
    fun testFrameLapChangePlace() {
        givenRobotInits(101, 102)
        givenRaceState(State.RUNNING)
        every { frameProcessor.checkFrame(101, FRAME, any()) } returns Type.LAP
        every { frameProcessor.checkFrame(102, FRAME, any()) } returns Type.LAP

        whenHandleMessage(aFrameMessage(101))

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1)
        assertThatMessageHasLapWithLapsCount(messages[0], 101, 1, 1)

        whenHandleMessage(aFrameMessage(102))
        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1)
        assertThatMessageHasLapWithLapsCount(messages[0], 102, 1, 2)

        whenHandleMessage(aFrameMessage(102))
        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2)
        assertThat(messages, hasSize(2))
        assertThatMessageHasLapWithLapsCount(messages[0], 102, 2, 1)
        assertThatMessageHasLapWithLapsCount(messages[1], 101, 1, 2)


        verify { frameProcessor.robotInit(101) }
        verify { frameProcessor.robotInit(102) }
        verify { frameProcessor.checkFrame(101, FRAME, any()) }
        verify { frameProcessor.checkFrame(102, FRAME, any()) }
    }

    @Test
    fun testPitStopShowTime() {
        givenRobotInits(101)
        givenRaceState(State.RUNNING)

        Thread.sleep(100)
        justRun { lapsCounterScheduler.addSchedulerForPitStop(capture(messageArgumentCaptor), PIT_STOP_TEST_TIME) }
        whenHandleMessage(Message(Type.PIT_STOP, serial = 101))

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2)
        assertThat(messages[0].type, equalTo(Type.PIT_STOP))
        assertThat(messages[0].serial, equalTo(101))
        assertThatMessageHasLap(messages[1], 101)
        assertTimeEquals(messages[1].pitStopFinishTime!!, 100 + PIT_STOP_TEST_TIME)

        verify { lapsCounterScheduler.addSchedulerForPitStop(any(), PIT_STOP_TEST_TIME) }
        verify { frameProcessor.robotInit(101) }

        assertThat(messageArgumentCaptor.captured.type, equalTo(Type.PIT_STOP_FINISH))
        assertThat(messageArgumentCaptor.captured.serial, equalTo(101))
    }

    @Test
    fun testSortRobotsByLapsAndTime() {
        val first = Robot(serial = 101, place = 1, laps = 2, time = 2)
        val second = Robot(serial = 102, place = 2, laps = 2, time = 5)
        val third = Robot(serial = 103, place = 3, laps = 3, time = 10)
        val robots = listOf(first, second, third)
        ReflectionTestUtils.setField(lapsCounterService, "robots", robots)
        val affectedRobots: List<Robot> = lapsCounterService.sortRobotsByLapsAndTime()

        assertThat(affectedRobots, hasSize(3))
        assertThat(affectedRobots[0], equalTo(third))
        assertThat(affectedRobots[0].place, equalTo(1))
        assertThat(affectedRobots[1], equalTo(first))
        assertThat(affectedRobots[1].place, equalTo(2))
        assertThat(affectedRobots[2], equalTo(second))
        assertThat(affectedRobots[2].place, equalTo(3))
    }

    @Test
    fun testSortRobotsByLapsAndTimeAndNum() {
        val first = Robot(serial = 101, place = 1, laps = 0, time = 0, num = 2)
        val second = Robot(serial = 102, place = 2, laps = 0, time = 0, num = 1)
        val robots = listOf(first, second)
        ReflectionTestUtils.setField(lapsCounterService, "robots", robots)
        val affectedRobots: List<Robot> = lapsCounterService.sortRobotsByLapsAndTime()

        assertThat(affectedRobots, hasSize(2))
        assertThat(affectedRobots[0], equalTo(second))
        assertThat(affectedRobots[0].place, equalTo(1))
        assertThat(affectedRobots[1], equalTo(first))
        assertThat(affectedRobots[1].place, equalTo(2))
    }

    @Test
    fun testScheduleIfNotStarted() {
        val scheduled = lapsCounterService.scheduled()
        assertThat(scheduled, nullValue())
    }

    @Test
    fun testScheduleIfRunning() {
        givenRaceState(State.RUNNING)

        val scheduled = lapsCounterService.scheduled()
        assertThat(scheduled!!.type, equalTo(Type.TIME))
        assertThat(scheduled.time, greaterThanOrEqualTo(0L))
        assertThat(scheduled.time, lessThan(50L))
    }

    private fun aCommand(state: State) = Message(Type.COMMAND, state = state)

    private fun aFrameMessage(serial: Int) =
        Message(Type.FRAME, serial = serial, frame = FRAME)

    private fun givenRaceState(state: State) {
        when (state) {
            State.FINISH -> {
                lapsCounterService.handleMessage(aCommand(State.STEADY))
                lapsCounterService.handleMessage(aCommand(State.RUNNING))
                lapsCounterService.handleMessage(aCommand(State.FINISH))
            }

            State.RUNNING -> {
                lapsCounterService.handleMessage(aCommand(State.STEADY))
                lapsCounterService.handleMessage(aCommand(State.RUNNING))
            }

            State.STEADY -> lapsCounterService.handleMessage(aCommand(State.STEADY))
            State.READY -> {}
        }
    }

    private fun whenHandleMessage(message: Message) {
        messageResult = lapsCounterService.handleMessage(message)
        messages = messageResult!!.messages
    }

    private fun givenRobotInits(vararg serials: Int) {
        serials.forEach {
            justRun { frameProcessor.robotInit(it) }
            lapsCounterService.handleMessage(Message(Type.ROBOT_INIT, serial = it))
        }
    }

    private fun assertThatMessageResultHasTypeAndMessages(responseType: ResponseType, countMessages: Int) {
        assertThat(messageResult!!.responseType, equalTo(responseType))
        assertThat(messageResult!!.messages, hasSize(countMessages))
    }

    private fun assertThatMessageHasState(message: Message, state: State) {
        assertThat(message.type, equalTo(Type.STATE))
        assertThat(message.state, equalTo(state))
    }

    private fun assertThatMessageHasTime(message: Message) {
        assertThat(message.type, equalTo(Type.TIME))
        assertThat(message.time, notNullValue())
    }

    private fun assertThatMessageHasFrame(message: Message) {
        assertThat(message.type, equalTo(Type.FRAME))
    }

    private fun assertThatMessageHasLap(message: Message, serial: Int) {
        assertThat(message.type, equalTo(Type.LAP))
        assertThat(message.serial, equalTo(serial))
        assertThat(message.time, equalTo(0L))
    }

    private fun assertThatMessageHasLapWithLapsCount(message: Message, serial: Int, laps: Int, place: Int) {
        assertThat(message.type, equalTo(Type.LAP))
        assertThat(message.serial, equalTo(serial))
        assertThat(message.laps, equalTo(laps))
        assertThat(message.place, equalTo(place))
    }

    private fun assertTimeEquals(time: Long, expectedTime: Long) {
        assertThat(time, greaterThanOrEqualTo(expectedTime))
        assertThat(time, lessThanOrEqualTo(expectedTime + 100))
    }

    companion object {
        private const val FRAME = 0xAA00
        const val PIT_STOP_TEST_TIME: Long = 1230
    }
}