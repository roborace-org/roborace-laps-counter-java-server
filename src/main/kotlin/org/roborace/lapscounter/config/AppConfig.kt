package org.roborace.lapscounter.config

import org.roborace.lapscounter.service.robofinist.Robofinist
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
class AppConfig {
    @Bean
    fun taskScheduler() = ThreadPoolTaskScheduler().apply {
        poolSize = 1
    }

    @Bean
    fun robofinist2() = Robofinist()

}
