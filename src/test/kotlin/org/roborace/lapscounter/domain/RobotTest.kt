package org.roborace.lapscounter.domain

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RobotTest {
    private lateinit var robot: Robot

    @BeforeEach
    fun setUp() {
        robot = Robot(serial = 101, name = "TestRobot", num = 1)
    }

    @Test
    fun testInitialState() {
        assertThat(robot.serial, equalTo(101))
        assertThat(robot.name, equalTo("TestRobot"))
        assertThat(robot.num, equalTo(1))
        assertThat(robot.place, equalTo(0))
        assertThat(robot.laps, equalTo(0))
        assertThat(robot.time, equalTo(0L))
        assertThat(robot.currentLapStartTime, equalTo(0L))
        assertThat(robot.lastLapTime, equalTo(0L))
        assertThat(robot.bestLapTime, equalTo(0L))
        assertThat(robot.pitStopFinishTime, nullValue())
    }

    @Test
    fun testIncLapsFirstLap() {
        robot.incLaps(5000L)

        assertThat(robot.laps, equalTo(1))
        assertThat(robot.time, equalTo(5000L))
        assertThat(robot.currentLapStartTime, equalTo(5000L))
        assertThat(robot.lastLapTime, equalTo(5000L))
        assertThat(robot.bestLapTime, equalTo(5000L))
    }

    @Test
    fun testIncLapsSecondLap() {
        robot.incLaps(5000L)
        robot.incLaps(8000L)

        assertThat(robot.laps, equalTo(2))
        assertThat(robot.time, equalTo(8000L))
        assertThat(robot.currentLapStartTime, equalTo(8000L))
        assertThat(robot.lastLapTime, equalTo(3000L)) // 8000 - 5000
        assertThat(robot.bestLapTime, equalTo(3000L)) // Better than first lap
    }

    @Test
    fun testIncLapsSlowerLap() {
        robot.incLaps(3000L)
        robot.incLaps(8000L)

        assertThat(robot.laps, equalTo(2))
        assertThat(robot.time, equalTo(8000L))
        assertThat(robot.currentLapStartTime, equalTo(8000L))
        assertThat(robot.lastLapTime, equalTo(5000L)) // 8000 - 3000
        assertThat(robot.bestLapTime, equalTo(3000L)) // First lap was better
    }

    @Test
    fun testIncLapsMultipleLaps() {
        robot.incLaps(2000L)
        robot.incLaps(5000L)
        robot.incLaps(7500L)

        assertThat(robot.laps, equalTo(3))
        assertThat(robot.time, equalTo(7500L))
        assertThat(robot.currentLapStartTime, equalTo(7500L))
        assertThat(robot.lastLapTime, equalTo(2500L)) // 7500 - 5000
        assertThat(robot.bestLapTime, equalTo(2000L)) // First lap was best
    }

    @Test
    fun testDecLapFromMultipleLaps() {
        robot.incLaps(2000L)
        robot.incLaps(5000L)
        robot.incLaps(7500L)
        
        robot.decLap()

        assertThat(robot.laps, equalTo(2))
        assertThat(robot.time, equalTo(5000L)) // Back to second lap time
    }

    @Test
    fun testDecLapFromOneLap() {
        robot.incLaps(2000L)
        
        robot.decLap()

        assertThat(robot.laps, equalTo(0))
        assertThat(robot.time, equalTo(0L)) // Back to zero
    }

    @Test
    fun testDecLapFromZeroLaps() {
        robot.decLap()

        assertThat(robot.laps, equalTo(-1))
        assertThat(robot.time, equalTo(0L))
    }

    @Test
    fun testReset() {
        robot.incLaps(2000L)
        robot.incLaps(5000L)
        robot.pitStopFinishTime = 10000L
        
        robot.reset()

        assertThat(robot.laps, equalTo(0))
        assertThat(robot.time, equalTo(0L))
        assertThat(robot.bestLapTime, equalTo(0L))
        assertThat(robot.lastLapTime, equalTo(0L))
        assertThat(robot.currentLapStartTime, equalTo(0L))
        assertThat(robot.pitStopFinishTime, nullValue())
    }

    @Test
    fun testBestLapTimeTracking() {
        robot.incLaps(5000L) // First lap: 5000ms
        robot.incLaps(8000L) // Second lap: 3000ms (better)
        robot.incLaps(12000L) // Third lap: 4000ms (worse than best)
        robot.incLaps(14500L) // Fourth lap: 2500ms (new best)

        assertThat(robot.bestLapTime, equalTo(2500L))
        assertThat(robot.lastLapTime, equalTo(2500L))
    }

    @Test
    fun testLapTimesAfterDecLap() {
        robot.incLaps(3000L)
        robot.incLaps(6000L)
        robot.incLaps(9000L)
        
        // Best lap time should be 3000ms (first lap)
        assertThat(robot.bestLapTime, equalTo(3000L))
        
        robot.decLap()
        
        // After decLap, bestLapTime should remain the same (it's not recalculated)
        assertThat(robot.bestLapTime, equalTo(3000L))
        assertThat(robot.time, equalTo(6000L))
    }

    @Test
    fun testCurrentLapStartTimeProgression() {
        assertThat(robot.currentLapStartTime, equalTo(0L))
        
        robot.incLaps(1000L)
        assertThat(robot.currentLapStartTime, equalTo(1000L))
        
        robot.incLaps(3500L)
        assertThat(robot.currentLapStartTime, equalTo(3500L))
        
        robot.incLaps(7000L)
        assertThat(robot.currentLapStartTime, equalTo(7000L))
    }

    @Test
    fun testPitStopFinishTime() {
        robot.pitStopFinishTime = 15000L
        assertThat(robot.pitStopFinishTime, equalTo(15000L))
        
        robot.reset()
        assertThat(robot.pitStopFinishTime, nullValue())
    }
}