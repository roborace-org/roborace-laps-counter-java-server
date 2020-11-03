package org.roborace.lapscounter.service;

import lombok.extern.slf4j.Slf4j;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.MessageResult;
import org.roborace.lapscounter.domain.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.roborace.lapscounter.domain.State.FINISH;

@Slf4j
@Service
public class LapsCounterScheduler {

    public static final Message FINISH_MESSAGE = Message.builder().type(Type.COMMAND).state(FINISH).build();

    @Autowired
    private RoboraceWebSocketHandler webSocketHandler;
    @Autowired
    private LapsCounterService lapsCounterService;

    private Timer timer;

    @Scheduled(fixedRate = 10000)
    void showStat() {
        String clients = webSocketHandler.getSessions().stream()
                .map(session -> String.format("%s open:%s", session.getRemoteAddress(), session.isOpen()))
                .collect(Collectors.joining(", "));
        log.info("Connected websocket clients: {}", clients);
    }

    @Scheduled(fixedRate = 10000)
    void scheduled() {
        Message scheduled = lapsCounterService.scheduled();
        if (scheduled != null) {
            webSocketHandler.broadcast(scheduled);
        }
    }


    public void addSchedulerForFinishRace(long raceStateLimit) {
        if (raceStateLimit > 0) {
            timer = new Timer();
            timer.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            MessageResult command = lapsCounterService.handleMessage(FINISH_MESSAGE);
                            webSocketHandler.broadcast(command.getMessages());
                            log.info("Race is finished by time limit");
                        }
                    },
                    TimeUnit.SECONDS.toMillis(raceStateLimit)
            );
        }
    }

    public void removeSchedulerForFinishRace() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }
}
