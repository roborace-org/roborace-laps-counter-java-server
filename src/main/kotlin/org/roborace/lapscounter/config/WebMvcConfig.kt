package org.roborace.lapscounter.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig {
    @Bean
    fun forwardToIndex() = object : WebMvcConfigurer {
        override fun addViewControllers(registry: ViewControllerRegistry) {
            registry.addViewController("/").setViewName("/index.html")
            registry.addViewController("/admin").setViewName("/admin.html")
        }
    }
}
