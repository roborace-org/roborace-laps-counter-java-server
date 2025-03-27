package org.roborace.lapscounter.service.robofinist

@Suppress("MagicNumber")
enum class Program(val text: String, val regulationId: Int, val uniqueParticipant: Int) {

    ROBORACE_PRO("Roborace. PRO", 3077, 0),
    ROBORACE_PRO_MINI("Roborace. PRO Mini", 3077, 0),
    ROBORACE_OK("Roborace. Образовательные конструкторы", 3078, 1),
    ROBORACE_OK_JR("Roborace. Образовательные конструкторы. Junior", 3086, 1),
    LINE_FOLLOWER_PRO("Следование по линии. PRO", 3092, 1),
    LINE_FOLLOWER_JR("Следование по линии. Юниоры", 3093, 1),
    LINE_FOLLOWER_OK("Следование по линии. Образовательные конструкторы", 3093, 1),
    BIG_JOURNEY_JR_OK("Большое путешествие. Младшая категория: Образовательные конструкторы", 3019, 1),
    BIG_JOURNEY_SR("Большое путешествие. Старшая категория", 3018, 1),
    ROBOCUP_JUNIOR_RESCUE_LINE("RoboCupJunior Rescue Line", 2192, 1),
    RELAY_RACE("Эстафета", 3007, 0),
    DRONES("Автономные воздушные аппараты", 3008, 1),
    FOOTBALL_3X3("Футбол управляемых роботов 3x3", 1860, 1),
    INTELLECTUAL_SUMO_15X15_OK("Интеллектуальное сумо 15х15: образовательные конструкторы", 3031, 1),
    RTK_CUP_SEEKER("Кубок РТК. Искатель", 2467, 1),
    RTK_CUP_EXTREME("Кубок РТК. Экстремал", 2468, 1),
    FIRA("FIRA Challenge - Autonomous Cars", 2495, 1),
    ARKANOID("Арканоид", 3020, 1),
    WALKING_ROBOT_MARATHON("Марафон шагающих роботов", 3024, 1),
    MINI_SUMO("Мини-сумо 10х10", 3217, 0),
    MICRO_SUMO("Микро-сумо 5х5", 3218, 0),
    MAZE("Лабиринт", 3216, 1), ;

    companion object {
        fun findByName(name: String): Program? = entries.firstOrNull {
            it.text.equals(name, ignoreCase = true)
        }.also { result -> if (result == null) println("FAILED to find program for name $name") }
    }

}
