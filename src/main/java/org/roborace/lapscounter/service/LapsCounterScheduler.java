package org.roborace.lapscounter.service;

import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.MessageResult;
import org.roborace.lapscounter.domain.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static org.roborace.lapscounter.domain.State.FINISH;

@Service
public class LapsCounterScheduler {

    public static final Logger log = LoggerFactory.getLogger(LapsCounterService.class);
    public static final Message FINISH_MESSAGE = Message.builder().type(Type.COMMAND).state(FINISH).build();

    @Autowired
    private RoboraceWebSocketHandler webSocketHandler;
    @Autowired
    private LapsCounterService lapsCounterService;

    private Timer timer = new Timer();

    @Scheduled(fixedRate = 10000)
    void showStat() {
        String clients = webSocketHandler.getSessions().stream()
                .map(session -> String.format("%s open:%s", session.getRemoteAddress(), session.isOpen()))
                .collect(Collectors.joining(", "));
        log.debug("Connected websocket clients: {}", clients);
    }

    @Scheduled(fixedRate = 10000)
    void scheduled() {
        Message scheduled = lapsCounterService.scheduled();
        if (scheduled != null) {
            webSocketHandler.broadcast(scheduled);
        }
    }


    public void addSchedulerForFinishRace(long delayMs) {
        addScheduler(() -> {
            MessageResult command = lapsCounterService.handleMessage(FINISH_MESSAGE);
            webSocketHandler.broadcast(command.getMessages());
            log.info("Race is finished by time limit");
        }, delayMs);
    }

    public void addSchedulerForPitStop(Message message, long delayMs) {
        addScheduler(() -> {
            webSocketHandler.broadcast(message);
            log.info("Pit stop is finished");
        }, delayMs);
    }

    public void addScheduler(Runnable runnable, long delayMs) {
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                },
                delayMs
        );
    }

    public void resetSchedulers() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = new Timer();
        }
    }
}
