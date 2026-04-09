package org.roborace.lapscounter.service.robofinist

enum class Partner(val id: Int, val orgName: String, val type: Int = 3) {
//    HOLIK(44303, "Частное унитарное предприятие «Соревнования по робототехнике»", type = 1),
    SMARTBREST(43388, "Научно-инновационное учреждение \"Умный Брест\""),
    FINIST(1, "Благотворительный фонд \"ФИНИСТ\""),
    ITEEN(42742, "ITeen Academy Образовательный центр программирования и высоких технологий"),
    LEARN(44328, "Репетиторский центр \"Учись - и точка\""),
    ITSCHOOL(42582, "ООО \"АйТи Скул\""),
    AKSIOMA(42389, "Клуб робототехники \"Аксиома\""),
    VECTOR(43948, "Клуб робототехники, электроники и программирования VECTOR"),
    IMPULS(42694, "Клуб робототехники \"Импульс\""),
    CYBERLAB(43952, "CyberLab"),
    KODWARS(44698, "IT-Клуб робототехники и программирования \"Кодвартс\""),
    NEXTLEVEL(2090, "Учебный центр NEXT LEVEL"),
    PINMODE(2364, "Клуб технического творчества \"pinMode\""),
    ROBOCLEVER(42579, "STEM-класс RoboClever"),
    ROBOLIFE(44271, "Общество с ограниченной ответственностью \"РОБО ЛАЙФ\""),
    ROBOTRON(44348, "Клуб робототехники \"Роботрон\""),
    ;
    companion object {
        val partnerNames = entries.map { it.orgName }.toSet()
    }
}
