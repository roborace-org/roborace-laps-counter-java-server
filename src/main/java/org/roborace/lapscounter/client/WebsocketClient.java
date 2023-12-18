package org.roborace.lapscounter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.State;
import org.roborace.lapscounter.domain.Type;

import java.io.IOException;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ClientEndpoint
public class WebsocketClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String name;
    private Session userSession = null;

    private final Queue<Message> messages = new ConcurrentLinkedQueue<>();
    private Message lastMessage;
    private State state;

    public WebsocketClient(URI endpointURI, String name) throws IOException, DeploymentException {
        this.name = name;

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        container.connectToServer(this, endpointURI);

    }

    @OnOpen
    public void onOpen(Session userSession) {
        System.out.printf("[%6s] Opening websocket userSession = [%s]\n", name, userSession);
        this.userSession = userSession;
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.printf("[%6s] Closing websocket userSession = [%s], reason = [%s]\n", name, userSession, reason);
        this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message) throws IOException {
        System.out.printf("[%6s] Message received = [%s]\n", name, message);
        messages.add(OBJECT_MAPPER.readValue(message, Message.class));
    }

    public void sendMessage(String message) {
        System.out.printf("[%6s] Send message = [%s]\n", name, message);
        this.userSession.getAsyncRemote().sendText(message);
    }

    public void closeClient() {
        if (userSession != null) {
            try {
                userSession.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Message pollMessage() {
        return messages.poll();
    }

    public boolean hasMessage() {
        return !messages.isEmpty();
    }
    public boolean hasNoMessage() {
        return messages.isEmpty();
    }

    public boolean hasMessageWithType(Type type) {
        if (messages.isEmpty()) {
            return false;
        }
        lastMessage = messages.poll();
        boolean result = lastMessage.getType() == type;
        if (result && type == Type.STATE) {
            state = lastMessage.getState();
        }
        return result;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public String getName() {
        return name;
    }

    public State getState() {
        return state;
    }
}