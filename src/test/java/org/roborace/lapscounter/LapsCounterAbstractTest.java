package org.roborace.lapscounter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.roborace.lapscounter.client.WebsocketClient;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.State;
import org.roborace.lapscounter.domain.Type;
import org.springframework.beans.factory.annotation.Value;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.ONE_MILLISECOND;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.roborace.lapscounter.domain.Message.builder;
import static org.roborace.lapscounter.domain.State.READY;

abstract class LapsCounterAbstractTest {

    protected static final int FIRST_SERIAL = 100;
    protected static final int SECOND_SERIAL = 101;

    @Value("${local.server.port}")
    private int port = 8888;

    private static final String WS_SERVER = "ws://localhost:%d/ws";
    protected static final long TIME_SEND_INTERVAL = 10_000L;

    protected final ObjectMapper objectMapper = new ObjectMapper();


    protected WebsocketClient ui;


    @BeforeAll
    static void beforeAllAbstract() {
        Awaitility.setDefaultTimeout(FIVE_SECONDS);
        Awaitility.setDefaultPollInterval(ONE_MILLISECOND);
    }

    @BeforeEach
    void setUpAbstract() {

        ui = createClient("UI");
        givenReadyState();

        System.out.println("Given state READY");

    }

    @AfterEach
    void tearDownAbstract() {
        ui.closeClient();
    }

    protected WebsocketClient createClient(String name) {
        try {
            String scheme = String.format(WS_SERVER, port);
            return new WebsocketClient(new URI(scheme), name);
        } catch (IOException | DeploymentException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected WebsocketClient createAndInitRobot(String name, int serial) {
        WebsocketClient robot = createClient(name);
        shouldReceiveState(robot, READY);
        sendMessage(robot, builder().type(Type.ROBOT_INIT).serial(serial).build());
        await().until(() -> ui.hasMessageWithType(Type.LAP));
        await().until(() -> robot.hasMessageWithType(Type.LAP));
        return robot;
    }

    private void givenReadyState() {
        shouldReceiveType(ui, Type.STATE);
        if (ui.getLastMessage().getState() != READY) {
            sendCommandAndCheckState(READY);
        }
    }
    protected void givenRunningState() {
        sendCommandAndCheckState(State.STEADY);
        sendCommandAndCheckState(State.RUNNING);
    }


    protected void sendCommandAndCheckState(State state) {
        sendCommand(state);
        shouldReceiveState(ui, state);
    }

    protected void sendState() {
        sendMessage(ui, buildWithType(Type.STATE));
    }

    protected void sendCommand(State state) {
        sendMessage(ui, Message.builder().type(Type.COMMAND).state(state).build());
    }

    protected void sendTimeRequestCommand(long raceTimeLimit) {
        sendMessage(ui, Message.builder().type(Type.TIME).raceTimeLimit(raceTimeLimit).build());
    }

    protected void sendMessage(WebsocketClient client, Message message) {
        try {
            client.sendMessage(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected static Message buildWithType(Type type) {
        return Message.builder().type(type).build();
    }


    protected void shouldReceiveState(WebsocketClient client, State state) {
        shouldReceiveType(client, Type.STATE);
        assertThat(client.getLastMessage().getState(), equalTo(state));
    }

    protected Message shouldReceiveType(WebsocketClient client, Type type) {
        await().until(() -> client.hasMessageWithType(type));
        return client.getLastMessage();
    }

}