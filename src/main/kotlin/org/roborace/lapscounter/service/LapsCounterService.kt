package org.roborace.lapscounter.service

import org.roborace.lapscounter.domain.LapsCounterException
import org.roborace.lapscounter.domain.Robot
import org.roborace.lapscounter.domain.State
import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.api.Message
import org.roborace.lapscounter.domain.api.MessageResult
import org.roborace.lapscounter.domain.api.MessageResult.Companion.broadcast
import org.roborace.lapscounter.domain.api.MessageResult.Companion.single
import org.roborace.lapscounter.domain.api.ResponseType
import org.roborace.lapscounter.service.util.Stopwatch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class LapsCounterService(
    private val frameProcessor: FrameProcessor,
    @Value("\${laps.pit-stop-time}") private val pitStopTime: Long = 0
) {

    @Lazy
    @Autowired
    private lateinit var lapsCounterScheduler: LapsCounterScheduler

    private var state = State.READY
    private var raceTimeLimit: Long = 0
    val stopwatch = Stopwatch()
    private val robots: MutableList<Robot> = mutableListOf()


    @Synchronized
    fun handleMessage(message: Message): MessageResult =
        when (message.type) {
            Type.COMMAND -> command(message)
            Type.STATE -> single(getState())
            Type.ROBOT_INIT -> robotInit(message)
            Type.ROBOT_EDIT -> robotEdit(message)
            Type.ROBOT_REMOVE -> robotRemove(message)
            Type.TIME -> timeRequest(message)
            Type.LAPS -> single(getLapMessages(robots).toMutableList())
            Type.LAP_MAN -> lapManual(message)
            Type.PIT_STOP -> pitStop(message)
            Type.FRAME -> frame(message)
            else -> throw LapsCounterException("Method not supported: [${message.type}]")
        }

    fun afterConnectionEstablished() = listOf(getState(), timeMessage())

    private fun command(message: Message): MessageResult {
        val parsedState = message.state ?: throw LapsCounterException("State is null")
        if (!isCorrectCommand(parsedState)) {
            throw LapsCounterException("Wrong current state to apply command: [$state]->[$parsedState]")
        }
        if (parsedState == state) {
            throw LapsCounterException("State already set up: [$state]->[$parsedState]")
        }

        val messageResult = MessageResult(ResponseType.BROADCAST)
        state = parsedState
        messageResult.add(getState())
        when (state) {
            State.READY -> {
                stopwatch.reset()
                robots.forEach { it.reset() }
                frameProcessor.reset()
                sortRobotsByLapsAndTime()
                messageResult.addAll(getLapMessages(robots))
            }

            State.STEADY -> {
            }

            State.RUNNING -> {
                stopwatch.start()
                if (raceTimeLimit > 0) {
                    lapsCounterScheduler.addSchedulerForFinishRace(TimeUnit.SECONDS.toMillis(raceTimeLimit))
                }
            }

            State.FINISH -> {
                stopwatch.finish()
                lapsCounterScheduler.resetSchedulers()
            }
        }

        messageResult.add(timeMessage())
        return messageResult
    }

    private fun isCorrectCommand(newState: State) =
        if (newState.ordinal == state.ordinal + 1) true
        else newState.ordinal == 0 && state.ordinal == State.entries.size - 1

    private fun robotInit(message: Message): MessageResult {
        val serial = message.serial ?: throw LapsCounterException("Robot serial is not defined")
        val existing = findRobot(serial)
        val robot: Robot =
            if (existing != null) {
                log.info("Reconnect robot {}", existing)
                existing
            } else {
                Robot(
                    serial = serial,
                    name = message.name ?: ("Robot $serial"),
                    num = maxNum() + 1,
                    place = robots.size + 1,
                ).also {
                    it.reset()
                    robots.add(it)
                    frameProcessor.robotInit(it.serial)
                    log.info("Connect new robot {}", it)
                }
            }
        log.debug("Connected robots: {}", robots)
        return broadcast(getLap(robot))
    }

    private fun maxNum() = robots.maxOfOrNull { it.num } ?: 0

    private fun robotEdit(message: Message): MessageResult {
        val robot = findRobotOrElseThrow(message.serial)
        log.info("Edit robot {}", robot)
        robot.name = message.name
        return broadcast(getLap(robot))
    }

    private fun robotRemove(message: Message): MessageResult {
        val robot = findRobotOrElseThrow(message.serial)
        robots.remove(robot)
        frameProcessor.robotRemove(robot.serial)
        return broadcast(listOf(message) + getLapMessages(sortRobotsByLapsAndTime()))
    }

    private fun timeRequest(message: Message): MessageResult =
        if (message.raceTimeLimit != null && state != State.RUNNING) {
            raceTimeLimit = message.raceTimeLimit
            broadcast(timeMessage())
        } else single(timeMessage())

    private fun lapManual(message: Message): MessageResult {
        if (state != State.RUNNING) {
            log.info("Lap manual ignored: state is not running")
            throw LapsCounterException("Lap manual ignored: state is not running")
        }
        if (message.laps == null) {
            throw LapsCounterException("Laps count is not defined")
        }

        val robot = findRobotOrElseThrow(message.serial)
        val affectedRobots = if (message.laps > 0) {
            incLaps(robot, stopwatch.time())
        } else {
            decLaps(robot)
        }
        return broadcast(getLapMessages(affectedRobots))
    }

    fun sortRobotsByLapsAndTime(): List<Robot> {
        val comparator = Comparator.comparingInt(Robot::laps).reversed()
            .thenComparingLong(Robot::time)
            .thenComparingInt(Robot::num)
        robots.sortWith(comparator)
        return robots.mapIndexedNotNull { index, robot ->
            robot.takeIf { robot.place != index + 1 }
                .also { robot.place = index + 1 }
        }
    }

    private fun pitStop(message: Message): MessageResult {
        if (state != State.RUNNING) {
            log.debug("PitStop ignored: state is not running")
            throw LapsCounterException("PitStop ignored: state is not running")
        }
        val robot = findRobotOrElseThrow(message.serial)

        robot.pitStopFinishTime = stopwatch.time() + pitStopTime
        log.info("Used PIT_STOP for robot: {}", robot.serial)

        val pitStopFinish = Message(Type.PIT_STOP_FINISH, serial = robot.serial)
        lapsCounterScheduler.addSchedulerForPitStop(pitStopFinish, pitStopTime)

        val pitStopStart = Message(Type.PIT_STOP, serial = robot.serial)
        return broadcast(listOf(pitStopStart, getLap(robot)))
    }

    private fun frame(message: Message): MessageResult {
        if (state != State.RUNNING) {
            log.debug("Frame ignored: state is not running")
            return MessageResult(ResponseType.SINGLE)
        }
        if (message.frame == null) {
            log.debug("Frame is null")
            return MessageResult(ResponseType.SINGLE)
        }
        val robot = findRobotOrElseThrow(message.serial)

        val raceTime = stopwatch.time()
        val frameType = frameProcessor.checkFrame(robot.serial, message.frame, raceTime)
        return when (frameType) {
            Type.LAP -> {
                val robots = incLaps(robot, raceTime)
                broadcast(getLapMessages(robots))
            }

            Type.LAP_MINUS -> {
                val robots = decLaps(robot)
                broadcast(getLapMessages(robots))
            }

            Type.FRAME -> {
                if (frameProcessor.isStartFrame(message.frame)) {
                    robot.currentLapStartTime = raceTime
                }
                single(Message(Type.FRAME))
            }

            else -> MessageResult(ResponseType.SINGLE)
        }
    }

    private fun incLaps(robot: Robot, raceTime: Long): List<Robot> {
        robot.incLaps(raceTime)
        return sortRobotsByLapsAndTime().ifEmpty {
            listOf(robot)
        }
    }

    private fun decLaps(robot: Robot): List<Robot> {
        robot.decLap()
        return sortRobotsByLapsAndTime().ifEmpty {
            listOf(robot)
        }
    }

    private fun getLapMessages(robots: List<Robot>) = robots.map { getLap(it) }

    fun scheduled(): Message =
        timeMessage().also {
            log.debug("Send time")
        }

    fun getState(): Message = Message(Type.STATE, state = state)

    fun timeMessage() = Message(Type.TIME, time = stopwatch.time(), raceTimeLimit = raceTimeLimit)

    private fun getLap(robot: Robot) =
        Message(
            type = Type.LAP,
            serial = robot.serial,
            name = robot.name,
            num = robot.num,
            laps = robot.laps,
            time = robot.time,
            bestLapTime = robot.bestLapTime,
            lastLapTime = robot.lastLapTime,
            place = robot.place,
        ).apply {
            robot.pitStopFinishTime?.let { pitStopFinishTime = it }
        }

    private fun findRobotOrElseThrow(serial: Int?) =
        findRobot(serial)
            ?: throw LapsCounterException("Cannot find robot by serial $serial")

    private fun findRobot(serial: Int?): Robot? =
        (serial ?: throw LapsCounterException("Robot serial is not defined"))
            .let { serialNotNull ->
                robots.find { it.serial == serialNotNull }
            }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(LapsCounterService::class.java)
    }
}
