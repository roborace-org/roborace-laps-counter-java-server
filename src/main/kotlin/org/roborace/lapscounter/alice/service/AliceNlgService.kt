package org.roborace.lapscounter.alice.service

import org.roborace.lapscounter.domain.State
import kotlin.math.abs

object AliceNlgService {

    fun numberMalePhrase(serial: Int) =
        when (serial) {
            1 -> "первый"
            2 -> "второй"
            3 -> "третий"
            4 -> "четвертый"
            5 -> "пятый"
            6 -> "шестой"
            else -> serial.toString()
        }

    fun numberFemalePhrase(serial: Int) =
        when (serial) {
            1 -> "первая"
            2 -> "вторая"
            3 -> "третья"
            4 -> "четвертая"
            5 -> "пятая"
            6 -> "шестая"
            else -> serial.toString()
        }

    fun numberItPhrase(serial: Int) =
        when (serial) {
            1 -> "первое"
            2 -> "второе"
            3 -> "третье"
            4 -> "четвертое"
            5 -> "пятое"
            6 -> "шестое"
            else -> "$serial-ое"
        }

    fun lapsIncPhrase(laps: Int) =
        when (laps) {
            1 -> "круг"
            -1 -> "минус"
            else -> laps.toString()
        }

    fun lapsPhrase(laps: Int) =
        when (abs(laps) % 10) {
            1 -> "круг"
            2, 3, 4 -> "круга"
            else -> "кругов"
        }

    fun commandPhrase(command: State) =
        when (command) {
            State.READY -> "на старт"
            State.STEADY -> "внимание"
            State.RUNNING -> "марш"
            State.FINISH -> "финиш"
        }

    fun pastMinutesPhrase(minutes: Long) =
        if (minutes in 11..14)
            "Прошло $minutes минут"
        else
            when (minutes.toInt() % 10) {
                1 -> "Прошла $minutes минута"
                2, 3, 4 -> "Прошло $minutes минуты"
                else -> "Прошло $minutes минут"
            }
}
