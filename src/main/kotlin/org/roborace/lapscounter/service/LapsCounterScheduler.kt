package org.roborace.lapscounter.service

import mu.KotlinLogging
import org.roborace.lapscounter.domain.State
import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.api.Message
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.Timer
import java.util.TimerTask

private val logger = KotlinLogging.logger {}

@Service
class LapsCounterScheduler(
    private val webSocketHandler: RoboraceWebSocketHandler,
    private val lapsCounterService: LapsCounterService,
) {

    private var timer = Timer()

    @Scheduled(fixedRate = 10000)
    fun showStat() {
        webSocketHandler.getSessionsCopy()
            .joinToString(", ") {
                "${it.remoteAddress} open:${it.isOpen}"
            }.also {
                logger.debug("Connected websocket clients: {}", it)
            }
    }

    fun addSchedulerForFinishRace(targetMs: Long) {
        logger.info("Scheduling finish in $targetMs ms")
        addScheduler(targetMs - lapsCounterService.stopwatch.time()) {
            while (lapsCounterService.stopwatch.time() < targetMs) Thread.yield()
            lapsCounterService.handleMessage(FINISH_MESSAGE).also {
                webSocketHandler.broadcast(it.messages)
            }
            logger.info("Race is finished by time limit")
        }
    }

    fun addSchedulerForPitStop(message: Message, delayMs: Long) {
        addScheduler(delayMs) {
            webSocketHandler.broadcast(message)
            logger.info("Pit stop is finished ${message.serial}")
        }
    }

    fun addScheduler(delayMs: Long, runnable: Runnable) {
        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    runnable.run()
                }
            },
            delayMs - 10
        )
    }

    fun resetSchedulers() {
        timer.cancel()
        timer.purge()
        timer = Timer()
    }

    companion object {
        private val FINISH_MESSAGE = Message(Type.COMMAND, state = State.FINISH)
    }
}
