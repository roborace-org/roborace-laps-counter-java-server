import java.time.DayOfWeek
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate

class CalendarGenerator {

    val months = mapOf(
        1 to "Январь",
        2 to "Февраль",
        3 to "Март",
        4 to "Апрель",
        5 to "Май",
        6 to "Июнь",
        7 to "Июль",
        8 to "Август",
        9 to "Сентябрь",
        10 to "Октябрь",
        11 to "Ноябрь",
        12 to "Декабрь"
    )

    fun generate() {

        val all = """
            <table>
            <tr>
            <td>${generate(2024, 10)}</td>
            <td>${generate(2024, 11)}</td>
            <td>${generate(2024, 12)}</td>
            </tr>
            <tr>
            <td>${generate(2025, 1)}</td>
            <td>${generate(2025, 2)}</td>
            <td>${generate(2025, 3)}</td>
            </tr>
            <tr>
            <td>${generate(2025, 4)}</td>
            <td>${generate(2025, 5)}</td>
            <td>${generate(2025, 6)}</td>
            </tr>
            </table>
        """.trimIndent()

        println(all)

    }

    private fun generate(year: Int, month: Int): String {

        val header = """
            <thead><tr><td colspan="7">${months[month]}</td></tr></thead>
            <tr>
              <td>Пн</td>
              <td>Вт</td>
              <td>Ср</td>
              <td>Чт</td>
              <td>Пт</td>
              <td>Сб</td>
              <td>Вск</td>
            </tr>
            """.trimIndent()

        var date = LocalDate.of(year, month, 1)

        var line = "<tr>"
        var temp = date
        while (temp.dayOfWeek != DayOfWeek.MONDAY) {
            temp = temp.minusDays(1)
            line += "<td></td>"
        }

        while (date.month.value == month) {
            val style = if (date.dayOfWeek == SATURDAY || date.dayOfWeek == SUNDAY) """ style="background-color: #fff82d;"""" else ""
            line += "<td$style>${date.dayOfMonth}</td>"
            if (date.dayOfWeek == SUNDAY) {
                line += "</tr>\n<tr>"
            }
            date = date.plusDays(1)
        }
        while (date.dayOfWeek != DayOfWeek.MONDAY) {
            line += "<td></td>"
            date = date.plusDays(1)
        }
        line += "</tr>"

        return """
                <table>
                $header
                $line
                </table>
                """.trimIndent()
    }


}

fun maincal() {
    CalendarGenerator().generate()

}