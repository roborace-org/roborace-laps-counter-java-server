package org.roborace.lapscounter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.Type;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ClientEndpoint
public class WebsocketClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String name;
    private Session userSession = null;

    private List<Message> messages = new ArrayList<>();
    private Message lastMessage;

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

    public boolean hasMessage() {
        return !messages.isEmpty();
    }

    public boolean hasMessageWithType(Type type) {
        if (!hasMessage()) {
            return false;
        }
        lastMessage = messages.remove(0);
        return lastMessage.getType() == type;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public String getName() {
        return name;
    }
}