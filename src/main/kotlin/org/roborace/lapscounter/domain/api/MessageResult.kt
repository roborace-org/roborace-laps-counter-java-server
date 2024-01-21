package org.roborace.lapscounter.domain.api

import org.roborace.lapscounter.domain.api.ResponseType.BROADCAST
import org.roborace.lapscounter.domain.api.ResponseType.SINGLE


data class MessageResult(
    var responseType: ResponseType,
    var messages: MutableList<Message> = mutableListOf(),
) {
    fun add(message: Message) = messages.add(message)
    fun addAll(messages: List<Message>) = this.messages.addAll(messages)


    companion object {
        @JvmStatic
        fun single(message: Message) = MessageResult(SINGLE, mutableListOf(message))

        @JvmStatic
        fun single(messages: List<Message>) = MessageResult(SINGLE, messages.toMutableList())

        @JvmStatic
        fun broadcast(message: Message) = MessageResult(BROADCAST, mutableListOf(message))

        @JvmStatic
        fun broadcast(messages: List<Message>) = MessageResult(BROADCAST, messages.toMutableList())
    }
}
