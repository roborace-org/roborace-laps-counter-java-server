package org.roborace.lapscounter.config

import org.roborace.lapscounter.domain.State
import org.roborace.lapscounter.service.LapsCounterService
import org.roborace.lapscounter.service.RoboraceWebSocketHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.lang.Thread.yield
import java.time.Instant
import java.util.concurrent.Executor
import java.util.concurrent.Executors


@Configuration
@EnableScheduling
class DynamicSchedulingConfig : SchedulingConfigurer {
    @Autowired
    private lateinit var lapsCounterService: LapsCounterService

    @Autowired
    private lateinit var webSocketHandler: RoboraceWebSocketHandler

    @Value("\${laps.time-send-interval:10000}")
    private val timeSendInterval: Long = 0

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor())
        taskRegistrar.addTriggerTask(
            {
                if (lapsCounterService.getState().state == State.RUNNING) {
                    while (lapsCounterService.stopwatch.time() % timeSendInterval != 0L) {
                        yield()
                    }
                }
                lapsCounterService.scheduled().let {
                    webSocketHandler.broadcast(it)
                }
            },
            {
                val time = lapsCounterService.stopwatch.time()
                val delay = timeSendInterval - (time.takeIf { time < timeSendInterval } ?: 0)
                Instant.now().plusMillis(delay - 10)
            }
        )
    }

    fun taskExecutor(): Executor = Executors.newSingleThreadScheduledExecutor()
}