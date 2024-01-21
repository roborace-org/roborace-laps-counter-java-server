package org.roborace.lapscounter.service.util

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.jupiter.api.Test

internal class StopwatchTest {
    @Test
    fun testHappyPath() {
        val stopwatch = Stopwatch()
        assertThat(stopwatch.time(), `is`(0L))

        stopwatch.start()
        Thread.sleep(DELAY)
        assertThat(stopwatch.time(), lessThanOrEqualTo(DELAY + 10))

        stopwatch.finish()
        assertThat(stopwatch.time(), lessThanOrEqualTo(DELAY + 20))
    }

    @Test
    fun testStartAfterFinish() {
        val stopwatch = Stopwatch().start()
        Thread.sleep(DELAY)
        assertThat(stopwatch.time(), lessThanOrEqualTo(DELAY + 10))

        stopwatch.finish()
        assertThat(stopwatch.time(), lessThanOrEqualTo(DELAY + 10))

        stopwatch.start()
        assertThat(stopwatch.time(), `is`(0L))
        Thread.sleep(DELAY)
        assertThat(stopwatch.time(), lessThanOrEqualTo(DELAY + 10))
    }

    @Test
    fun testReset() {
        val stopwatch = Stopwatch().start()
        Thread.sleep(DELAY)
        assertThat(stopwatch.time(), lessThanOrEqualTo(DELAY + 10))

        stopwatch.finish()
        assertThat(stopwatch.time(), lessThanOrEqualTo(DELAY + 10))

        stopwatch.reset()
        Thread.sleep(DELAY)
        assertThat(stopwatch.time(), `is`(0L))
    }

    companion object {
        private const val DELAY = 125L
    }
}