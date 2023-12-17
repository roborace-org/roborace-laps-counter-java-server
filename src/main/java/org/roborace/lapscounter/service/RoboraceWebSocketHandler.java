package org.roborace.lapscounter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.MessageResult;
import org.roborace.lapscounter.domain.ResponseType;
import org.roborace.lapscounter.domain.Type;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@Slf4j
@Component
public class RoboraceWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final LapsCounterService lapsCounterService;

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    public RoboraceWebSocketHandler(LapsCounterService lapsCounterService) {
        this.lapsCounterService = lapsCounterService;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        try {
            String payload = textMessage.getPayload();
            log.info("handleTextMessage {} {}", session.getRemoteAddress(), payload);
            Message message = JSON.readValue(payload, Message.class);

            MessageResult messageResult = lapsCounterService.handleMessage(message);
            if (messageResult == null) {
                return;
            }

            if (messageResult.getResponseType() == ResponseType.BROADCAST) {
                broadcast(messageResult.getMessages());
            } else if (messageResult.getResponseType() == ResponseType.SINGLE) {
                singleSession(messageResult.getMessages(), session);
            }
        } catch (Exception e) {
            log.error("Exception happen during message handling: {}", e.getMessage(), e);
            if (session.isOpen()) {
                Message message = Message.builder().type(Type.ERROR).message(e.getMessage()).build();
                sendTextMessage(convert(message), session);
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        log.debug("ConnectionEstablished {}", session.getRemoteAddress());
        sessions.add(session);

        singleSession(lapsCounterService.afterConnectionEstablished(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.debug("ConnectionClosed {}", session.getRemoteAddress());
        sessions.remove(session);
    }

    public List<WebSocketSession> getSessions() {
        return sessions;
    }

    private void singleSession(List<Message> messages, WebSocketSession session) {
        for (Message message : messages) {
            sendTextMessage(convert(message), session);
        }
    }

    public void broadcast(List<Message> messages) {
        for (Message message : messages) {
            broadcast(message);
        }
    }

    public void broadcast(Message message) {
        TextMessage textMessage = convert(message);
        sessions.stream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> sendTextMessage(textMessage, session));
    }

    private TextMessage convert(Object message) {
        try {
            return new TextMessage(JSON.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.error("Error creating json message for object: {}. Reason: {}", message, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private synchronized void sendTextMessage(TextMessage textMessage, WebSocketSession session) {
        try {
            session.sendMessage(textMessage);
        } catch (IOException e) {
            log.error("Error while sending messages to ws client. Reason: {}", e.getMessage(), e);
        }
    }
}
