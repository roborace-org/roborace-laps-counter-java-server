package org.roborace.lapscounter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.MessageResult;
import org.roborace.lapscounter.domain.ResponseType;
import org.roborace.lapscounter.domain.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;


@Component
public class RoboraceWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RoboraceWebSocketHandler.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final LapsCounterService lapsCounterService;
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Autowired
    public RoboraceWebSocketHandler(LapsCounterService lapsCounterService) {
        this.lapsCounterService = lapsCounterService;
    }


    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws IOException {
        try {
            String payload = textMessage.getPayload();
            LOG.info("handleTextMessage {} {}", session.getRemoteAddress(), payload);
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
            LOG.error("Exception happen during message handling: {}", e.getMessage(), e);
            if (session.isOpen()) {
                Message message = Message.builder().type(Type.ERROR).message(e.getMessage()).build();
                sendObjectAsJson(message, session);
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        LOG.info("ConnectionEstablished {}", session.getRemoteAddress());
        sessions.add(session);

        singleSession(lapsCounterService.afterConnectionEstablished(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        LOG.info("ConnectionClosed {}", session.getRemoteAddress());
        sessions.remove(session);
    }

    private void singleSession(List<Message> messages, WebSocketSession session) {
        for (Message message : messages) {
            sendObjectAsJson(message, session);
        }
    }

    private void broadcast(List<Message> messages) {
        for (Message message : messages) {
            broadcast(message);
        }
    }

    private void broadcast(Message message) {
        sessions.stream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> sendObjectAsJson(message, session));
    }

    private void sendObjectAsJson(Object message, WebSocketSession session) {
        try {
            session.sendMessage(new TextMessage(JSON.writeValueAsString(message)));
        } catch (IOException e) {
            LOG.error(String.format("Error while sending messages to ws client. Reason: %s", e.getMessage()), e);
        }
    }

    @Scheduled(fixedRate = 10000)
    private void showStat() {
        String clients = sessions.stream()
                .map(session -> String.format("%s open:%s", session.getRemoteAddress(), session.isOpen()))
                .collect(Collectors.joining(", "));
        LOG.info("Connected websocket clients: {}", clients);
    }

    @Scheduled(fixedRate = 10000)
    private void scheduled() {
        Message scheduled = lapsCounterService.scheduled();
        if (scheduled != null) {
            broadcast(scheduled);
        }
    }
}
