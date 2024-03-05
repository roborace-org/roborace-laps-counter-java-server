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

    fun `continue`(): Stopwatch {
        startTime = millis() - time()
        endTime = 0
        return this
    }

    fun stop(): Stopwatch {
        endTime = millis()
        return this
    }

    fun reset(): Stopwatch {
        startTime = 0
        endTime = 0
        return this
    }

    fun isRunning() = startTime != 0L && endTime == 0L

    companion object {
        private fun millis() = System.currentTimeMillis()
    }
}
