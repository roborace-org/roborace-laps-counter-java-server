package org.roborace.lapscounter.service.util

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.jupiter.api.Test

internal class StopwatchTest {
    @Test
    fun testHappyPath() {
        val stopwatch = Stopwatch()
            .assertTimeEqual(0L)

        stopwatch.start()
            .sleep(DELAY)
            .assertTimeEqualSmooth(DELAY)

        stopwatch.stop()
            .assertTimeEqualSmooth(DELAY)
    }

    @Test
    fun testTimeStillSameAfterFinish() {
        val stopwatch = Stopwatch()
            .start()
            .sleep(DELAY)
            .stop()
            .assertTimeEqualSmooth(DELAY)

        stopwatch.sleep(DELAY)
            .assertTimeEqualSmooth(DELAY)
    }

    @Test
    fun testStartAfterFinish() {
        val stopwatch = Stopwatch().start()
            .sleep(DELAY)
            .stop()
            .assertTimeEqualSmooth(DELAY)

        stopwatch.start()
            .assertTimeEqual(0L)
            .sleep(DELAY)
            .assertTimeEqualSmooth(DELAY)
    }

    @Test
    fun testReset() {
        val stopwatch = Stopwatch().start()
            .sleep(DELAY)
            .assertTimeEqualSmooth(DELAY)
            .stop()
            .assertTimeEqualSmooth(DELAY)

        stopwatch.reset()
            .sleep(DELAY)
            .assertTimeEqual(0L)
    }

    @Test
    fun testPauseContinue() {
        val stopwatch = Stopwatch().start()
            .sleep(DELAY)
            .stop()
            .sleep(DELAY)

        stopwatch.`continue`()
            .assertTimeEqualSmooth(DELAY)
            .sleep(DELAY)
            .assertTimeEqualSmooth(2 * DELAY)
    }

    @Test
    fun testPauseContinueFinish() {
        val stopwatch = Stopwatch().start()
            .sleep(DELAY)
            .stop()
            .sleep(DELAY)
            .`continue`()
            .sleep(DELAY)

        stopwatch.stop()
            .assertTimeEqualSmooth(2 * DELAY)
            .sleep(DELAY)
            .assertTimeEqualSmooth(2 * DELAY)
    }

    companion object {
        private const val DELAY = 125L
    }
}

private fun Stopwatch.sleep(delay: Long): Stopwatch {
    Thread.sleep(delay)
    return this
}

private fun Stopwatch.assertTimeEqual(ms: Long) = this.apply {
    assertThat(this.time(), `is`(ms))
}

private fun Stopwatch.assertTimeEqualSmooth(ms: Long) = this.apply {
    val time = this.time()
    assertThat(time, lessThanOrEqualTo(ms + 15))
    assertThat(time, greaterThanOrEqualTo(ms))
}
