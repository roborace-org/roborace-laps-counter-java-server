package org.roborace.lapscounter.robofinist.model.bid

data class Bid(
    val id: Int,
    val name: String,
    val status: Int,
    val statusLabel: String = BidStatus.fromCode(status).text,
    val organizations: List<Organization>,
)

data class Organization(
    val id: Int,
    val name: String,
)

enum class BidStatus(val code: Int, val text: String) {
    UNKNOWN(-1, "Не известно"),
    DRAFT(0, "Черновик"),
    REVIEW(1, "На рассмотрении/отправлена"),
    REQUIRE_CLARIFICATION(2, "Требует уточнения"),
    REMOVED(3, "Удалена"),
    REJECTED(4, "Отклонена"),
    ACCEPTED(5, "Принята"),
    PARTICIPATED(6, "Приняла участие"),
    ABSENCE(7, "Неявка");
    companion object {
        fun fromCode(code: Int): BidStatus {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}