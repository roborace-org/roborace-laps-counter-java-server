package org.roborace.lapscounter.service;

import org.roborace.lapscounter.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.roborace.lapscounter.domain.State.*;


@Service
public class LapsCounterService {

    private static final Logger LOG = LoggerFactory.getLogger(LapsCounterService.class);

    private State state = READY;
    private final Stopwatch stopwatch = new Stopwatch();
    private final List<Robot> robots = new ArrayList<>();

    private final FrameProcessor frameProcessor;

    @Autowired
    public LapsCounterService(FrameProcessor frameProcessor) {
        this.frameProcessor = frameProcessor;
    }


    public MessageResult handleMessage(Message message) {

        switch (message.getType()) {
            case COMMAND:
                return command(message);
            case STATE:
                return MessageResult.single(getState());
            case ROBOT_INIT:
                return robotInit(message);
            case ROBOT_EDIT:
                return robotEdit(message);
            case TIME:
                return MessageResult.single(getTime());
            case LAPS:
                return new MessageResult(getLapMessages(), ResponseType.SINGLE);
            case LAP:
                throw new LapsCounterException("Method not supported");
            case LAP_MAN:
                return lapManual(message);
            case FRAME:
                return frame(message);
            case ERROR:
                throw new LapsCounterException("Method not supported");
            default:
                return null;
        }
    }

    private MessageResult command(Message message) {
        State parsedState = message.getState();
        if (parsedState == null) {
            throw new LapsCounterException("State is null");
        }
        if (!isCorrectCommand(parsedState)) {
            throw new LapsCounterException("Wrong current state to apply command: [" + state + "]->[" + parsedState + "]");
        }

        MessageResult messageResult = new MessageResult(new ArrayList<>(), ResponseType.BROADCAST);
        if (parsedState != state) {
            state = parsedState;
            messageResult.add(getState());
            switch (state) {
                case STEADY:
                    robots.forEach(Robot::reset);
                    frameProcessor.reset();
                    messageResult.addAll(getLapMessages());
                    break;
                case RUNNING:
                    stopwatch.start();
                    messageResult.add(getTime());
                    break;
                case FINISH:
                    stopwatch.finish();
                    messageResult.add(getTime());
                    break;
            }
        }
        return messageResult;
    }

    private boolean isCorrectCommand(State newState) {
        if (newState.ordinal() == state.ordinal() + 1) return true;
        if (newState.ordinal() == 0 && state.ordinal() == values().length - 1) return true;
        return false;
    }

    private MessageResult robotInit(Message message) {
        Optional<Robot> existing = getRobot(message.getSerial());
        Robot robot;
        if (existing.isPresent()) {
            LOG.info("Reconnect robot {}", existing.get());
            robot = existing.get();
        } else {
            robot = new Robot();
            robot.setSerial(message.getSerial());
            robot.setNum(robots.size() + 1);
            robot.setPlace(robots.size() + 1);
            robot.reset();
            robots.add(robot);
            frameProcessor.robotInit(robot);
            LOG.info("Connect new robot {}", robot);
        }
        LOG.info("Connected robots: {}", robots);
        return MessageResult.broadcast(getLap(robot));
    }

    private MessageResult robotEdit(Message message) {
        Robot robot = getRobotOrElseThrow(message.getSerial());
        LOG.info("Edit robot {}", robot);
        robot.setName(message.getName());
        return MessageResult.broadcast(getLap(robot));
    }

    private MessageResult lapManual(Message message) {
        if (state != RUNNING) {
            LOG.info("Lap manual ignored: state is not running");
            throw new LapsCounterException("Lap manual ignored: state is not running");
        }
        if (message.getLaps() == null) {
            throw new LapsCounterException("Laps count is not setup");
        }

        Robot robot = getRobotOrElseThrow(message.getSerial());
        if (message.getLaps() > 0) {
            incLaps(robot, stopwatch.getTime());
        } else {
            robot.decLaps(); // TODO restore time
        }
        return MessageResult.broadcast(getLap(robot));
    }

    private MessageResult frame(Message message) {
        if (state != RUNNING) {
            LOG.info("Frame ignored: state is not running");
            return null;
        }
        Robot robot = getRobotOrElseThrow(message.getSerial());

        long raceTime = stopwatch.getTime();
        Type frameType = frameProcessor.checkFrame(robot, message.getFrame(), raceTime);
        if (frameType == Type.LAP) {
            incLaps(robot, raceTime);
            return MessageResult.broadcast(getLap(robot));
        } else if (frameType == Type.FRAME) {
            return MessageResult.single(new Message(Type.FRAME));
        }
        return null;
    }

    private void incLaps(Robot robot, long raceTime) {
        robot.incLaps();
        robot.setTime(raceTime);
    }

    private List<Message> getLapMessages() {
        return robots.stream()
                .map(this::getLap)
                .collect(Collectors.toList());
    }

    public Message scheduled() {
        if (state == RUNNING) {
            LOG.info("Send time");
            return getTime();
        }
        return null;
    }

    public Message getState() {
        Message message = new Message();
        message.setType(Type.STATE);
        message.setState(state);
        return message;
    }

    public Message getTime() {
        Message message = new Message();
        message.setType(Type.TIME);
        message.setTime(stopwatch.getTime());
        return message;
    }

    private Message getLap(Robot robot) {
        Message message = new Message();
        message.setType(Type.LAP);
        message.setSerial(robot.getSerial());
        message.setName(robot.getName());
        message.setNum(robot.getNum());
        message.setLaps(robot.getLaps());
        message.setTime(robot.getTime());
        return message;
    }

    private Robot getRobotOrElseThrow(Integer serial) {
        return Optional.ofNullable(serial)
                .flatMap(this::getRobot)
                .orElseThrow(() -> new LapsCounterException("Cannot find robot by serial"));
    }

    private Optional<Robot> getRobot(Integer serial) {
        return robots.stream().filter(r -> r.getSerial() == serial).findAny();
    }
}
