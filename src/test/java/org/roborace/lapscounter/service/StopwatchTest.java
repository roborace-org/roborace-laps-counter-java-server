package org.roborace.lapscounter.service;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

class StopwatchTest {

    private static final long DELAY = 125L;

    @Test
    void testHappyPath() throws InterruptedException {
        Stopwatch stopwatch = new Stopwatch();
        assertThat(stopwatch.getTime(), is(0L));

        stopwatch.start();
        Thread.sleep(DELAY);
        assertThat(stopwatch.getTime(), lessThanOrEqualTo(DELAY + 10));

        stopwatch.finish();
        assertThat(stopwatch.getTime(), lessThanOrEqualTo(DELAY + 20));
    }

    @Test
    void testStartAfterFinish() throws InterruptedException {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        Thread.sleep(DELAY);
        assertThat(stopwatch.getTime(), lessThanOrEqualTo(DELAY + 10));

        stopwatch.finish();
        assertThat(stopwatch.getTime(), lessThanOrEqualTo(DELAY + 10));

        stopwatch.start();
        assertThat(stopwatch.getTime(), is(0L));
        Thread.sleep(DELAY);
        assertThat(stopwatch.getTime(), lessThanOrEqualTo(DELAY + 10));
    }

    @Test
    void testReset() throws InterruptedException {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        Thread.sleep(DELAY);
        assertThat(stopwatch.getTime(), lessThanOrEqualTo(DELAY + 10));

        stopwatch.finish();
        assertThat(stopwatch.getTime(), lessThanOrEqualTo(DELAY + 10));

        stopwatch.reset();
        Thread.sleep(DELAY);
        assertThat(stopwatch.getTime(), is(0L));
    }

}