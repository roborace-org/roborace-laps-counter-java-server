package org.roborace.lapscounter.service.util

class Stopwatch {
    private var startTime: Long = 0
    private var endTime: Long = 0

    fun time() = (if (isRunning()) millis() else endTime) - startTime

    fun start(): Stopwatch {
        startTime = millis()
        endTime = 0
        return this
    }

    fun finish() {
        endTime = millis()
    }

    fun reset() {
        startTime = 0
        endTime = 0
    }

    fun isRunning() = startTime != 0L && endTime == 0L

    private fun millis() = System.currentTimeMillis()
}
