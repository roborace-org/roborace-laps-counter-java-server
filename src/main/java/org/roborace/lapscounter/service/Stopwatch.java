package org.roborace.lapscounter.service;

public class Stopwatch {

    private long startTime;
    private long endTime;

    public long getTime() {
        if (endTime >= startTime) {
            return endTime - startTime;
        }
        return millis() - startTime;
    }

    public void start() {
        startTime = millis();
    }

    public void finish() {
        endTime = millis();
    }
    public void reset() {
        startTime = 0;
        endTime = 0;
    }

    private long millis() {
        return System.currentTimeMillis();
    }

}
