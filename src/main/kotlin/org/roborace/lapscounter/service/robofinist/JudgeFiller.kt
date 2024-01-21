package org.roborace.lapscounter.service.robofinist

import org.roborace.lapscounter.service.robofinist.User.*

object JudgeFiller {
     val usersInPrograms = mapOf(
        Program.ROBORACE_PRO to setOf(
            OHRIMCHUK_VALERIJ,
            SMIRNOV_EVGENIJ_ANDREEVICH,
            ZHUKOVSKIJ_NIKITA_EVGENEVICH,
            KOVALEV_ALEKSANDR_NIKOLAEVICH,
            DUBATOVKA_VLADISLAV_VITALEVICH,
        ),
        Program.ROBORACE_PRO_MINI to setOf(
            OHRIMCHUK_VALERIJ,
            SMIRNOV_EVGENIJ_ANDREEVICH,
            ZHUKOVSKIJ_NIKITA_EVGENEVICH,
            KOVALEV_ALEKSANDR_NIKOLAEVICH,
            DUBATOVKA_VLADISLAV_VITALEVICH,
        ),
        Program.ROBORACE_OK to setOf(
            OHRIMCHUK_VALERIJ,
            SMIRNOV_EVGENIJ_ANDREEVICH,
            ZHUKOVSKIJ_NIKITA_EVGENEVICH,
            KOVALEV_ALEKSANDR_NIKOLAEVICH,
            DUBATOVKA_VLADISLAV_VITALEVICH,
        ),
        Program.ROBORACE_OK_JR to setOf(
            SMIRNOV_EVGENIJ_ANDREEVICH,
            KOVALEV_ALEKSANDR_NIKOLAEVICH,
            DUBATOVKA_VLADISLAV_VITALEVICH,
        ),
        Program.LINE_FOLLOWER_PRO to setOf(
            VOLOSHKO_IGOR_LEONIDOVICH,
            YORSH_ALEKSANDR_MIHAJLOVICH,
        ),
        Program.LINE_FOLLOWER_JR to setOf(
            VOLOSHKO_IGOR_LEONIDOVICH,
            YORSH_ALEKSANDR_MIHAJLOVICH,
        ),
        Program.LINE_FOLLOWER_OK to setOf(
            VOLOSHKO_IGOR_LEONIDOVICH,
            YORSH_ALEKSANDR_MIHAJLOVICH,
        ),
        Program.BIG_JOURNEY_JR_OK to setOf(
            VOJTOVICH_PAVEL_LEONIDOVICH,
        ),
        Program.BIG_JOURNEY_SR to setOf(
            VOJTOVICH_PAVEL_LEONIDOVICH,
        ),
        Program.ROBOCUP_JUNIOR_RESCUE_LINE to setOf(
            GAVRILOV_ANDREJ_ANDREEVICH,
            GAVRILOVA_ELENA_LEONIDOVNA,
        ),
        Program.RELAY_RACE to setOf(
            GAVRILOV_ANDREJ_ANDREEVICH,
            KUPRIYANCEV_VLADIMIR_VLADIMIROVICH,
        ),
        Program.DRONES to setOf(
            KUPRIYANCEV_VLADIMIR_VLADIMIROVICH,
            KUPRIYANCEVA_ALESYA_PETROVNA,
        ),
        Program.FOOTBALL_3X3 to setOf(
            GOTOV_NAZAR_EVGENEVICH,
            NECHAEV_VADIM_EVGENEVICH,
        ),
        Program.INTELLECTUAL_SUMO_15X15_OK to setOf(
            SMIRNOV_EVGENIJ_ANDREEVICH,
        ),
        Program.RTK_CUP_SEEKER to setOf(
            SADOVNIKOV_VYACHESLAV,
            GOTOV_NAZAR_EVGENEVICH,
            TARASEVICH_ANATOLIJ_FEDOROVICH,
        ),
        Program.RTK_CUP_EXTREME to setOf(
            SADOVNIKOV_VYACHESLAV,
            GOTOV_NAZAR_EVGENEVICH,
            TARASEVICH_ANATOLIJ_FEDOROVICH,
        ),
        Program.FIRA to setOf(
            OGNEVOJ_VLADIMIR_DMITRIEVICH,
            OGNEVAYA_TATYANA_ANATOLEVNA,
        ),
        Program.WALKING_ROBOT_MARATHON to setOf(
            VOLOSHKO_IGOR_LEONIDOVICH,
            YORSH_ALEKSANDR_MIHAJLOVICH,
        ),
        Program.ARKANOID to setOf(
            User.GOLIK_ALEKSEJ_VALEREVICH,
        ),
        Program.MINI_SUMO to setOf(
            VOLOSHKO_IGOR_LEONIDOVICH,
            YORSH_ALEKSANDR_MIHAJLOVICH,
        ),
        Program.MICRO_SUMO to setOf(
            VOLOSHKO_IGOR_LEONIDOVICH,
            YORSH_ALEKSANDR_MIHAJLOVICH,
        ),
        Program.MAZE to setOf(
            VOLOSHKO_IGOR_LEONIDOVICH,
            YORSH_ALEKSANDR_MIHAJLOVICH,
        ),

        )


    fun findProgramsForUser(user: User): Set<Program> =
        usersInPrograms.filterValues { it.contains(user) }.keys
}
