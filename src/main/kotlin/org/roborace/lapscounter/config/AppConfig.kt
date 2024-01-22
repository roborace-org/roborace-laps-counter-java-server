package org.roborace.lapscounter.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
class AppConfig {
    @Bean
    fun taskScheduler() = ThreadPoolTaskScheduler().apply {
        poolSize = 1
    }
}
