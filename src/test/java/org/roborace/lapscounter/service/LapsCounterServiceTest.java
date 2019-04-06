package org.roborace.lapscounter.service;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.roborace.lapscounter.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LapsCounterServiceTest {

    private static final int FRAME = 0xAA00;

    @Mock
    private FrameProcessor frameProcessor;

    @InjectMocks
    private LapsCounterService lapsCounterService;

    @Captor
    private ArgumentCaptor<Robot> robotArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> raceTimeArgumentCaptor;

    @BeforeEach
    void setUp() {

    }

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(frameProcessor);
    }

    @Test
    void testInitialState() {
        Message state = Message.builder().type(Type.STATE).build();

        MessageResult messageResult = lapsCounterService.handleMessage(state);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.SINGLE));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));

        assertThatMessageHasState(messages.get(0), State.READY);
    }

    @Test
    void testCommandSteady() {
        Message command = aCommand(State.STEADY);
        MessageResult messageResult = lapsCounterService.handleMessage(command);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));

        assertThatMessageHasState(messages.get(0), State.STEADY);
        Mockito.verify(frameProcessor).reset();
    }

    @Test
    void testSameCommandException() {
        Assertions.assertThrows(LapsCounterException.class, () -> {
            Message command = aCommand(State.READY);
            lapsCounterService.handleMessage(command);
        });
    }

    @Test
    void testCommandRunning() {
        givenRaceState(State.STEADY);

        Message commandGo = aCommand(State.RUNNING);
        MessageResult messageResult = lapsCounterService.handleMessage(commandGo);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(2));

        assertThatMessageHasState(messages.get(0), State.RUNNING);
        assertThatMessageHasTime(messages.get(1));

        Mockito.verify(frameProcessor).reset();
    }

    @Test
    void testCommandFinish() {
        givenRaceState(State.RUNNING);

        Message commandFinish = aCommand(State.FINISH);
        MessageResult messageResult = lapsCounterService.handleMessage(commandFinish);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(2));

        assertThatMessageHasState(messages.get(0), State.FINISH);
        assertThatMessageHasTime(messages.get(1));

        Mockito.verify(frameProcessor).reset();
    }

    @Test
    void testRobotInit() {
        Message robotInit = Message.builder().type(Type.ROBOT_INIT).serial(101).build();
        MessageResult messageResult = lapsCounterService.handleMessage(robotInit);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));
        assertThatMessageHasLap(messages.get(0), 101);

        Mockito.verify(frameProcessor).robotInit(robotArgumentCaptor.capture());
        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot, equalTo(Robot.builder().serial(101).num(1).place(1).laps(0).build()));
    }

    @Test
    void testSecondRobotInit() {
        givenRobotInits(101);

        Message secondRobotInit = Message.builder().type(Type.ROBOT_INIT).serial(102).build();
        MessageResult messageResult = lapsCounterService.handleMessage(secondRobotInit);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));
        assertThatMessageHasLap(messages.get(0), 102);

        Mockito.verify(frameProcessor, times(2)).robotInit(robotArgumentCaptor.capture());
        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot, equalTo(Robot.builder().serial(102).num(2).place(2).laps(0).build()));
    }

    @Test
    void testRobotEdit() {
        givenRobotInits(101);

        Message robotEdit = Message.builder().type(Type.ROBOT_EDIT).serial(101).name("Winner").build();
        MessageResult messageResult = lapsCounterService.handleMessage(robotEdit);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));
        assertThatMessageHasLap(messages.get(0), 101);

        Mockito.verify(frameProcessor).robotInit(robotArgumentCaptor.capture());
        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot, equalTo(Robot.builder().serial(101).name("Winner").num(1).place(1).laps(0).build()));
    }

    @Test
    void testTime() {
        Message time = Message.builder().type(Type.TIME).build();
        MessageResult messageResult = lapsCounterService.handleMessage(time);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.SINGLE));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));
        assertThatMessageHasTime(messages.get(0));
    }

    @Test
    void testLaps() {
        givenRobotInits(101, 102);

        Message laps = Message.builder().type(Type.LAPS).build();
        MessageResult messageResult = lapsCounterService.handleMessage(laps);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.SINGLE));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(2));
        assertThatMessageHasLap(messages.get(0), 101);
        assertThatMessageHasLap(messages.get(1), 102);

        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));
    }

    @Test
    void testLapManSimple() {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);

        Message laps = Message.builder().type(Type.LAP_MAN).serial(101).laps(1).build();
        MessageResult messageResult = lapsCounterService.handleMessage(laps);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));
        assertThatMessageHasLapWithLapsCount(messages.get(0), 101, 1, 1);

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));
    }

    @Test
    void testLapMan() {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);

        Message laps = Message.builder().type(Type.LAP_MAN).serial(102).laps(1).build();
        MessageResult messageResult = lapsCounterService.handleMessage(laps);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(2));
        assertThatMessageHasLapWithLapsCount(messages.get(0), 102, 1, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(1), 101, 0, 2);

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));
    }

    @Test
    void testLapManDec() throws InterruptedException {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);

        Thread.sleep(100);
        Message lapsInc = Message.builder().type(Type.LAP_MAN).serial(102).laps(1).build();
        lapsCounterService.handleMessage(lapsInc);
        Thread.sleep(50);
        Message lapsDec = Message.builder().type(Type.LAP_MAN).serial(102).laps(-1).build();
        MessageResult messageResult = lapsCounterService.handleMessage(lapsDec);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(2));
        assertThatMessageHasLapWithLapsCount(messages.get(0), 101, 0, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(1), 102, 0, 2);
        assertThat(messages.get(1).getTime(), equalTo(0L));

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));
    }

    @Test
    void testLapManDecCheckTime() throws InterruptedException {
        givenRobotInits(102);
        givenRaceState(State.RUNNING);

        Thread.sleep(100);
        Message lapsInc = Message.builder().type(Type.LAP_MAN).serial(102).laps(1).build();
        MessageResult messageResult = lapsCounterService.handleMessage(lapsInc);
        Long firstLapTime = messageResult.getMessages().get(0).getTime();
        Thread.sleep(50);
        lapsCounterService.handleMessage(lapsInc);
        Thread.sleep(50);
        Message lapsDec = Message.builder().type(Type.LAP_MAN).serial(102).laps(-1).build();
        messageResult = lapsCounterService.handleMessage(lapsDec);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));
        assertThatMessageHasLapWithLapsCount(messages.get(0), 102, 1, 1);
        assertThat(messages.get(0).getTime(), equalTo(firstLapTime));

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor).robotInit(any(Robot.class));
    }

    @Test
    void testFrame() {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);
        when(frameProcessor.checkFrame(argThat(robot -> robot.getSerial() == 101), eq(FRAME), anyLong())).thenReturn(Type.FRAME);

        MessageResult messageResult = lapsCounterService.handleMessage(aFrameMessage(101));
        assertThat(messageResult.getResponseType(), equalTo(ResponseType.SINGLE));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));
        assertThatMessageHasFrame(messages.get(0));

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor).checkFrame(robotArgumentCaptor.capture(), eq(FRAME), anyLong());
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));

        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot, equalTo(Robot.builder().serial(101).num(1).place(1).laps(0).build()));
    }

    @Test
    void testFrameLap() {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);
        when(frameProcessor.checkFrame(argThat(robot -> robot.getSerial() == 101), eq(FRAME), anyLong())).thenReturn(Type.LAP);

        MessageResult messageResult = lapsCounterService.handleMessage(aFrameMessage(101));
        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));
        assertThatMessageHasLapWithLapsCount(messages.get(0), 101, 1, 1);


        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor).checkFrame(robotArgumentCaptor.capture(), eq(FRAME), raceTimeArgumentCaptor.capture());
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));

        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot, equalTo(Robot.builder().serial(101).num(1).place(1).laps(1).time(raceTimeArgumentCaptor.getValue()).build()));
    }

    @Test
    void testLapChangePlace() {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);
        when(frameProcessor.checkFrame(any(), eq(FRAME), anyLong())).thenReturn(Type.LAP);

        MessageResult messageResult = lapsCounterService.handleMessage(aFrameMessage(101));
        assertThatMessageHasLapWithLapsCount(messageResult.getMessages().get(0), 101, 1, 1);
        messageResult = lapsCounterService.handleMessage(aFrameMessage(102));
        assertThatMessageHasLapWithLapsCount(messageResult.getMessages().get(0), 102, 1, 2);


        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor, times(2)).checkFrame(robotArgumentCaptor.capture(), eq(FRAME), raceTimeArgumentCaptor.capture());
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));

        Robot actualRobot = robotArgumentCaptor.getValue();
        Robot expected = Robot.builder().serial(102).num(2).place(2).laps(1).time(raceTimeArgumentCaptor.getValue()).build();
        assertThat(actualRobot, equalTo(expected));
    }

    @Test
    void testSortRobotsByLapsAndTime() {
        Robot first = aRobot(101, 1, 2, 2);
        Robot second = aRobot(102, 2, 2, 5);
        Robot third = aRobot(103, 3, 3, 10);
        List<Robot> robots = asList(first, second, third);
        ReflectionTestUtils.setField(lapsCounterService, "robots", robots);
        List<Robot> affectedRobots = lapsCounterService.sortRobotsByLapsAndTime();

        assertThat(affectedRobots, Matchers.hasSize(3));
        assertThat(affectedRobots.get(0), equalTo(third));
        assertThat(affectedRobots.get(0).getPlace(), equalTo(1));
        assertThat(affectedRobots.get(1), equalTo(first));
        assertThat(affectedRobots.get(1).getPlace(), equalTo(2));
        assertThat(affectedRobots.get(2), equalTo(second));
        assertThat(affectedRobots.get(2).getPlace(), equalTo(3));
    }

    @Test
    void testSortRobotsByLapsAndTimeAndNum() {
        Robot first = aRobot(101, 1, 0, 0);
        first.setNum(2);
        Robot second = aRobot(102, 2, 0, 0);
        second.setNum(1);
        List<Robot> robots = asList(first, second);
        ReflectionTestUtils.setField(lapsCounterService, "robots", robots);
        List<Robot> affectedRobots = lapsCounterService.sortRobotsByLapsAndTime();

        assertThat(affectedRobots, Matchers.hasSize(2));
        assertThat(affectedRobots.get(0), equalTo(second));
        assertThat(affectedRobots.get(0).getPlace(), equalTo(1));
        assertThat(affectedRobots.get(1), equalTo(first));
        assertThat(affectedRobots.get(1).getPlace(), equalTo(2));
    }

    @Test
    void testScheduleIfNotStarted() {
        Message scheduled = lapsCounterService.scheduled();
        assertThat(scheduled, nullValue());
    }

    @Test
    void testScheduleIfRunning() {
        givenRaceState(State.RUNNING);

        Message scheduled = lapsCounterService.scheduled();
        assertThat(scheduled.getType(), equalTo(Type.TIME));
        assertThat(scheduled.getTime(), Matchers.greaterThanOrEqualTo(0L));
        assertThat(scheduled.getTime(), Matchers.lessThan(50L));

        Mockito.verify(frameProcessor).reset();
    }

    private Robot aRobot(int serial, int place, int laps, long time) {
        return Robot.builder().serial(serial).place(place).laps(laps).time(time).build();
    }

    private Message aFrameMessage(int serial) {
        return Message.builder().type(Type.FRAME).serial(serial).frame(FRAME).build();
    }

    private void givenRaceState(State state) {
        switch (state) {
            case FINISH:
                lapsCounterService.handleMessage(aCommand(State.STEADY));
                lapsCounterService.handleMessage(aCommand(State.RUNNING));
                lapsCounterService.handleMessage(aCommand(State.FINISH));
                break;
            case RUNNING:
                lapsCounterService.handleMessage(aCommand(State.STEADY));
                lapsCounterService.handleMessage(aCommand(State.RUNNING));
                break;
            case STEADY:
                lapsCounterService.handleMessage(aCommand(State.STEADY));
                break;
            case READY:
        }
    }

    private void givenRobotInits(Integer... serials) {
        for (Integer serial : serials) {
            lapsCounterService.handleMessage(Message.builder().type(Type.ROBOT_INIT).serial(serial).build());
        }
    }

    private Message aCommand(State state) {
        return Message.builder().type(Type.COMMAND).state(state).build();
    }

    private void assertThatMessageHasState(Message message, State state) {
        assertThat(message.getType(), equalTo(Type.STATE));
        assertThat(message.getState(), equalTo(state));
    }

    private void assertThatMessageHasTime(Message message) {
        assertThat(message.getType(), equalTo(Type.TIME));
        assertThat(message.getTime(), equalTo(0L));
    }

    private void assertThatMessageHasFrame(Message message) {
        assertThat(message.getType(), equalTo(Type.FRAME));
    }

    private void assertThatMessageHasLap(Message message, int serial) {
        assertThat(message.getType(), equalTo(Type.LAP));
        assertThat(message.getSerial(), equalTo(serial));
        assertThat(message.getTime(), equalTo(0L));
    }

    private void assertThatMessageHasLapWithLapsCount(Message message, int serial, int laps, int place) {
        assertThat(message.getType(), equalTo(Type.LAP));
        assertThat(message.getSerial(), equalTo(serial));
        assertThat(message.getLaps(), equalTo(laps));
        assertThat(message.getPlace(), equalTo(place));
    }
}