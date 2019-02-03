package org.roborace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.ONE_MILLISECOND;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.roborace.State.READY;

abstract class LapsCounterAbstractTest {

    private static final String WS_SERVER = "ws://192.168.1.200:8888/";
    protected static final int TIME_SEND_INTERVAL = 10_000;

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
            return new WebsocketClient(new URI(WS_SERVER), name);
        } catch (IOException | DeploymentException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void givenReadyState() {
        shouldReceiveType(ui, Type.STATE);
        if (ui.getLastMessage().getState() != READY) {
            sendCommandAndCheckState(READY);
        }
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

    protected void shouldReceiveType(WebsocketClient client, Type type) {
        await().until(() -> client.hasMessageWithType(type));
    }

}