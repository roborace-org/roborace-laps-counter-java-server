package org.roborace.lapscounter.service.robofinist

enum class Program(val text: String, val regulationId: Int, val uniqueParticipant: Int) {

    ROBORACE_PRO("Roborace. PRO", 2397, 0),
    ROBORACE_PRO_MINI("Roborace. PRO Mini", 2397, 0),
    ROBORACE_OK("Roborace. Образовательные конструкторы", 2398, 1),
    ROBORACE_OK_JR("Roborace. Образовательные конструкторы. Junior", 1727, 1),
    LINE_FOLLOWER_PRO("Следование по линии. PRO", 2388, 1),
    LINE_FOLLOWER_JR("Следование по линии. Юниоры", 2387, 1),
    LINE_FOLLOWER_OK("Следование по линии. Образовательные конструкторы", 2387, 1),
    BIG_JOURNEY_JR_OK("Большое путешествие. Младшая категория: Образовательные конструкторы", 2233, 1),
    BIG_JOURNEY_SR("Большое путешествие. Старшая категория", 2234, 1),
    ROBOCUP_JUNIOR_RESCUE_LINE("RoboCupJunior Rescue Line", 2192, 1),
    RELAY_RACE("Эстафета", 1856, 0),
    DRONES("Автономные воздушные аппараты", 1869, 1),
    FOOTBALL_3X3("Футбол управляемых роботов 3x3", 1860, 1),
    INTELLECTUAL_SUMO_15X15_OK("Интеллектуальное сумо 15х15: образовательные конструкторы", 2021, 1),
    RTK_CUP_SEEKER("Кубок РТК. Искатель", 2467, 1),
    RTK_CUP_EXTREME("Кубок РТК. Экстремал", 2468, 1),
    FIRA("FIRA Challenge - Autonomous Cars", 2495, 1),
    WALKING_ROBOT_MARATHON("Марафон шагающих роботов", 2232, 1),
    ARKANOID("Арканоид", 2000, 1),
    MINI_SUMO("Мини-сумо 10х10", 2450, 0),
    MICRO_SUMO("Микро-сумо 5х5", 2451, 0),
    MAZE("Лабиринт", 2452, 1), ;

    companion object {
        fun findByName(name: String): Program? = entries.firstOrNull {
            it.text.equals(name, ignoreCase = true)
        }.also { result -> if (result == null) println("FAILED to find program for name $name") }
    }

}