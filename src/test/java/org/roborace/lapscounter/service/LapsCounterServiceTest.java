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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LapsCounterServiceTest {

    private static final int FRAME = 0xAA00;

    @Mock
    private FrameProcessor frameProcessor;
    @Mock
    private LapsCounterScheduler lapsCounterScheduler;

    @InjectMocks
    private LapsCounterService lapsCounterService;

    @Captor
    private ArgumentCaptor<Robot> robotArgumentCaptor;
    @Captor
    private ArgumentCaptor<Long> raceTimeArgumentCaptor;


    MessageResult messageResult;
    List<Message> messages;


    @BeforeEach
    void setUp() {

    }

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(frameProcessor, lapsCounterScheduler);
    }

    @Test
    void testInitialState() {
        Message state = Message.builder().type(Type.STATE).build();

        whenHandleMessage(state);

        assertThatMessageResultHasTypeAndMessages(ResponseType.SINGLE, 1);
        assertThatMessageHasState(messages.get(0), State.READY);
    }

    @Test
    void testCommandSteady() {
        Message command = aCommand(State.STEADY);
        whenHandleMessage(command);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2);
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

        Message raceTimeLimitMessage = Message.builder().type(Type.TIME).raceTimeLimit(5L).build();
        lapsCounterService.handleMessage(raceTimeLimitMessage);

        Message commandGo = aCommand(State.RUNNING);
        whenHandleMessage(commandGo);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2);
        assertThatMessageHasState(messages.get(0), State.RUNNING);
        assertThatMessageHasTime(messages.get(1));

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(lapsCounterScheduler).addSchedulerForFinishRace(5L);
    }

    @Test
    void testCommandFinish() {
        givenRaceState(State.RUNNING);

        Message commandFinish = aCommand(State.FINISH);
        whenHandleMessage(commandFinish);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2);
        assertThatMessageHasState(messages.get(0), State.FINISH);
        assertThatMessageHasTime(messages.get(1));

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(lapsCounterScheduler).addSchedulerForFinishRace(0L);
        Mockito.verify(lapsCounterScheduler).removeSchedulerForFinishRace();
    }

    @Test
    void testRobotInit() {
        Message robotInit = Message.builder().type(Type.ROBOT_INIT).serial(101).build();
        whenHandleMessage(robotInit);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1);
        assertThatMessageHasLap(messages.get(0), 101);

        Mockito.verify(frameProcessor).robotInit(robotArgumentCaptor.capture());
        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot, equalTo(Robot.builder().serial(101).name("Robot 101").num(1).place(1).laps(0).build()));
    }

    @Test
    void testSecondRobotInit() {
        givenRobotInits(101);

        Message secondRobotInit = Message.builder().type(Type.ROBOT_INIT).serial(102).build();
        whenHandleMessage(secondRobotInit);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1);
        assertThatMessageHasLap(messages.get(0), 102);

        Mockito.verify(frameProcessor, times(2)).robotInit(robotArgumentCaptor.capture());
        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot, equalTo(Robot.builder().serial(102).name("Robot 102").num(2).place(2).laps(0).build()));
    }

    @Test
    void testRobotEdit() {
        givenRobotInits(101);

        Message robotEdit = Message.builder().type(Type.ROBOT_EDIT).serial(101).name("Winner").build();
        whenHandleMessage(robotEdit);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1);
        assertThatMessageHasLap(messages.get(0), 101);

        Mockito.verify(frameProcessor).robotInit(robotArgumentCaptor.capture());
        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot, equalTo(Robot.builder().serial(101).name("Winner").num(1).place(1).laps(0).build()));
    }

    @Test
    void testRobotRemoveSingle() {
        givenRobotInits(101);

        Message robotRemove = Message.builder().type(Type.ROBOT_REMOVE).serial(101).build();
        whenHandleMessage(robotRemove);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1);
        assertThat(messages.get(0).getType(), equalTo(Type.ROBOT_REMOVE));
        assertThat(messages.get(0).getSerial(), equalTo(101));

        Message laps = Message.builder().type(Type.LAPS).build();
        whenHandleMessage(laps);
        assertThatMessageResultHasTypeAndMessages(ResponseType.SINGLE, 0);

        Mockito.verify(frameProcessor).robotInit(robotArgumentCaptor.capture());
        Mockito.verify(frameProcessor).robotRemove(robotArgumentCaptor.capture());
    }

    @Test
    void testTime() {
        Message time = Message.builder().type(Type.TIME).build();
        whenHandleMessage(time);

        assertThatMessageResultHasTypeAndMessages(ResponseType.SINGLE, 1);
        assertThatMessageHasTime(messages.get(0));
    }

    @Test
    void testLaps() {
        givenRobotInits(101, 102);

        Message laps = Message.builder().type(Type.LAPS).build();
        whenHandleMessage(laps);

        assertThatMessageResultHasTypeAndMessages(ResponseType.SINGLE, 2);
        assertThatMessageHasLap(messages.get(0), 101);
        assertThatMessageHasLap(messages.get(1), 102);

        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));
    }

    @Test
    void testLapManSimple() {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);

        Message laps = Message.builder().type(Type.LAP_MAN).serial(101).laps(1).build();
        whenHandleMessage(laps);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(0), 101, 1, 1);

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));
        Mockito.verify(lapsCounterScheduler).addSchedulerForFinishRace(0L);
    }

    @Test
    void testLapMan() {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);

        Message laps = Message.builder().type(Type.LAP_MAN).serial(102).laps(1).build();
        whenHandleMessage(laps);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2);
        assertThatMessageHasLapWithLapsCount(messages.get(0), 102, 1, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(1), 101, 0, 2);

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));
        Mockito.verify(lapsCounterScheduler).addSchedulerForFinishRace(0L);
    }

    @Test
    void testLapManDec() throws InterruptedException {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);

        Thread.sleep(100);
        Message lapsInc = Message.builder().type(Type.LAP_MAN).serial(102).laps(1).build();
        whenHandleMessage(lapsInc);
        assertThatMessageHasLapWithLapsCount(messages.get(0), 102, 1, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(1), 101, 0, 2);

        Thread.sleep(50);
        Message lapsDec = Message.builder().type(Type.LAP_MAN).serial(102).laps(-1).build();
        whenHandleMessage(lapsDec);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2);
        assertThatMessageHasLapWithLapsCount(messages.get(0), 101, 0, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(1), 102, 0, 2);
        assertThat(messages.get(1).getTime(), equalTo(0L));

        Thread.sleep(50);
        whenHandleMessage(lapsDec);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(0), 102, -1, 2);
        assertThat(messages.get(0).getTime(), equalTo(0L));

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));
        Mockito.verify(lapsCounterScheduler).addSchedulerForFinishRace(0L);
    }

    @Test
    void testLapManDecCheckTime() throws InterruptedException {
        givenRobotInits(102);
        givenRaceState(State.RUNNING);

        Thread.sleep(100);
        Message lapsInc = Message.builder().type(Type.LAP_MAN).serial(102).laps(1).build();
        whenHandleMessage(lapsInc);
        Long firstLapTime = messageResult.getMessages().get(0).getTime();
        Thread.sleep(50);
        whenHandleMessage(lapsInc);
        Thread.sleep(50);
        Message lapsDec = Message.builder().type(Type.LAP_MAN).serial(102).laps(-1).build();
        whenHandleMessage(lapsDec);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(0), 102, 1, 1);
        assertThat(messages.get(0).getTime(), equalTo(firstLapTime));

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor).robotInit(any(Robot.class));
        Mockito.verify(lapsCounterScheduler).addSchedulerForFinishRace(0L);
    }

    @Test
    void testLapManDecNegativeLapsHasZeroTime() throws InterruptedException {
        givenRobotInits(102);
        givenRaceState(State.RUNNING);

        Thread.sleep(100);
        Message lapsInc = Message.builder().type(Type.LAP_MAN).serial(102).laps(1).build();
        Message lapsDec = Message.builder().type(Type.LAP_MAN).serial(102).laps(-1).build();

        whenHandleMessage(lapsDec);
        whenHandleMessage(lapsDec);
        whenHandleMessage(lapsInc);

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(0), 102, -1, 1);
        assertThat(messages.get(0).getTime(), equalTo(0L));

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor).robotInit(any(Robot.class));
        Mockito.verify(lapsCounterScheduler).addSchedulerForFinishRace(0L);
    }

    @Test
    void testFrame() {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);
        when(frameProcessor.checkFrame(argThat(robot -> robot.getSerial() == 101), eq(FRAME), anyLong())).thenReturn(Type.FRAME);
        when(frameProcessor.isStartFrame(eq(FRAME))).thenReturn(true);

        whenHandleMessage(aFrameMessage(101));

        assertThatMessageResultHasTypeAndMessages(ResponseType.SINGLE, 1);
        assertThatMessageHasFrame(messages.get(0));

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor).checkFrame(robotArgumentCaptor.capture(), eq(FRAME), anyLong());
        Mockito.verify(frameProcessor).isStartFrame(eq(FRAME));
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));

        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot.getSerial(), equalTo(101));
        assertThat(actualRobot.getName(), equalTo("Robot 101"));
        assertThat(actualRobot.getNum(), equalTo(1));
        assertThat(actualRobot.getPlace(), equalTo(1));
        assertThat(actualRobot.getLaps(), equalTo(0));
        assertThat(actualRobot.getCurrentLapStartTime(), greaterThan(0L));
        Mockito.verify(lapsCounterScheduler).addSchedulerForFinishRace(0L);
    }

    @Test
    void testFrameLap() {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);
        when(frameProcessor.checkFrame(argThat(robot -> robot.getSerial() == 101), eq(FRAME), anyLong())).thenReturn(Type.LAP);

        whenHandleMessage(aFrameMessage(101));

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(0), 101, 1, 1);


        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor).checkFrame(robotArgumentCaptor.capture(), eq(FRAME), raceTimeArgumentCaptor.capture());
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));
        Mockito.verify(lapsCounterScheduler).addSchedulerForFinishRace(0L);

        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot.getSerial(), equalTo(101));
        assertThat(actualRobot.getName(), equalTo("Robot 101"));
        assertThat(actualRobot.getNum(), equalTo(1));
        assertThat(actualRobot.getPlace(), equalTo(1));
        assertThat(actualRobot.getLaps(), equalTo(1));
        Long time = raceTimeArgumentCaptor.getValue();
        assertThat(actualRobot.getTime(), equalTo(time));
        assertThat(actualRobot.getCurrentLapStartTime(), equalTo(time));
        assertThat(actualRobot.getLastLapTime(), equalTo(time));
    }

    @Test
    void testFrameLapChangePlace() {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);
        when(frameProcessor.checkFrame(any(), eq(FRAME), anyLong())).thenReturn(Type.LAP);

        whenHandleMessage(aFrameMessage(101));

        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(0), 101, 1, 1);

        whenHandleMessage(aFrameMessage(102));
        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(0), 102, 1, 2);

        whenHandleMessage(aFrameMessage(102));
        assertThatMessageResultHasTypeAndMessages(ResponseType.BROADCAST, 2);
        assertThat(messages, Matchers.hasSize(2));
        assertThatMessageHasLapWithLapsCount(messages.get(0), 102, 2, 1);
        assertThatMessageHasLapWithLapsCount(messages.get(1), 101, 1, 2);


        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor, times(3)).checkFrame(robotArgumentCaptor.capture(), eq(FRAME), raceTimeArgumentCaptor.capture());
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));
        Mockito.verify(lapsCounterScheduler).addSchedulerForFinishRace(0L);

        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot.getSerial(), equalTo(102));
        assertThat(actualRobot.getName(), equalTo("Robot 102"));
        assertThat(actualRobot.getNum(), equalTo(2));
        assertThat(actualRobot.getPlace(), equalTo(1));
        assertThat(actualRobot.getLaps(), equalTo(2));
        Long time = raceTimeArgumentCaptor.getValue();
        assertThat(actualRobot.getTime(), equalTo(time));
        assertThat(actualRobot.getCurrentLapStartTime(), equalTo(time));
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
        Mockito.verify(lapsCounterScheduler).addSchedulerForFinishRace(0L);
    }

    private Robot aRobot(int serial, int place, int laps, long time) {
        return Robot.builder().serial(serial).place(place).laps(laps).time(time).build();
    }

    private Message aCommand(State state) {
        return Message.builder().type(Type.COMMAND).state(state).build();
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

    private void whenHandleMessage(Message message) {
        messageResult = lapsCounterService.handleMessage(message);
        messages = messageResult.getMessages();
    }

    private void givenRobotInits(int... serials) {
        for (int serial : serials) {
            lapsCounterService.handleMessage(Message.builder().type(Type.ROBOT_INIT).serial(serial).build());
        }
    }

    private void assertThatMessageResultHasTypeAndMessages(ResponseType responseType, int countMessages) {
        assertThat(messageResult.getResponseType(), equalTo(responseType));
        assertThat(messageResult.getMessages(), Matchers.hasSize(countMessages));
    }

    private void assertThatMessageHasState(Message message, State state) {
        assertThat(message.getType(), equalTo(Type.STATE));
        assertThat(message.getState(), equalTo(state));
    }

    private void assertThatMessageHasTime(Message message) {
        assertThat(message.getType(), equalTo(Type.TIME));
        assertThat(message.getTime(), lessThanOrEqualTo(1L));
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