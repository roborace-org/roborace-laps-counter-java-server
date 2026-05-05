package org.roborace.lapscounter.robofinist.model.stage

import com.fasterxml.jackson.annotation.JsonProperty

data class Stage(
    @param:JsonProperty("id") val id: Long,
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("status") val status: Int? = null,
    @param:JsonProperty("statusLabel") val statusLabel: String? = null,
)

enum class StageStatus(val code: Int, val text: String) {
    REGISTRATION(0, "Регистрация"),
    FORMING(1, "Формируется"),
    IN_PROGRESS(2, "В процессе"),
    COMPLETED(3, "Завершено"),
}