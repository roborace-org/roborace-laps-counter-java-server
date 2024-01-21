package org.roborace.lapscounter.service.robofinist

import org.roborace.lapscounter.service.robofinist.User.DUBATOVKA_VLADISLAV_VITALEVICH
import org.roborace.lapscounter.service.robofinist.User.GAVRILOVA_ELENA_LEONIDOVNA
import org.roborace.lapscounter.service.robofinist.User.GAVRILOV_ANDREJ_ANDREEVICH
import org.roborace.lapscounter.service.robofinist.User.GOTOV_NAZAR_EVGENEVICH
import org.roborace.lapscounter.service.robofinist.User.KOVALEV_ALEKSANDR_NIKOLAEVICH
import org.roborace.lapscounter.service.robofinist.User.KUPRIYANCEVA_ALESYA_PETROVNA
import org.roborace.lapscounter.service.robofinist.User.KUPRIYANCEV_VLADIMIR_VLADIMIROVICH
import org.roborace.lapscounter.service.robofinist.User.LIZA
import org.roborace.lapscounter.service.robofinist.User.NECHAEV_VADIM_EVGENEVICH
import org.roborace.lapscounter.service.robofinist.User.OGNEVAYA_TATYANA_ANATOLEVNA
import org.roborace.lapscounter.service.robofinist.User.OGNEVOJ_VLADIMIR_DMITRIEVICH
import org.roborace.lapscounter.service.robofinist.User.OHRIMCHUK_VALERIJ
import org.roborace.lapscounter.service.robofinist.User.SADOVNIKOV_VYACHESLAV
import org.roborace.lapscounter.service.robofinist.User.SAFRONOV_ILYA_EDUARDOVICH
import org.roborace.lapscounter.service.robofinist.User.SMIRNOV_EVGENIJ_ANDREEVICH
import org.roborace.lapscounter.service.robofinist.User.TARASEVICH_ANATOLIJ_FEDOROVICH
import org.roborace.lapscounter.service.robofinist.User.VOJTOVICH_PAVEL_LEONIDOVICH
import org.roborace.lapscounter.service.robofinist.User.VOLOSHKO_IGOR_LEONIDOVICH
import org.roborace.lapscounter.service.robofinist.User.YORSH_ALEKSANDR_MIHAJLOVICH

object JudgeFiller {
    val usersInPrograms = mapOf(
        Program.ROBORACE_PRO to setOf(
            OHRIMCHUK_VALERIJ,
            SMIRNOV_EVGENIJ_ANDREEVICH,
//            ZHUKOVSKIJ_NIKITA_EVGENEVICH,
            KOVALEV_ALEKSANDR_NIKOLAEVICH,
            DUBATOVKA_VLADISLAV_VITALEVICH,
            LIZA,
        ),
        Program.ROBORACE_PRO_MINI to setOf(
//            OHRIMCHUK_VALERIJ,
            SMIRNOV_EVGENIJ_ANDREEVICH,
//            ZHUKOVSKIJ_NIKITA_EVGENEVICH,
            KOVALEV_ALEKSANDR_NIKOLAEVICH,
            DUBATOVKA_VLADISLAV_VITALEVICH,
            LIZA,
        ),
        Program.ROBORACE_OK to setOf(
//            OHRIMCHUK_VALERIJ,
            SMIRNOV_EVGENIJ_ANDREEVICH,
//            ZHUKOVSKIJ_NIKITA_EVGENEVICH,
            KOVALEV_ALEKSANDR_NIKOLAEVICH,
            DUBATOVKA_VLADISLAV_VITALEVICH,
            LIZA,
        ),
        Program.ROBORACE_OK_JR to setOf(
            SMIRNOV_EVGENIJ_ANDREEVICH,
            KOVALEV_ALEKSANDR_NIKOLAEVICH,
            DUBATOVKA_VLADISLAV_VITALEVICH,
            LIZA,
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
            SAFRONOV_ILYA_EDUARDOVICH,
        ),
        Program.BIG_JOURNEY_SR to setOf(
            VOJTOVICH_PAVEL_LEONIDOVICH,
            SAFRONOV_ILYA_EDUARDOVICH,
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
            User.TIMUR,
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
