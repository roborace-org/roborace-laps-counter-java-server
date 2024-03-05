package org.roborace.lapscounter.service

import org.roborace.lapscounter.domain.State
import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.api.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.Timer
import java.util.TimerTask

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
                log.debug("Connected websocket clients: {}", it)
            }
    }

    fun addSchedulerForFinishRace(targetMs: Long) {
        log.info("Scheduling finish in $targetMs ms")
        addScheduler(targetMs - lapsCounterService.stopwatch.time()) {
            while (lapsCounterService.stopwatch.time() < targetMs) Thread.yield()
            lapsCounterService.handleMessage(FINISH_MESSAGE).also {
                webSocketHandler.broadcast(it.messages)
            }
            log.info("Race is finished by time limit")
        }
    }

    fun addSchedulerForPitStop(message: Message, delayMs: Long) {
        addScheduler(delayMs) {
            webSocketHandler.broadcast(message)
            log.info("Pit stop is finished ${message.serial}")
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
        private val log: Logger = LoggerFactory.getLogger(LapsCounterService::class.java)

        private val FINISH_MESSAGE = Message(Type.COMMAND, state = State.FINISH)
    }
}
