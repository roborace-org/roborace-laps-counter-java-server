package org.roborace.lapscounter.service.robofinist

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.roborace.lapscounter.service.robofinist.model.BaseRequest
import org.roborace.lapscounter.service.robofinist.model.bid.BidParticipantsRequest
import org.roborace.lapscounter.service.robofinist.model.bid.BidParticipantsResponse
import org.roborace.lapscounter.service.robofinist.model.bids.BidsSearchRequest
import org.roborace.lapscounter.service.robofinist.model.bids.BidsSearchResponse
import org.roborace.lapscounter.service.robofinist.model.event.EventAdminAddRequest
import org.roborace.lapscounter.service.robofinist.model.event.EventAdminNotificationAddAppealRequest
import org.roborace.lapscounter.service.robofinist.model.event.EventAdminNotificationAddRequest
import org.roborace.lapscounter.service.robofinist.model.event.EventProgramSearchRequest
import org.roborace.lapscounter.service.robofinist.model.event.EventProgramSearchResponse
import org.roborace.lapscounter.service.robofinist.model.partner.EventPartnerAddRequest
import org.roborace.lapscounter.service.robofinist.model.partner.EventPartnerAddResponse
import org.roborace.lapscounter.service.robofinist.model.program.ProgramCreateRequest
import org.roborace.lapscounter.service.robofinist.model.program.ProgramCreateResponse
import org.roborace.lapscounter.service.robofinist.model.result.ResultCreateRequest
import org.roborace.lapscounter.service.robofinist.model.result.ResultCreateRequest.ResultParams
import org.roborace.lapscounter.service.robofinist.model.stage.StageCreateRequest
import org.roborace.lapscounter.service.robofinist.model.winners.WinnersSearchRequest
import org.roborace.lapscounter.service.robofinist.model.winners.WinnersSearchResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestTemplate
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.random.Random


//@Service
@Suppress("MagicNumber", "MaxLineLength")
class Robofinist {

    private val client = RobofinistClient()

    fun addEventAdmins(eventId: Int) {
        User.entries.forEach {
            println("Add user $it")
            client.execute(EventAdminAddRequest(eventId = eventId, userId = it.id), Map::class.java)
        }
    }

    fun eventProgramsSearch(eventId: Int): Map<String, Int> =
        client.execute(EventProgramSearchRequest(eventId = eventId), EventProgramSearchResponse::class.java)!!
            .data
            .associate { normalizeProgram(it.name) to it.id }

    private fun normalizeProgram(name: String) =
        name.trim()
            .replace("  ", "")
            .replace(",", ".")
            .replace("Pro", "PRO")
            .replace("mini", "Mini")
            .replace("образовательные", "Образовательные")
            .replace("Roborace PRO", "Roborace. PRO")
            .replace("3х3", "3x3")
            .replace("4х4", "4x4")
            .replace("Большое путешествие младшая категория", "Большое путешествие. Младшая категория")
            .replace("Большое путешествие старшая категория", "Большое путешествие. Старшая категория")
            .replace("Большое путешествие: младшая категория", "Большое путешествие. Младшая категория")
            .replace("Большое путешествие: старшая категория", "Большое путешествие. Старшая категория")
            .replace(Regex("\\s*\\(\\d региональный этап\\)"), "")
            .replace(Regex("\\.+\\s*$"), "")

    fun eventProgramSearch(programId: Int) {
        client.execute(EventProgramSearchRequest(programId = programId, showStages = true), Map::class.java)
    }

    fun createRoboraceProgram(eventId: Int, program: Program, programsMap: Map<String, Int>) {
        val id = programsMap[program.text] ?: client.execute(ProgramCreateRequest(
            eventId = eventId,
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Соревнования Roborace во многом похожи на соревнования Формулы 1, но с тем отличием, что соревнуются не управляемые пилотами болиды, а полностью автономные роботы. Роботы полагаются на показания своих датчиков, чтобы ориентироваться по трассе (ограниченной бортами), маневрировать, выбирать скорость движения и избегать столкновений с соперниками.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
        ), ProgramCreateResponse::class.java)
            ?.data?.id?.toInt()
        id?.let { createRREtaps(it) }
    }

    private fun createRREtaps(programId: Int) {
        println("Created program $programId")
        client.execute(StageCreateRequest(
            programId = programId,
            name = "Квалификация",
            typeStart = 1,
            typeFinal = 0,
            countSingle = 3,
            formula = "{\"name\":\"Время круга\",\"params\":[{\"id\":\"Time\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:asc\",\"disq_reasons\":\"Не уложился в заданное время круга\"}",
        ), Map::class.java)
        client.execute(StageCreateRequest(
            programId = programId,
            name = "Отборочный заезд",
            typeStart = 0,
            typeFinal = 0,
            countSingle = 1,
            formula = "{\"name\":\"Круги\",\"params\":[{\"id\":\"Laps\",\"name\":\"Кол-во кругов\",\"type\":\"number\"},{\"id\":\"Time\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Круги\",\"type\":\"number\"},{\"formula\":\"[p1]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"Не уложился в заданное время круга\"}",
        ), Map::class.java)
        client.execute(StageCreateRequest(
            programId = programId,
            name = "Финальный заезд",
            typeStart = 0,
            typeFinal = 1,
            countSingle = 1,
            formula = "{\"name\":\"Круги\",\"params\":[{\"id\":\"Laps\",\"name\":\"Кол-во кругов\",\"type\":\"number\"},{\"id\":\"Time\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Круги\",\"type\":\"number\"},{\"formula\":\"[p1]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"Не уложился в заданное время круга\"}",
        ), Map::class.java)
    }

    fun createRoboraceOkJrProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Соревнования Roborace во многом похожи на соревнования Формулы 1, но с тем отличием, что соревнуются не управляемые пилотами болиды, а полностью автономные роботы. Роботы полагаются на показания своих датчиков, чтобы ориентироваться по трассе (ограниченной бортами), маневрировать, выбирать скорость движения и избегать столкновений с соперниками.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        ), ProgramCreateResponse::class.java)?.let {
            val programId = it.data.id.toInt()
            println("Created program $programId")
            client.execute(StageCreateRequest(
                programId = programId,
                name = "Квалификация",
                typeStart = 1,
                typeFinal = 0,
                countSingle = 3,
                formula = "{\"name\":\"Круг по времени\",\"params\":[{\"id\":\"p0\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:asc\",\"disq_reasons\":\"Не уложился в заданное время\"}",
            ), Map::class.java)
            client.execute(StageCreateRequest(
                programId = programId,
                name = "Финальный",
                typeStart = 0,
                typeFinal = 1,
                countSingle = 0,
                challongeType = "playOff",
            ), Map::class.java)
            Unit
        }

    }

    fun createLineFollowerProgram(eventId: Int, program: Program, checkUniqueParticipant: Int = 1) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Задачей для робота в этом виде является преодоление трассы вдоль черной линии за наименьшее время. Робот должен ехать по черной линии в автоматическом режиме.</p>",
            checkUniqueParticipant = checkUniqueParticipant,
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            ?.let {
                val programId = it.data.id.toInt()
                println("Created program $programId")
                client.execute(StageCreateRequest(
                    programId = programId,
                    name = "Основной",
                    typeStart = 1,
                    typeFinal = 1,
                    countSingle = 3,
                    formula = "{\"name\":\"Время\",\"params\":[{\"id\":\"p0\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:asc\",\"disq_reasons\":\"\"}",

                    ), Map::class.java)
            }
    }


    fun createBigJourneyJrProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>«Большое путешествие» — это дисциплина, составленная из нескольких классических упражнений, которые робот должен выполнить последовательно и без остановки.</p><p>За три минут роботу предстоит: проехать по линии, обогнув препятствие, преодолеть лабиринт, линию с горкой, а также выбить все банки в кегельринге.<br>Побеждает тот, чей робот набрал наибольшее количество баллов при прохождении трассы.&nbsp;</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Основной",
                        typeStart = 1,
                        typeFinal = 1,
                        countSingle = 2,
                        formula = "{\"name\":\"Большое путешествие младшая\",\"params\":[{\"id\":\"p0\",\"name\":\"Следование по линии с препятствием (max 40)\",\"type\":\"number\"},{\"id\":\"p1\",\"name\":\"Лабиринт (max 80)\",\"type\":\"number\"},{\"id\":\"p2\",\"name\":\"Следование по линии c горкой (max 40)\",\"type\":\"number\"},{\"id\":\"p3\",\"name\":\"Вытолкнуто кегель\",\"type\":\"number\"},{\"id\":\"p4\",\"name\":\"Время (повтор - 03:00:00)\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]+[p1]+[p2]+[p3]*5\",\"name\":\"Баллы\",\"type\":\"number\"},{\"formula\":\"[p4]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"\"}",
                    ), Map::class.java)
                }
            }
    }

    fun createBigJourneySrProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>«Большое путешествие» — это дисциплина, составленная из нескольких классических упражнений, которые робот должен выполнить последовательно и без остановки.</p><p>За пять минут роботу предстоит: проехать по линии, обогнув движущееся препятствие, преодолеть лабиринт, линию с инверсией, а также выбить все банки в кегельринге, кроме одной. Оставшуюся банку необходимо вернуть на старт, пройдя все препятствия в обратном порядке.</p><p>Побеждает тот, чей робот набрал наибольшее количество баллов при прохождении трассы.&nbsp;</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Основной",
                        typeStart = 1,
                        typeFinal = 1,
                        countSingle = 2,
                        formula = "{\"name\":\"Большое путешествие старшая\",\"params\":[{\"id\":\"p0\",\"name\":\"Следование по линии с движущимся препятствием (max 40)\",\"type\":\"number\"},{\"id\":\"p1\",\"name\":\"Лабиринт (max 80)\",\"type\":\"number\"},{\"id\":\"p2\",\"name\":\"Следование по инверсной линии (max 40)\",\"type\":\"number\"},{\"id\":\"p3\",\"name\":\"Вытолкнуто кегель (до 8)\",\"type\":\"number\"},{\"id\":\"p4\",\"name\":\"Кегельринг (max 40)\",\"type\":\"number\"},{\"id\":\"p5\",\"name\":\"Следование по инверсной линии обратно (max 40)\",\"type\":\"number\"},{\"id\":\"p6\",\"name\":\"Лабиринт обратно (max 80)\",\"type\":\"number\"},{\"id\":\"p7\",\"name\":\"Следование по линии с движущимся препятствием обратно (max 40)\",\"type\":\"number\"},{\"id\":\"p8\",\"name\":\"Время (повтор - 05:00:00)\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]+[p1]+[p2]+[p3]*5+[p4]+[p5]+[p6]+[p7]\",\"name\":\"Баллы\",\"type\":\"number\"},{\"formula\":\"[p8]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"\"}",

                        ), Map::class.java)
                }
            }
    }

    fun createRescueLineProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Участниками RoboСupJunior Rescue Line становятся самые смелые, ведь им предстоит сконструировать робота, который должен самостоятельно выполнить спасательную миссию. Роботу предстоит двигаться по линии через разрушенные препятствия, возвышенности, неровности, чтобы забрать пострадавших и вернуть их на базу, где им будет оказана помощь.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Попытки",
                        typeStart = 1,
                        typeFinal = 0,
                        countSingle = 2,
                        formula = "{\"name\":\"Rescue Line (по параметрам)\",\"params\":[{\"id\":\"p0\",\"name\":\"Преодоленных промежутков\",\"type\":\"number\"},{\"id\":\"p1\",\"name\":\"Преодоленных барьеров\",\"type\":\"number\"},{\"id\":\"p2\",\"name\":\"Преодоленных перекрестков/тупиков\",\"type\":\"number\"},{\"id\":\"p3\",\"name\":\"Преодоленных рамп\",\"type\":\"number\"},{\"id\":\"p4\",\"name\":\"Преодоленных препятствий\",\"type\":\"number\"},{\"id\":\"p5\",\"name\":\"Преодоленных качелей\",\"type\":\"number\"},{\"id\":\"p6\",\"name\":\"Пройдено клеток до контрольной точки с 1 попытки\",\"type\":\"number\"},{\"id\":\"p7\",\"name\":\"Пройдено клеток до контрольной точки со 2 попытки\",\"type\":\"number\"},{\"id\":\"p8\",\"name\":\"Пройдено клеток до контрольной точки с 3 попытки\",\"type\":\"number\"},{\"id\":\"p9\",\"name\":\"Спасено живых и мертвых жертв в соответствующие точки эвакуации - живые в зеленую, мертвая в красную (мертвые жертвы учитываются в случае спасения всех живых)\",\"type\":\"number\"},{\"id\":\"p10\",\"name\":\"Уровень пункта эвакуации (1 или 2)\",\"type\":\"number\"},{\"id\":\"p11\",\"name\":\"Спасательный комплект доставлен в пункт эвакуации со старта (1) после подбора (2)\",\"type\":\"number\"},{\"id\":\"p12\",\"name\":\"Отсутствий прогресса в зоне эвакуации (на участке между контрольными точками с зоной эвакуации)\",\"type\":\"number\"},{\"id\":\"p13\",\"name\":\"Всего отсутствий прогресса\",\"type\":\"number\"},{\"id\":\"p14\",\"name\":\"Достиг финишной клетки и остановился на 5 секунд\",\"type\":\"toggle\"},{\"id\":\"p15\",\"name\":\"Время заезда (max 08:00.00)\",\"type\":\"time\"}],\"results\":[{\"formula\":\"(([p0]*10+[p1]*5+[p2]*10+[p3]*10+[p4]*15+[p5]*15)+([p6]*5+[p7]*3+[p8]*1)+([p14]*if([p13]<12,(60-5*[p13]),0)))*(((if((1+0.2*[p10])-0.025*[p12]*[p10]>1,(1+0.2*[p10])-0.025*[p12]*[p10],1)^[p9]))*(1+([p10]*(([p11]^2-[p11])/2+[p11]))/10))\",\"name\":\"Баллы\",\"type\":\"number\"},{\"formula\":\"[p15]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"\"}",

                        ), Map::class.java)
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Итог",
                        typeStart = 0,
                        typeFinal = 1,
                        countSingle = 1,
                        formula = "{\"name\":\"Взвешенная сумма нормализованных оценок среднего арифметического баллов за 4 попытки и оценки по рубрикам\",\"params\":[{\"id\":\"p0\",\"name\":\"Баллов за 1 заезд\",\"type\":\"number\"},{\"id\":\"p1\",\"name\":\"Лучший балл 1 заезда\",\"type\":\"number\"},{\"id\":\"p2\",\"name\":\"Баллов за 2 заезд\",\"type\":\"number\"},{\"id\":\"p3\",\"name\":\"Лучший балл 2 заезда\",\"type\":\"number\"},{\"id\":\"p4\",\"name\":\"Баллов за 3 заезд\",\"type\":\"number\"},{\"id\":\"p5\",\"name\":\"Лучший балл 3 заезда\",\"type\":\"number\"},{\"id\":\"p6\",\"name\":\"Баллов за 4 заезд\",\"type\":\"number\"},{\"id\":\"p7\",\"name\":\"Лучший балл 4 заезда\",\"type\":\"number\"},{\"id\":\"p8\",\"name\":\"Баллов за TDP\",\"type\":\"number\"},{\"id\":\"p9\",\"name\":\"Лучший балл за TDP\",\"type\":\"number\"},{\"id\":\"p10\",\"name\":\"Баллов за инженерный журнал\",\"type\":\"number\"},{\"id\":\"p11\",\"name\":\"Лучший балл за инженерный журнал\",\"type\":\"number\"},{\"id\":\"p12\",\"name\":\"Баллов за постер\",\"type\":\"number\"},{\"id\":\"p13\",\"name\":\"Лучший балл за постер\",\"type\":\"number\"}],\"results\":[{\"formula\":\"0.8*(if([p3]=0,([p0]/[p1]),if([p5]=0,avg([p0]/[p1],[p2]/[p3]),if([p7]=0,avg([p0]/[p1],[p2]/[p3],[p4]/[p5]),avg([p0]/[p1],[p2]/[p3],[p4]/[p5],[p6]/[p7])))))+0.2*(0.4*([p8]/[p9])+0.4*([p10]/[p11])+0.2*([p12]/[p13]))\",\"name\":\"Итоговый балл\",\"type\":\"number\"}],\"sort\":\"0:desc\",\"disq_reasons\":\"\"}",

                        ), Map::class.java)
                }
            }
    }

    fun createRelayRaceProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Эстафета – это прежде всего командное соревнование. Участникам предстоить собрать двух роботов и запрограммировать их таким образом, чтобы они самостоятельно передавали друг другу эстафетную палочку в специальной зоне.<br>Время выполнения задания – 5 минут. Чем больше передач совершат роботы, тем больше шансов на победу.</p>",
            maxRobot = 2,
            checkUniqueParticipant = program.uniqueParticipant,
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Основной",
                        typeStart = 1,
                        typeFinal = 1,
                        countSingle = 2,
                        formula = "{\"name\":\"Эстафета\",\"params\":[{\"id\":\"p0\",\"name\":\"Время первой передачи\",\"type\":\"time\"},{\"id\":\"p1\",\"name\":\"Количество передач\",\"type\":\"number\"},{\"id\":\"p2\",\"name\":\"Количество запусков, учитывая первый\",\"type\":\"number\"}],\"results\":[{\"formula\":\"[p1]/[p2]\",\"name\":\"Среднее арифметическое количества передач\",\"type\":\"number\"},{\"formula\":\"[p0]\",\"name\":\"Время первой передачи\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"\"}",

                        ), Map::class.java)
                }
            }
    }

    fun createDronesProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Воздушные гонки - соревнования дронов. Сконструированный участником робот будет выполнять задания, не касаясь земли и воздушных препятствий. За летное время нужно набрать как можно больше баллов выполняя различные задания.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Основной",
                        typeStart = 1,
                        typeFinal = 1,
                        countSingle = 2,
                        formula = "{\"name\":\"Автономные воздушные аппараты\",\"params\":[{\"id\":\"[p0]\",\"name\":\"Старт (10)\",\"type\":\"toggle\"},{\"id\":\"[p1]\",\"name\":\"ЛЭП под верхним канатом (20)\",\"type\":\"toggle\"},{\"id\":\"[p2]\",\"name\":\"ЛЭП дополнительно над верхним канатом (30)\",\"type\":\"toggle\"},{\"id\":\"[p3]\",\"name\":\"Количество засчитанных клеток Дороги (по 20)\",\"type\":\"number\"},{\"id\":\"[p4]\",\"name\":\"Непрерывный пролет над всеми клетками Дороги (50)\",\"type\":\"toggle\"},{\"id\":\"[p5]\",\"name\":\"Посадка (50)\",\"type\":\"toggle\"},{\"id\":\"[p6]\",\"name\":\"Номер лучшего полета\",\"type\":\"number\"},{\"id\":\"[p7]\",\"name\":\"Время лучшего полета\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]*10+[p1]*20+[p2]*30+[p3]*20+[p4]*50+[p5]*50\",\"name\":\"Сумма баллов лучшего полета\",\"type\":\"number\"},{\"formula\":\"[p6]\",\"name\":\"Номер лучшего полета\",\"type\":\"number\"},{\"formula\":\"[p7]\",\"name\":\"Время лучшего полета\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc,2:asc\",\"disq_reasons\":\"\"}",

                        ), Map::class.java)
                }
            }
    }

    fun createFootballProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Участникам футбола управляемых роботов 3x3 предстоит проявить себя не только в конструировании роботов, но и продемонстрировать сплоченную командную работу.</p><p>На поле соревнуются 6 роботов, каждым из которых управляет участник.</p><p>Полигон&nbsp;(3х5 м) представляет собой уменьшенную копию настоящего футбольного поля. Задача каждой команды – забить наибольшее количество голов в ворота соперника.</p><p>&nbsp;</p><p>&nbsp;</p><p>&nbsp;</p>",
            maxRobot = 4,
            maxParticipant = 4,
            checkUniqueParticipant = program.uniqueParticipant,
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Основной",
                        typeStart = 1,
                        typeFinal = 0,
                        countSingle = 0,
                        challongeType = "RoundRobin",

                        ), Map::class.java)
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Плей-офф",
                        typeStart = 0,
                        typeFinal = 1,
                        countSingle = 0,
                        challongeType = "playOff",

                        ), Map::class.java)
                }
            }
    }

    fun createIntellectualSumoProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Интеллектуальное сумо — соревнование для самых стойких. Ваша задача: создать робота, который сможет вытолкнуть за пределы поля робота-соперника.&nbsp;</p>",
            maxRobot = 1,
            maxParticipant = 2,
            checkUniqueParticipant = program.uniqueParticipant,
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Регистрация",
                        typeStart = 1,
                        typeFinal = 0,
                        quota = 28,
                        challongeType = "RoundRobin",

                        ), Map::class.java)
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Группа А",
                        typeStart = 0,
                        typeFinal = 0,
                        challongeType = "RoundRobin",

                        ), Map::class.java)
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Группа Б",
                        typeStart = 0,
                        typeFinal = 0,
                        challongeType = "RoundRobin",

                        ), Map::class.java)
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Финальные матчи",
                        typeStart = 0,
                        typeFinal = 1,
                        countSingle = 0,
                        challongeType = "playOff",
                        formula = null,

                        ), Map::class.java)
                }
            }
    }

    fun createRtkProgram(eventId: Int, program: Program, maxParticipant: Int) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>«Кубок РТК» - робототехнические соревнования для наземных роботов на специальном испытательном полигоне. Робот может быть собран на любой элементной базе, без ограничений по конструкции. Управление роботом должно осуществляться по беспроводной связи кроме ИК-пультов.</p><p>Полигон представляет собой реконфигурируемую полосу препятствий, состоящую из участков различной сложности. Ячейки полигона имитируют условия урбанизированной среды, пересеченной местности и последствий катастроф, а также содержат множество разноплановых заданий. На полигоне робот может продемонстрировать:&nbsp;</p><p>&nbsp;- проходимость, преодолевая участки пересеченной местности, завалы, подъемы и спуски;&nbsp;</p><p>&nbsp;- работу манипулятора, собирая и доставляя предметы, нажимая кнопки, поворачивая краны;&nbsp;</p><p>&nbsp;- автономность, считывая QR-коды, автономно следуя по линии.&nbsp;</p><p>&nbsp;</p>",
            maxParticipant = maxParticipant,
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("video_link", "file_image_id", "content"),
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Основной",
                        typeStart = 1,
                        typeFinal = 1,
                        quota = 20,
                        countSingle = maxParticipant,
                        formula = "{\"name\":\"Баллы\",\"params\":[{\"id\":\"P0\",\"name\":\"Кол-во баллов\",\"type\":\"number\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Баллы\",\"type\":\"number\"}],\"sort\":\"0:desc\",\"disq_reasons\":\"Не выполнение регламента\"}",

                        ), Map::class.java)
                }
            }
    }

    fun createFiraProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Соревнования FIRA Challenge – Autonomouse Cars фокусируются на том, чтобы<br>вдохновить исследователей на разработку беспилотных транспортных средств. В<br>соревнованиях FIRA Challenge – Autonomouse Cars имеются два полигона. Первый<br>представляет из себя гоночную трассу, а второй – городскую среду. Для каждого<br>полигона разработана своя система оценки, а итоговый балл определяется как сумма<br>результатов обоих испытаний.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Основной",
                        typeStart = 1,
                        typeFinal = 1,
                        countSingle = 2,
                        formula = "{\"name\":\"Автономная гонка (трасса)\",\"params\":[{\"id\":\"Ka\",\"name\":\"коэффициент автономности( Ka = 0,5 если вычисления не на борту автомобиля, Ka = 1 если вычисления на борту автомобиля)\",\"type\":\"number\"},{\"id\":\"Tstage\",\"name\":\"время в секундах, отведенное на заезд\",\"type\":\"number\"},{\"id\":\"Ncp\",\"name\":\"общее число чекпоинтов, которые необходимо пройти\",\"type\":\"number\"},{\"id\":\"cp\",\"name\":\"количество пройденных чекпоинтов\",\"type\":\"number\"},{\"id\":\"Ttotal\",\"name\":\"время автомобиля в секундах, затраченное на заезд\",\"type\":\"number\"},{\"id\":\"Lcp\",\"name\":\"количество пропущенных чекпоинтов\",\"type\":\"number\"},{\"id\":\"Lel\",\"name\":\"количество потерянных элементов\",\"type\":\"number\"},{\"id\":\"Time\",\"name\":\"время заезда\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]*((1+if([p1]>([p4]+[p5]*0.5*([p1]/[p2])+[p6]*0.2*([p1]/[p2])),([p1]-([p4]+[p5]*0.5*([p1]/[p2])+[p6]*0.2*([p1]/[p2])))/[p1],0))*35*[p3])\",\"name\":\"Очки за трассу\",\"type\":\"number\"},{\"formula\":\"[p7]\",\"name\":\"Время заезда\",\"type\":\"time\"}],\"sort\":\"0:desc,1:asc\",\"disq_reasons\":\"столкновение с преградой\"}",

                        ), Map::class.java)
                }
            }
    }

    fun createWalkingRobotMarathonProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Участникам марафона предстоит разработать робота, который сможет преодолеть полигон шагом, бегом или прыжками. Это может быть робот-паук, а могут быть просто две ноги.<br>Время на выполнение задания – 5 минут.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Основной",
                        typeStart = 1,
                        typeFinal = 1,
                        countSingle = 3,
                        formula = "{\"name\":\"Время\",\"params\":[{\"id\":\"p0\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:asc\",\"disq_reasons\":\"\"}",

                        ), Map::class.java)
                }
            }
    }

    fun createArkanoidProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>В 1986 году была придумана игра «Арканоид». С тех пор в неё играют не только люди, но и роботы. Участникам состязаний предстоит сконструировать робота, который с помощью видеозрения сможет отбивать удары противника, перемещаясь по рейке. Победителем становится тот, кто забьет больше мячей в ворота соперника.</p>",
            maxParticipant = 5,
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        ), ProgramCreateResponse::class.java)?.let {
            val programId = it.data.id.toInt()
            println("Created program arkanoid $programId")
            client.execute(StageCreateRequest(
                programId = programId,
                name = "Регистрационный",
                typeStart = 1,
                typeFinal = 0,


            ), Map::class.java)
            client.execute(StageCreateRequest(
                programId = programId,
                name = "Финальный",
                typeStart = 0,
                typeFinal = 1,
                countSingle = 0,
                challongeType = "RoundRobin",
            ), Map::class.java)
            Unit
        }
    }

    fun createMiniSumoProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Мини-сумо — соревнование для самых стойких. Ваша задача: создать робота, который сможет вытолкнуть за пределы поля робота-соперника.&nbsp;</p><p>К участию допускаются роботы любой конструкции, размером не более 10х10 см.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Формирование групп",
                        typeStart = 1,
                        typeFinal = 0,
                        challongeType = "RoundRobin",

                        ), Map::class.java)
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Групповой",
                        typeStart = 0,
                        typeFinal = 0,
                        challongeType = "RoundRobin",

                        ), Map::class.java)
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Олимпийская",
                        typeStart = 0,
                        typeFinal = 1,
                        quota = 4,
                        challongeType = "playOff",

                        ), Map::class.java)
                }
            }
    }

    fun createMicroSumoProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Микро-сумо — соревнование для самых стойких. Ваша задача: создать робота, который сможет вытолкнуть за пределы поля робота-соперника.&nbsp;</p><p>К участию допускаются роботы любой конструкции, размером не более 5х5 см.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Формирование групп",
                        typeStart = 1,
                        typeFinal = 0,
                        challongeType = "RoundRobin",

                        ), Map::class.java)
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Групповой",
                        typeStart = 0,
                        typeFinal = 0,
                        challongeType = "RoundRobin",

                        ), Map::class.java)
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Олимпийская",
                        typeStart = 0,
                        typeFinal = 1,
                        quota = 4,
                        challongeType = "playOff",

                        ), Map::class.java)
                }
            }
    }

    fun createMazeProgram(eventId: Int, program: Program) {
        client.execute(ProgramCreateRequest(
            regulationId = program.regulationId,
            sort = program.ordinal,
            name = program.text,
            description = "<p>Цель робота - добраться из одного конца лабиринта в другой за минимальное время. Каждому участнику дается 5 минут для совершения попыток. Конфигурация лабиринта заранее неизвестна.</p>",
            checkUniqueParticipant = program.uniqueParticipant,
            arRequiredRobotsFields = listOf("file_image_id", "content"),
            eventId = eventId,
        ), ProgramCreateResponse::class.java)
            .also {
                it?.let {
                    val programId = it.data.id.toInt()
                    println("Created program $programId")
                    client.execute(StageCreateRequest(
                        programId = programId,
                        name = "Основной",
                        typeStart = 1,
                        typeFinal = 1,
                        countSingle = 1,
                        formula = "{\"name\":\"Лабиринт по времени\",\"params\":[{\"id\":\"P0\",\"name\":\"Кол-во баллов\",\"type\":\"number\"},{\"id\":\"Time\",\"name\":\"Время\",\"type\":\"time\"}],\"results\":[{\"formula\":\"[p0]\",\"name\":\"Баллы\",\"type\":\"number\"},{\"formula\":\"[p1]\",\"name\":\"Время\",\"type\":\"time\"}],\"sort\":\"0:asc,1:asc\",\"disq_reasons\":\"\"}",
                    ), Map::class.java)
                }
            }
    }


    fun getBids(programId: Int): BidsSearchResponse? =
        client.execute(BidsSearchRequest(programId = programId), BidsSearchResponse::class.java)

    fun createPartners(eventId: Int) {
        Partner.entries.forEach {
            createPartner(eventId, it.id)
        }
    }

    private fun createPartner(eventId: Int, orgId: Int): EventPartnerAddResponse? =
        client.execute(EventPartnerAddRequest(eventId = eventId, orgId = orgId), EventPartnerAddResponse::class.java)

    private val participantsMap = mutableMapOf<Int, BidParticipantsResponse?>()

    fun getBidParticipants(bidId: Int): BidParticipantsResponse? =
//        participantsMap[bidId]
//            ?:
            client.execute(BidParticipantsRequest(bidId = bidId), BidParticipantsResponse::class.java)
//                .also {
//                    participantsMap[bidId] = it }


    fun getWinners(programId: Int): WinnersSearchResponse? =
        client.execute(WinnersSearchRequest(programId = programId), WinnersSearchResponse::class.java)

    fun createResult() {
        client.executeV1(ResultCreateRequest(
            stageId = 8922,
            bidId = 97178,
            number = 2,
            params = ResultParams("3", Random.nextInt(600).toDouble()),
        ))
    }

    fun eventNotificationsAdd(eventId: Int, programsToIdMap: Map<String, Int>) {
        for ((programName, programId) in programsToIdMap) {
            val program = Program.findByName(programName)
            println("Add notification for program $programId $programName")
            JudgeFiller.usersInPrograms[program]?.forEach { user ->
                println("Add notification for user $user and program $programId $programName")
                client.execute(EventAdminNotificationAddRequest(
                    userId = user.id,
                    programId = programId,
                    eventId = eventId,
                ), Map::class.java)
                client.execute(EventAdminNotificationAddAppealRequest(
                    userId = user.id,
                    programId = programId,
                    eventId = eventId,
                ), Map::class.java)
            }
            client.execute(EventAdminNotificationAddAppealRequest(
                userId = User.GOLIK_ALEKSEJ_VALEREVICH.id,
                programId = programId,
                eventId = eventId,
            ), Map::class.java)
        }
    }

    fun setEventMapPoint(eventId: Int) {
        val req = """{"__url": "event/edit", "event_id": $eventId, "onMap": "53.89681215, 27.566126014032122", "token":"${RobofinistClient.commonToken}"}"""
        client.executeString(req)
    }


}


class LoggingRequestInterceptor : ClientHttpRequestInterceptor {
    @Throws(IOException::class)
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        traceRequest(request, body)
        val response: ClientHttpResponse = execution.execute(request, body)
        traceResponse(response)
        return response
    }

    @Throws(IOException::class)
    private fun traceRequest(request: HttpRequest, body: ByteArray) {
        log.info("===========================request begin================================================")
        log.info("URI         : {}", request.getURI())
        log.info("Method      : {}", request.getMethod())
        log.info("Headers     : {}", request.getHeaders())
        log.info("Request body: {}", String(body, charset("UTF-8")))
        log.info("==========================request end================================================")
    }

    @Throws(IOException::class)
    private fun traceResponse(response: ClientHttpResponse) {
        val inputStringBuilder = StringBuilder()
        val bufferedReader = BufferedReader(InputStreamReader(response.getBody(), "UTF-8"))
        var line = bufferedReader.readLine()
        while (line != null) {
            inputStringBuilder.append(line)
            inputStringBuilder.append('\n')
            line = bufferedReader.readLine()
        }
        log.info("============================response begin==========================================")
        log.info("Status code  : {}", response.getStatusCode())
        log.info("Status text  : {}", response.getStatusText())
        log.info("Headers      : {}", response.getHeaders())
        log.info("Response body: {}", inputStringBuilder.toString())
        log.info("=======================response end=================================================")
    }

    companion object {
        val log = LoggerFactory.getLogger(LoggingRequestInterceptor::class.java)
    }
}

class RobofinistClient {
    private val restTemplate = RestTemplate()

    //        .apply { interceptors.add(LoggingRequestInterceptor()) }
    private val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)


    private var requestCount = 0
    fun executeString(request: String): String? {
        try {
            requestCount++
            println("Execute request #$requestCount $url_v2")
            println(request)
            val response = restTemplate.postForEntity(url_v2, request, String::class.java)
            println(response.statusCode)
            println(response.body)
            return response.body
        } catch (e: Exception) {
            println(e.message)
            return null
        }
    }
    fun <T> execute(request: BaseRequest, outClass: Class<T>): T? {
        try {
            requestCount++
            println("Execute request #$requestCount $url_v2")
            request.token = commonToken
            println(objectMapper.writeValueAsString(request))
            val response = restTemplate.postForEntity(url_v2, request, String::class.java)
            println(response.statusCode)
            println(response.body)
            return objectMapper.readValue(response.body, outClass)
        } catch (e: Exception) {
            println(e.message)
            return null
        }
    }

    fun executeV1(request: BaseRequest): String? {
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

    companion object {
//            private const val url_v2 = "https://robofinist.by/api/v2"

        private const val url_v2 = "https://robofinist.ru/api/v2"
        private const val url_v1 = "https://robofinist.ru/api/v1/"
        const val commonToken = "token"

    }
}

@Suppress("MagicNumber")
fun main() {

    val eventId = 1349
    val robofinist = Robofinist()

    robofinist.setEventMapPoint(eventId)
    robofinist.createPartners(eventId)
    var programsMap = robofinist.eventProgramsSearch(eventId = eventId)

    robofinist.createRoboraceProgram(eventId, Program.ROBORACE_PRO, programsMap)
    robofinist.createRoboraceProgram(eventId, Program.ROBORACE_PRO_MINI, programsMap)
    robofinist.createRoboraceProgram(eventId, Program.ROBORACE_OK, programsMap)
    robofinist.createRoboraceOkJrProgram(eventId, Program.ROBORACE_OK_JR)
    robofinist.createLineFollowerProgram(eventId, Program.LINE_FOLLOWER_PRO)
    robofinist.createLineFollowerProgram(eventId, Program.LINE_FOLLOWER_JR)
    robofinist.createLineFollowerProgram(eventId, Program.LINE_FOLLOWER_OK)
    robofinist.createBigJourneyJrProgram(eventId, Program.BIG_JOURNEY_JR_OK)
    robofinist.createBigJourneySrProgram(eventId, Program.BIG_JOURNEY_SR)
    robofinist.createRescueLineProgram(eventId, Program.ROBOCUP_JUNIOR_RESCUE_LINE)
    robofinist.createRelayRaceProgram(eventId, Program.RELAY_RACE)
    robofinist.createDronesProgram(eventId, Program.DRONES)
    robofinist.createFootballProgram(eventId, Program.FOOTBALL_3X3)
    robofinist.createIntellectualSumoProgram(eventId, Program.INTELLECTUAL_SUMO_15X15_OK)
    robofinist.createRtkProgram(eventId, Program.RTK_CUP_SEEKER, maxParticipant = 2)
    robofinist.createRtkProgram(eventId, Program.RTK_CUP_EXTREME, maxParticipant = 5)
    robofinist.createFiraProgram(eventId, Program.FIRA)
    robofinist.createArkanoidProgram(eventId, Program.ARKANOID)
    robofinist.createWalkingRobotMarathonProgram(eventId, Program.WALKING_ROBOT_MARATHON)
    robofinist.createMiniSumoProgram(eventId, Program.MINI_SUMO)
    robofinist.createMicroSumoProgram(eventId, Program.MICRO_SUMO)
    robofinist.createMazeProgram(eventId, Program.MAZE)


    robofinist.addEventAdmins(eventId)
    programsMap = robofinist.eventProgramsSearch(eventId = eventId)
    robofinist.eventNotificationsAdd(eventId = eventId, programsMap)

}
