package org.roborace.lapscounter.service;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.roborace.lapscounter.domain.*;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class LapsCounterServiceTest {

    @Mock
    private FrameProcessor frameProcessor;

    @InjectMocks
    private LapsCounterService lapsCounterService;

    private ArgumentCaptor<Robot> robotArgumentCaptor = ArgumentCaptor.forClass(Robot.class);

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
    void testLapMan() {
        givenRobotInits(101, 102);
        givenRaceState(State.RUNNING);

        Message laps = Message.builder().type(Type.LAP_MAN).serial(102).laps(1).build();
        MessageResult messageResult = lapsCounterService.handleMessage(laps);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));
        assertThatMessageHasLapWithLapsCount(messages.get(0), 102, 1);

        Mockito.verify(frameProcessor).reset();
        Mockito.verify(frameProcessor, times(2)).robotInit(any(Robot.class));
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

    private void assertThatMessageHasLap(Message message, int serial) {
        assertThat(message.getType(), equalTo(Type.LAP));
        assertThat(message.getSerial(), equalTo(serial));
        assertThat(message.getTime(), equalTo(0L));
    }

    private void assertThatMessageHasLapWithLapsCount(Message message, int serial, int laps) {
        assertThat(message.getType(), equalTo(Type.LAP));
        assertThat(message.getSerial(), equalTo(serial));
        assertThat(message.getLaps(), equalTo(laps));
    }
}