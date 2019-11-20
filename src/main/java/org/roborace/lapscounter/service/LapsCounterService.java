package org.roborace.lapscounter.service;

import org.roborace.lapscounter.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingInt;
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


    public synchronized MessageResult handleMessage(Message message) {

        switch (message.getType()) {
            case COMMAND:
                return command(message);
            case STATE:
                return MessageResult.single(getState());
            case ROBOT_INIT:
                return robotInit(message);
            case ROBOT_EDIT:
                return robotEdit(message);
            case ROBOT_REMOVE:
                return robotRemove(message);
            case TIME:
                return MessageResult.single(getTime());
            case LAPS:
                return new MessageResult(getLapMessages(robots), ResponseType.SINGLE);
            case LAP_MAN:
                return lapManual(message);
            case FRAME:
                return frame(message);
            case LAP:
            case ERROR:
            default:
                throw new LapsCounterException("Method not supported: [" + message.getType() + "]");
        }
    }

    public List<Message> afterConnectionEstablished() {
        if (state == RUNNING) {
            return Arrays.asList(getState(), getTime());
        }
        return Collections.singletonList(getState());
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
                    messageResult.addAll(getLapMessages(robots));
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
        return newState.ordinal() == 0 && state.ordinal() == values().length - 1;
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
            robot.setName("Robot " + message.getSerial());
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

    private MessageResult robotRemove(Message message) {
        Robot robot = getRobotOrElseThrow(message.getSerial());
        robots.remove(robot);
        frameProcessor.robotRemove(robot);
        MessageResult broadcast = MessageResult.broadcast();
        broadcast.add(message);
        broadcast.addAll(getLapMessages(sortRobotsByLapsAndTime()));
        return broadcast;
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
        List<Robot> affectedRobots;
        if (message.getLaps() > 0) {
            affectedRobots = incLaps(robot, stopwatch.getTime());
        } else {
            affectedRobots = decLaps(robot);
        }
        return MessageResult.broadcast(getLapMessages(affectedRobots));
    }

    List<Robot> sortRobotsByLapsAndTime() {
        robots.sort(comparingInt(Robot::getLaps).reversed()
                .thenComparingLong(Robot::getTime)
                .thenComparingInt(Robot::getNum));
        List<Robot> affectedRobots = new ArrayList<>(robots.size());
        for (int i = 0; i < robots.size(); i++) {
            int place = i + 1;
            Robot robot = robots.get(i);
            if (robot.getPlace() != place) {
                robot.setPlace(place);
                affectedRobots.add(robot);
            }
        }
        return affectedRobots;
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
            List<Robot> robots = incLaps(robot, raceTime);
            return MessageResult.broadcast(getLapMessages(robots));
        } else if (frameType == Type.FRAME) {
            if (frameProcessor.isStartFrame(message.getFrame())) {
                robot.setCurrentLapStartTime(raceTime);
            }
            return MessageResult.single(new Message(Type.FRAME));
        }
        return null;
    }

    private List<Robot> incLaps(Robot robot, long raceTime) {
        robot.incLaps(raceTime);
        List<Robot> affectedRobots = sortRobotsByLapsAndTime();
        if (affectedRobots.isEmpty()) {
            affectedRobots.add(robot);
        }
        return affectedRobots;
    }

    private List<Robot> decLaps(Robot robot) {
        robot.decLaps();
        robot.setTime(robot.extractLastLapTime());
        List<Robot> affectedRobots = sortRobotsByLapsAndTime();
        if (affectedRobots.isEmpty()) {
            affectedRobots.add(robot);
        }
        return affectedRobots;
    }

    private List<Message> getLapMessages(List<Robot> robots) {
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
        message.setBestLapTime(robot.getBestLapTime());
        message.setLastLapTime(robot.getLastLapTime());
        message.setPlace(robot.getPlace());
        message.setLastLapTime(robot.getLastLapTime());
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
