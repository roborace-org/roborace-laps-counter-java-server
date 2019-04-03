package org.roborace.lapscounter.service;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
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

        assertThatMessageHasState(messages.get(0), State.STEADY);
    }

    @Test
    void testCommandReady() {
        Message command = aCommand(State.READY);
        MessageResult messageResult = lapsCounterService.handleMessage(command);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));

        assertThatMessageHasState(messages.get(0), State.READY);
    }

    @Test
    void testCommandSteadySimple() {
        lapsCounterService.handleMessage(aCommand(State.READY));

        Message commandSteady = aCommand(State.STEADY);
        MessageResult messageResult = lapsCounterService.handleMessage(commandSteady);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));

        assertThatMessageHasState(messages.get(0), State.STEADY);

        Mockito.verify(frameProcessor).reset();
    }

    @Test
    void testCommandRunning() {
        lapsCounterService.handleMessage(aCommand(State.READY));
        lapsCounterService.handleMessage(aCommand(State.STEADY));

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
        lapsCounterService.handleMessage(aCommand(State.READY));
        lapsCounterService.handleMessage(aCommand(State.STEADY));
        lapsCounterService.handleMessage(aCommand(State.RUNNING));

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

        Mockito.verify(frameProcessor).robotInit(robotArgumentCaptor.capture());
        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot, equalTo(Robot.builder().serial(101).num(1).place(1).laps(0).build()));
    }

    @Test
    void testSecondRobotInit() {
        lapsCounterService.handleMessage(Message.builder().type(Type.ROBOT_INIT).serial(101).build());

        Message secondRobotInit = Message.builder().type(Type.ROBOT_INIT).serial(102).build();
        MessageResult messageResult = lapsCounterService.handleMessage(secondRobotInit);

        assertThat(messageResult.getResponseType(), equalTo(ResponseType.BROADCAST));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages, Matchers.hasSize(1));

        Mockito.verify(frameProcessor, times(2)).robotInit(robotArgumentCaptor.capture());
        Robot actualRobot = robotArgumentCaptor.getValue();
        assertThat(actualRobot, equalTo(Robot.builder().serial(102).num(2).place(2).laps(0).build()));
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
}