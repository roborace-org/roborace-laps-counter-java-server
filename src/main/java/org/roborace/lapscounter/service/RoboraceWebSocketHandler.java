package org.roborace.lapscounter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.roborace.lapscounter.domain.Message;
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

    @Autowired
    private LapsCounterService lapsCounterService;

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();


    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws IOException {
        try {
            String payload = textMessage.getPayload();
            LOG.info("handleTextMessage {} {}", session.getRemoteAddress(), payload);
            Message message = JSON.readValue(payload, Message.class);
            lapsCounterService.handleMessage(message);
        } catch (Exception e) {
            if (session.isOpen()) {
                Message message = Message.builder().type(Type.ERROR).message(e.getMessage()).build();
                sendMessage(message, session);
            }
        }
    }

    public void broadcast(Message message) {
        sessions.stream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> sendMessage(message, session));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        LOG.info("ConnectionEstablished {}", session.getRemoteAddress());
        sessions.add(session);

        sendMessage(lapsCounterService.getState(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        LOG.info("ConnectionClosed {}", session.getRemoteAddress());
        sessions.remove(session);
    }

    private void sendMessage(Object message, WebSocketSession session) {
        try {
            session.sendMessage(new TextMessage(JSON.writeValueAsString(message)));
        } catch (IOException e) {
            LOG.error(String.format("Error while sending message to ws client. Reason: %s", e.getMessage()), e);
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
        lapsCounterService.scheduled();
    }
}
