package org.roborace.lapscounter.service.robofinist

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.roborace.lapscounter.service.robofinist.model.BaseRequest
import org.roborace.lapscounter.service.robofinist.model.bids.BidsSearchRequest
import org.roborace.lapscounter.service.robofinist.model.event.EventAdminAddRequest
import org.roborace.lapscounter.service.robofinist.model.event.EventAdminNotificationAddAppealRequest
import org.roborace.lapscounter.service.robofinist.model.event.EventAdminNotificationAddRequest
import org.roborace.lapscounter.service.robofinist.model.event.EventProgramSearchRequest
import org.roborace.lapscounter.service.robofinist.model.event.EventProgramSearchResponse
import org.roborace.lapscounter.service.robofinist.model.program.ProgramCreateRequest
import org.roborace.lapscounter.service.robofinist.model.program.ProgramCreateResponse
import org.roborace.lapscounter.service.robofinist.model.result.ResultCreateRequest
import org.roborace.lapscounter.service.robofinist.model.result.ResultCreateRequest.ResultParams
import org.roborace.lapscounter.service.robofinist.model.stage.StageCreateRequest
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import kotlin.random.Random

@Service
class Robofinist {

    fun addEventAdmins(eventId: Int) {
        User.entries.forEach {
            println("Add user $it")
            execute(EventAdminAddRequest(eventId = eventId, userId = it.id, token = commonToken))
        }
    }

    fun eventProgramsSearch(eventId: Int): Map<Program, Int> {
        val response = execute(EventProgramSearchRequest(eventId = eventId, token = commonToken))

        val programs = objectMapper.readValue(response, EventProgramSearchResponse::class.java)
        println("programs = $programs")

        val programsMap = programs.data.associate { Program.findByName(it.name) to it.id }
            .filterKeys { it != null }
        println("programsMap = $programsMap")
        return programsMap as Map<Program, Int>

    }

    fun eventProgramSearch(programId: Int) {
        execute(EventProgramSearchRequest(programId = programId, showStages = true, token = commonToken))
    }

    fun createRoboraceProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            eventId = eventId,
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Соревнования Roborace во многом похожи на соревнования Формулы 1, но с тем отличием, что соревнуются не управляемые пилотами болиды, а полностью автономные роботы. Роботы полагаются на показания своих датчиков, чтобы ориентироваться по трассе (ограниченной бортами), маневрировать, выбирать скорость движения и избегать столкновений с соперниками.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
        ))
            ?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Квалификация",
                    typeStart = 1,
                    typeFinal = 0,
                    countSingle = 3,
                    formula = "{\"name\":\"Круги\",\"params\":[{\"id\":\"Laps\",\"name\":\"Кол-во кругов\",\"type\":\"number\"},{\"id\":\"Time\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Круги\",\"type\":\"number\"},{\"formula\":\"[p1]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"Не уложился в заданное время круга\"}",
                ))
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Отборочный заезд",
                    typeStart = 0,
                    typeFinal = 0,
                    countSingle = 1,
                    formula = "{\"name\":\"Круги\",\"params\":[{\"id\":\"Laps\",\"name\":\"Кол-во кругов\",\"type\":\"number\"},{\"id\":\"Time\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Круги\",\"type\":\"number\"},{\"formula\":\"[p1]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"Не уложился в заданное время круга\"}",
                ))
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Финальный заезд",
                    typeStart = 0,
                    typeFinal = 1,
                    countSingle = 1,
                    formula = "{\"name\":\"Круги\",\"params\":[{\"id\":\"Laps\",\"name\":\"Кол-во кругов\",\"type\":\"number\"},{\"id\":\"Time\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Круги\",\"type\":\"number\"},{\"formula\":\"[p1]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"Не уложился в заданное время круга\"}",
                ))
            }
    }

    fun createRoboraceOkJrProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Соревнования Roborace во многом похожи на соревнования Формулы 1, но с тем отличием, что соревнуются не управляемые пилотами болиды, а полностью автономные роботы. Роботы полагаются на показания своих датчиков, чтобы ориентироваться по трассе (ограниченной бортами), маневрировать, выбирать скорость движения и избегать столкновений с соперниками.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        ))?.let {
            val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
            val programId = programCreateResponse.data.id.toInt()
            println("Created program $programId")
            execute(StageCreateRequest(
                programId = programId,
                name = "Квалификация",
                typeStart = 1,
                typeFinal = 0,
                countSingle = 3,
                formula = "{\"name\":\"Круг по времени\",\"params\":[{\"id\":\"p0\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:asc\",\"disq_reasons\":\"Не уложился в заданное время\"}",
            ))
            execute(StageCreateRequest(
                programId = programId,
                name = "Финальный",
                typeStart = 0,
                typeFinal = 1,
                countSingle = 0,
                challongeType = "playOff",
            ))
        }

    }

    fun createLineFollowerProgram(eventId: Int, program: Program, checkUniqueParticipant: Int = 1) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Задачей для робота в этом виде является преодоление трассы вдоль черной линии за наименьшее время. Робот должен ехать по черной линии в автоматическом режиме.</p>",
            checkUniqueParticipant = checkUniqueParticipant,
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Основной",
                    typeStart = 1,
                    typeFinal = 1,
                    countSingle = 3,
                    formula = "{\"name\":\"Время\",\"params\":[{\"id\":\"p0\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:asc\",\"disq_reasons\":\"\"}",
                    token = commonToken,
                ))
            }
        }
    }

    fun createBigJourneyJrProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>«Большое путешествие» — это дисциплина, составленная из нескольких классических упражнений, которые робот должен выполнить последовательно и без остановки.</p><p>За три минут роботу предстоит: проехать по линии, обогнув препятствие, преодолеть лабиринт, линию с горкой, а также выбить все банки в кегельринге.<br>Побеждает тот, чей робот набрал наибольшее количество баллов при прохождении трассы.&nbsp;</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Основной",
                    typeStart = 1,
                    typeFinal = 1,
                    countSingle = 2,
                    formula = "{\"name\":\"Большое путешествие младшая\",\"params\":[{\"id\":\"p0\",\"name\":\"Следование по линии с препятствием (max 40)\",\"type\":\"number\"},{\"id\":\"p1\",\"name\":\"Лабиринт (max 80)\",\"type\":\"number\"},{\"id\":\"p2\",\"name\":\"Следование по линии c горкой (max 40)\",\"type\":\"number\"},{\"id\":\"p3\",\"name\":\"Вытолкнуто кегель\",\"type\":\"number\"},{\"id\":\"p4\",\"name\":\"Время (повтор - 03:00:00)\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]+[p1]+[p2]+[p3]*5\",\"name\":\"Баллы\",\"type\":\"number\"},{\"formula\":\"[p4]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"\"}",
                    token = commonToken,
                ))
            }
        }
    }

    fun createBigJourneySrProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>«Большое путешествие» — это дисциплина, составленная из нескольких классических упражнений, которые робот должен выполнить последовательно и без остановки.</p><p>За пять минут роботу предстоит: проехать по линии, обогнув движущееся препятствие, преодолеть лабиринт, линию с инверсией, а также выбить все банки в кегельринге, кроме одной. Оставшуюся банку необходимо вернуть на старт, пройдя все препятствия в обратном порядке.</p><p>Побеждает тот, чей робот набрал наибольшее количество баллов при прохождении трассы.&nbsp;</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Основной",
                    typeStart = 1,
                    typeFinal = 1,
                    countSingle = 2,
                    formula = "{\"name\":\"Большое путешествие старшая\",\"params\":[{\"id\":\"p0\",\"name\":\"Следование по линии с движущимся препятствием (max 40)\",\"type\":\"number\"},{\"id\":\"p1\",\"name\":\"Лабиринт (max 80)\",\"type\":\"number\"},{\"id\":\"p2\",\"name\":\"Следование по инверсной линии (max 40)\",\"type\":\"number\"},{\"id\":\"p3\",\"name\":\"Вытолкнуто кегель (до 8)\",\"type\":\"number\"},{\"id\":\"p4\",\"name\":\"Кегельринг (max 40)\",\"type\":\"number\"},{\"id\":\"p5\",\"name\":\"Следование по инверсной линии обратно (max 40)\",\"type\":\"number\"},{\"id\":\"p6\",\"name\":\"Лабиринт обратно (max 80)\",\"type\":\"number\"},{\"id\":\"p7\",\"name\":\"Следование по линии с движущимся препятствием обратно (max 40)\",\"type\":\"number\"},{\"id\":\"p8\",\"name\":\"Время (повтор - 05:00:00)\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]+[p1]+[p2]+[p3]*5+[p4]+[p5]+[p6]+[p7]\",\"name\":\"Баллы\",\"type\":\"number\"},{\"formula\":\"[p8]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"\"}",
                    token = commonToken,
                ))
            }
        }
    }

    fun createRescueLineProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Участниками RoboСupJunior Rescue Line становятся самые смелые, ведь им предстоит сконструировать робота, который должен самостоятельно выполнить спасательную миссию. Роботу предстоит двигаться по линии через разрушенные препятствия, возвышенности, неровности, чтобы забрать пострадавших и вернуть их на базу, где им будет оказана помощь.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Попытки",
                    typeStart = 1,
                    typeFinal = 0,
                    countSingle = 2,
                    formula = "{\"name\":\"Rescue Line (по параметрам)\",\"params\":[{\"id\":\"p0\",\"name\":\"Преодоленных промежутков\",\"type\":\"number\"},{\"id\":\"p1\",\"name\":\"Преодоленных барьеров\",\"type\":\"number\"},{\"id\":\"p2\",\"name\":\"Преодоленных перекрестков/тупиков\",\"type\":\"number\"},{\"id\":\"p3\",\"name\":\"Преодоленных рамп\",\"type\":\"number\"},{\"id\":\"p4\",\"name\":\"Преодоленных препятствий\",\"type\":\"number\"},{\"id\":\"p5\",\"name\":\"Преодоленных качелей\",\"type\":\"number\"},{\"id\":\"p6\",\"name\":\"Пройдено клеток до контрольной точки с 1 попытки\",\"type\":\"number\"},{\"id\":\"p7\",\"name\":\"Пройдено клеток до контрольной точки со 2 попытки\",\"type\":\"number\"},{\"id\":\"p8\",\"name\":\"Пройдено клеток до контрольной точки с 3 попытки\",\"type\":\"number\"},{\"id\":\"p9\",\"name\":\"Спасено живых и мертвых жертв в соответствующие точки эвакуации - живые в зеленую, мертвая в красную (мертвые жертвы учитываются в случае спасения всех живых)\",\"type\":\"number\"},{\"id\":\"p10\",\"name\":\"Уровень пункта эвакуации (1 или 2)\",\"type\":\"number\"},{\"id\":\"p11\",\"name\":\"Спасательный комплект доставлен в пункт эвакуации со старта (1) после подбора (2)\",\"type\":\"number\"},{\"id\":\"p12\",\"name\":\"Отсутствий прогресса в зоне эвакуации (на участке между контрольными точками с зоной эвакуации)\",\"type\":\"number\"},{\"id\":\"p13\",\"name\":\"Всего отсутствий прогресса\",\"type\":\"number\"},{\"id\":\"p14\",\"name\":\"Достиг финишной клетки и остановился на 5 секунд\",\"type\":\"toggle\"},{\"id\":\"p15\",\"name\":\"Время заезда (max 08:00.00)\",\"type\":\"time\"}],\"results\":[{\"formula\":\"(([p0]*10+[p1]*5+[p2]*10+[p3]*10+[p4]*15+[p5]*15)+([p6]*5+[p7]*3+[p8]*1)+([p14]*if([p13]<12,(60-5*[p13]),0)))*(((if((1+0.2*[p10])-0.025*[p12]*[p10]>1,(1+0.2*[p10])-0.025*[p12]*[p10],1)^[p9]))*(1+([p10]*(([p11]^2-[p11])/2+[p11]))/10))\",\"name\":\"Баллы\",\"type\":\"number\"},{\"formula\":\"[p15]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"\"}",
                    token = commonToken,
                ))
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Итог",
                    typeStart = 0,
                    typeFinal = 1,
                    countSingle = 1,
                    formula = "{\"name\":\"Взвешенная сумма нормализованных оценок среднего арифметического баллов за 4 попытки и оценки по рубрикам\",\"params\":[{\"id\":\"p0\",\"name\":\"Баллов за 1 заезд\",\"type\":\"number\"},{\"id\":\"p1\",\"name\":\"Лучший балл 1 заезда\",\"type\":\"number\"},{\"id\":\"p2\",\"name\":\"Баллов за 2 заезд\",\"type\":\"number\"},{\"id\":\"p3\",\"name\":\"Лучший балл 2 заезда\",\"type\":\"number\"},{\"id\":\"p4\",\"name\":\"Баллов за 3 заезд\",\"type\":\"number\"},{\"id\":\"p5\",\"name\":\"Лучший балл 3 заезда\",\"type\":\"number\"},{\"id\":\"p6\",\"name\":\"Баллов за 4 заезд\",\"type\":\"number\"},{\"id\":\"p7\",\"name\":\"Лучший балл 4 заезда\",\"type\":\"number\"},{\"id\":\"p8\",\"name\":\"Баллов за TDP\",\"type\":\"number\"},{\"id\":\"p9\",\"name\":\"Лучший балл за TDP\",\"type\":\"number\"},{\"id\":\"p10\",\"name\":\"Баллов за инженерный журнал\",\"type\":\"number\"},{\"id\":\"p11\",\"name\":\"Лучший балл за инженерный журнал\",\"type\":\"number\"},{\"id\":\"p12\",\"name\":\"Баллов за постер\",\"type\":\"number\"},{\"id\":\"p13\",\"name\":\"Лучший балл за постер\",\"type\":\"number\"}],\"results\":[{\"formula\":\"0.8*(if([p3]=0,([p0]/[p1]),if([p5]=0,avg([p0]/[p1],[p2]/[p3]),if([p7]=0,avg([p0]/[p1],[p2]/[p3],[p4]/[p5]),avg([p0]/[p1],[p2]/[p3],[p4]/[p5],[p6]/[p7])))))+0.2*(0.4*([p8]/[p9])+0.4*([p10]/[p11])+0.2*([p12]/[p13]))\",\"name\":\"Итоговый балл\",\"type\":\"number\"}],\"sort\":\"0:desc\",\"disq_reasons\":\"\"}",
                    token = commonToken,
                ))
            }
        }
    }

    fun createRelayRaceProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Эстафета – это прежде всего командное соревнование. Участникам предстоить собрать двух роботов и запрограммировать их таким образом, чтобы они самостоятельно передавали друг другу эстафетную палочку в специальной зоне.<br>Время выполнения задания – 5 минут. Чем больше передач совершат роботы, тем больше шансов на победу.</p>",
            maxRobot = 2,
            checkUniqueParticipant = program.uniqueParticipant,
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Основной",
                    typeStart = 1,
                    typeFinal = 1,
                    countSingle = 2,
                    formula = "{\"name\":\"Эстафета\",\"params\":[{\"id\":\"p0\",\"name\":\"Время первой передачи\",\"type\":\"time\"},{\"id\":\"p1\",\"name\":\"Количество передач\",\"type\":\"number\"},{\"id\":\"p2\",\"name\":\"Количество запусков, учитывая первый\",\"type\":\"number\"}],\"results\":[{\"formula\":\"[p1]/[p2]\",\"name\":\"Среднее арифметическое количества передач\",\"type\":\"number\"},{\"formula\":\"[p0]\",\"name\":\"Время первой передачи\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"\"}",
                    token = commonToken,
                ))
            }
        }
    }

    fun createDronesProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Воздушные гонки - соревнования дронов. Сконструированный участником робот будет выполнять задания, не касаясь земли и воздушных препятствий. За летное время нужно набрать как можно больше баллов выполняя различные задания.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Основной",
                    typeStart = 1,
                    typeFinal = 1,
                    countSingle = 2,
                    formula = "{\"name\":\"Автономные воздушные аппараты\",\"params\":[{\"id\":\"[p0]\",\"name\":\"Старт (10)\",\"type\":\"toggle\"},{\"id\":\"[p1]\",\"name\":\"ЛЭП под верхним канатом (20)\",\"type\":\"toggle\"},{\"id\":\"[p2]\",\"name\":\"ЛЭП дополнительно над верхним канатом (30)\",\"type\":\"toggle\"},{\"id\":\"[p3]\",\"name\":\"Количество засчитанных клеток Дороги (по 20)\",\"type\":\"number\"},{\"id\":\"[p4]\",\"name\":\"Непрерывный пролет над всеми клетками Дороги (50)\",\"type\":\"toggle\"},{\"id\":\"[p5]\",\"name\":\"Посадка (50)\",\"type\":\"toggle\"},{\"id\":\"[p6]\",\"name\":\"Номер лучшего полета\",\"type\":\"number\"},{\"id\":\"[p7]\",\"name\":\"Время лучшего полета\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]*10+[p1]*20+[p2]*30+[p3]*20+[p4]*50+[p5]*50\",\"name\":\"Сумма баллов лучшего полета\",\"type\":\"number\"},{\"formula\":\"[p6]\",\"name\":\"Номер лучшего полета\",\"type\":\"number\"},{\"formula\":\"[p7]\",\"name\":\"Время лучшего полета\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc,2:asc\",\"disq_reasons\":\"\"}",
                    token = commonToken,
                ))
            }
        }
    }

    fun createFootballProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Участникам футбола управляемых роботов 3x3 предстоит проявить себя не только в конструировании роботов, но и продемонстрировать сплоченную командную работу.</p><p>На поле соревнуются 6 роботов, каждым из которых управляет участник.</p><p>Полигон&nbsp;(3х5 м) представляет собой уменьшенную копию настоящего футбольного поля. Задача каждой команды – забить наибольшее количество голов в ворота соперника.</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p>",
            maxRobot = 4,
            maxParticipant = 4,
            checkUniqueParticipant = program.uniqueParticipant,
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Основной",
                    typeStart = 1,
                    typeFinal = 0,
                    countSingle = 0,
                    challongeType = "RoundRobin",
                    token = commonToken,
                ))
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Плей-офф",
                    typeStart = 0,
                    typeFinal = 1,
                    countSingle = 0,
                    challongeType = "playOff",
                    token = commonToken,
                ))
            }
        }
    }

    fun createIntellectualSumoProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Интеллектуальное сумо — соревнование для самых стойких. Ваша задача: создать робота, который сможет вытолкнуть за пределы поля робота-соперника.&nbsp;</p>",
            maxRobot = 1,
            maxParticipant = 2,
            checkUniqueParticipant = program.uniqueParticipant,
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Регистрация",
                    typeStart = 1,
                    typeFinal = 0,
                    quota = 28,
                    challongeType = "RoundRobin",
                    token = commonToken,
                ))
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Группа А",
                    typeStart = 1,
                    typeFinal = 0,
                    challongeType = "RoundRobin",
                    token = commonToken,
                ))
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Группа Б",
                    typeStart = 1,
                    typeFinal = 0,
                    challongeType = "RoundRobin",
                    token = commonToken,
                ))
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Финальные матчи",
                    typeStart = 0,
                    typeFinal = 1,
                    countSingle = 0,
                    challongeType = "playOff",
                    formula = null,
                    token = commonToken,
                ))
            }
        }
    }

    fun createRtkProgram(eventId: Int, program: Program, maxParticipant: Int) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>«Кубок РТК» - робототехнические соревнования для наземных роботов на специальном испытательном полигоне. Робот может быть собран на любой элементной базе, без ограничений по конструкции. Управление роботом должно осуществляться по беспроводной связи кроме ИК-пультов.</p><p>Полигон представляет собой реконфигурируемую полосу препятствий, состоящую из участков различной сложности. Ячейки полигона имитируют условия урбанизированной среды, пересеченной местности и последствий катастроф, а также содержат множество разноплановых заданий. На полигоне робот может продемонстрировать:&nbsp;</p><p>&nbsp;- проходимость, преодолевая участки пересеченной местности, завалы, подъемы и спуски;&nbsp;</p><p>&nbsp;- работу манипулятора, собирая и доставляя предметы, нажимая кнопки, поворачивая краны;&nbsp;</p><p>&nbsp;- автономность, считывая QR-коды, автономно следуя по линии.&nbsp;</p><p>&nbsp;</p>",
            maxParticipant = maxParticipant,
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("video_link", "file_image_id", "content"),
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Основной",
                    typeStart = 1,
                    typeFinal = 1,
                    quota = 20,
                    countSingle = maxParticipant,
                    formula = "{\"name\":\"Баллы\",\"params\":[{\"id\":\"P0\",\"name\":\"Кол-во баллов\",\"type\":\"number\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Баллы\",\"type\":\"number\"}],\"sort\":\"0:desc\",\"disq_reasons\":\"Не выполнение регламента\"}",
                    token = commonToken,
                ))
            }
        }
    }

    fun createFiraProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Соревнования FIRA Challenge – Autonomouse Cars фокусируются на том, чтобы<br>вдохновить исследователей на разработку беспилотных транспортных средств. В<br>соревнованиях FIRA Challenge – Autonomouse Cars имеются два полигона. Первый<br>представляет из себя гоночную трассу, а второй – городскую среду. Для каждого<br>полигона разработана своя система оценки, а итоговый балл определяется как сумма<br>результатов обоих испытаний.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Основной",
                    typeStart = 1,
                    typeFinal = 1,
                    countSingle = 2,
                    formula = "{\"name\":\"Автономная гонка (трасса)\",\"params\":[{\"id\":\"Ka\",\"name\":\"коэффициент автономности( Ka = 0,5 если вычисления не на борту автомобиля, Ka = 1 если вычисления на борту автомобиля)\",\"type\":\"number\"},{\"id\":\"Tstage\",\"name\":\"время в секундах, отведенное на заезд\",\"type\":\"number\"},{\"id\":\"Ncp\",\"name\":\"общее число чекпоинтов, которые необходимо пройти\",\"type\":\"number\"},{\"id\":\"cp\",\"name\":\"количество пройденных чекпоинтов\",\"type\":\"number\"},{\"id\":\"Ttotal\",\"name\":\"время автомобиля в секундах, затраченное на заезд\",\"type\":\"number\"},{\"id\":\"Lcp\",\"name\":\"количество пропущенных чекпоинтов\",\"type\":\"number\"},{\"id\":\"Lel\",\"name\":\"количество потерянных элементов\",\"type\":\"number\"},{\"id\":\"Time\",\"name\":\"время заезда\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]*((1+if([p1]>([p4]+[p5]*0.5*([p1]/[p2])+[p6]*0.2*([p1]/[p2])),([p1]-([p4]+[p5]*0.5*([p1]/[p2])+[p6]*0.2*([p1]/[p2])))/[p1],0))*35*[p3])\",\"name\":\"Очки за трассу\",\"type\":\"number\"},{\"formula\":\"[p7]\",\"name\":\"Время заезда\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"столкновение с преградой\"}",
                    token = commonToken,
                ))
            }
        }
    }

    fun createWalkingRobotMarathonProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Участникам марафона предстоит разработать робота, который сможет преодолеть полигон шагом, бегом или прыжками. Это может быть робот-паук, а могут быть просто две ноги.<br>Время на выполнение задания – 5 минут.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Основной",
                    typeStart = 1,
                    typeFinal = 1,
                    countSingle = 3,
                    formula = "{\"name\":\"Время\",\"params\":[{\"id\":\"p0\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:asc\",\"disq_reasons\":\"\"}",
                    token = commonToken,
                ))
            }
        }
    }

    fun createArkanoidProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>В 1986 году была придумана игра «Арканоид». С тех пор в неё играют не только люди, но и роботы. Участникам состязаний предстоит сконструировать робота, который с помощью видеозрения сможет отбивать удары противника, перемещаясь по рейке. Победителем становится тот, кто забьет больше мячей в ворота соперника.</p>",
            maxParticipant = 5,
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        ))
    }

    fun createMiniSumoProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Мини-сумо — соревнование для самых стойких. Ваша задача: создать робота, который сможет вытолкнуть за пределы поля робота-соперника.&nbsp;</p><p>К участию допускаются роботы любой конструкции, размером не более 10х10 см.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Формирование групп",
                    typeStart = 1,
                    typeFinal = 0,
                    challongeType = "RoundRobin",
                    token = commonToken,
                ))
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Групповой",
                    typeStart = 0,
                    typeFinal = 0,
                    challongeType = "RoundRobin",
                    token = commonToken,
                ))
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Олимпийская",
                    typeStart = 0,
                    typeFinal = 1,
                    quota = 4,
                    challongeType = "playOff",
                    token = commonToken,
                ))
            }
        }
    }

    fun createMicroSumoProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Микро-сумо — соревнование для самых стойких. Ваша задача: создать робота, который сможет вытолкнуть за пределы поля робота-соперника.&nbsp;</p><p>К участию допускаются роботы любой конструкции, размером не более 5х5 см.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Формирование групп",
                    typeStart = 1,
                    typeFinal = 0,
                    challongeType = "RoundRobin",
                    token = commonToken,
                ))
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Групповой",
                    typeStart = 0,
                    typeFinal = 0,
                    challongeType = "RoundRobin",
                    token = commonToken,
                ))
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Олимпийская",
                    typeStart = 0,
                    typeFinal = 1,
                    quota = 4,
                    challongeType = "playOff",
                    token = commonToken,
                ))
            }
        }
    }

    fun createMazeProgram(eventId: Int, program: Program) {
        execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>В этом виде состязаний участникам необходимо подготовить автономного мобильного робота, способного наиболее быстро добраться из одного конца лабиринта в другой,&nbsp;и вернуться обратно. Конфигурация лабиринта меняется перед каждой попыткой.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        )).also {
            it?.let {
                val programCreateResponse = objectMapper.readValue(it, ProgramCreateResponse::class.java)
                val programId = programCreateResponse.data.id.toInt()
                println("Created program $programId")
                execute(StageCreateRequest(
                    programId = programId,
                    name = "Основной",
                    typeStart = 1,
                    typeFinal = 1,
                    countSingle = 1,
                    formula = "{\"name\":\"Лабиринт по времени\",\"params\":[{\"id\":\"P0\",\"name\":\"Кол-во баллов\",\"type\":\"number\"},{\"id\":\"Time\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Баллы\",\"type\":\"number\"},{\"formula\":\"[p1]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:asc,1:asc\",\"disq_reasons\":\"\"}",
                    token = commonToken,
                ))
            }
        }
    }


    fun getBids(programId: Int) {
        execute(BidsSearchRequest(programId = programId, token = commonToken))
    }

    fun createResult() {
        executeV1(ResultCreateRequest(
            stageId = 8922,
            bidId = 97178,
            number = 2,
            params = ResultParams("3", Random.nextInt(600).toDouble()),
        ))
    }

    fun eventNotificationsAdd(eventId: Int, programsToIdMap: Map<Program, Int>) {
        for ((program, programId) in programsToIdMap) {
            println("Add notification for program $programId ${program.text}")
            JudgeFiller.usersInPrograms[program]?.forEach { user ->
                println("Add notification for user $user and program $programId ${program.text}")
                execute(EventAdminNotificationAddRequest(
                    userId = user.id,
                    programId = programId,
                    eventId = eventId,
                ))
                execute(EventAdminNotificationAddAppealRequest(
                    userId = user.id,
                    programId = programId,
                    eventId = eventId,
                ))
            }
            execute(EventAdminNotificationAddAppealRequest(
                userId = User.GOLIK_ALEKSEJ_VALEREVICH.id,
                programId = programId,
                eventId = eventId,
            ))
        }
    }


    companion object {
//        private const val url_v2 = "https://demo.robofinist.ru/api/v2"

        private const val url_v2 = "https://robofinist.ru/api/v2"
        private const val url_v1 = "https://demo.robofinist.ru/api/v1/"
        private const val commonToken = "token"
        private val objectMapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        private val restTemplate = RestTemplate()

        private var requestCount = 0
        private fun execute(request: BaseRequest): String? {
            try {
                requestCount++
                println("Execute request #$requestCount")
                request.token = commonToken
                println(objectMapper.writeValueAsString(request))
                val response = restTemplate.postForEntity(url_v2, request, String::class.java)
                println(response.statusCode)
                println(response.body)
                return response.body
            } catch (e: Exception) {
                println(e.message)
                return null
            }
        }

        private fun executeV1(request: BaseRequest): String? {
            try {
                println(objectMapper.writeValueAsString(request))
                val response = restTemplate.postForEntity(url_v1 + request.url, request, String::class.java)
                println(response.statusCode)
                println(response.body)
                return response.body
            } catch (e: Exception) {
                println(e.message)
                return null
            }
        }
    }
}

fun main() {

    val eventId = 1031
    val robofinist = Robofinist()

    robofinist.addEventAdmins(eventId)

//    robofinist.createRoboraceProgram(eventId, Program.ROBORACE_PRO)
//    robofinist.createRoboraceProgram(eventId, Program.ROBORACE_PRO_MINI)
//    robofinist.createRoboraceProgram(eventId, Program.ROBORACE_OK)
//    robofinist.createRoboraceOkJrProgram(eventId, Program.ROBORACE_OK_JR)
    robofinist.createLineFollowerProgram(eventId, Program.LINE_FOLLOWER_PRO)
    robofinist.createLineFollowerProgram(eventId, Program.LINE_FOLLOWER_JR)
    robofinist.createLineFollowerProgram(eventId, Program.LINE_FOLLOWER_OK)
//    robofinist.createBigJourneyJrProgram(eventId, Program.BIG_JOURNEY_JR_OK)
//    robofinist.createBigJourneySrProgram(eventId, Program.BIG_JOURNEY_SR)
//    robofinist.createRescueLineProgram(eventId, Program.ROBOCUP_JUNIOR_RESCUE_LINE)
    robofinist.createRelayRaceProgram(eventId, Program.RELAY_RACE)
    robofinist.createDronesProgram(eventId, Program.DRONES)
    robofinist.createFootballProgram(eventId, Program.FOOTBALL_3X3)
    robofinist.createIntellectualSumoProgram(eventId, Program.INTELLECTUAL_SUMO_15X15_OK)
    robofinist.createRtkProgram(eventId, Program.RTK_CUP_SEEKER, maxParticipant = 2)
    robofinist.createRtkProgram(eventId, Program.RTK_CUP_EXTREME, maxParticipant = 5)
    robofinist.createFiraProgram(eventId, Program.FIRA)
    robofinist.createWalkingRobotMarathonProgram(eventId, Program.WALKING_ROBOT_MARATHON)
    robofinist.createArkanoidProgram(eventId, Program.ARKANOID)
//    robofinist.createMiniSumoProgram(eventId, Program.MINI_SUMO)
//    robofinist.createMicroSumoProgram(eventId, Program.MICRO_SUMO)
    robofinist.createMazeProgram(eventId, Program.MAZE)


//    robofinist.getBids(programId = 6583)
//    robofinist.createResult()

    val programsMap = robofinist.eventProgramsSearch(eventId = eventId)
    robofinist.eventNotificationsAdd(eventId = eventId, programsMap)
//    robofinist.eventProgramSearch(programId = 6576)
}
