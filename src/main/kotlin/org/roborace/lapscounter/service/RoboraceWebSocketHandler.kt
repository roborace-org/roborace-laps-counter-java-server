package org.roborace.lapscounter.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.api.Message
import org.roborace.lapscounter.domain.api.ResponseType
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

private val logger = KotlinLogging.logger {}

@Component
class RoboraceWebSocketHandler(
    private val lapsCounterService: LapsCounterService,
    private val sessions: CopyOnWriteArrayList<WebSocketSession> = CopyOnWriteArrayList(),
) : TextWebSocketHandler() {

    public override fun handleTextMessage(session: WebSocketSession, textMessage: TextMessage) {
        try {
            logger.info("handleTextMessage {} {}", session.remoteAddress, textMessage.payload)
            val message = JSON.readValue(textMessage.payload, Message::class.java)

            val messageResult = lapsCounterService.handleMessage(message)

            when (messageResult.responseType) {
                ResponseType.BROADCAST -> broadcast(messageResult.messages)
                ResponseType.SINGLE -> sendSingleSession(messageResult.messages, session)
            }
        } catch (e: Exception) {
            logger.error("Exception happen during message handling: {}", e.message, e)
            if (session.isOpen) {
                val message = Message(Type.ERROR, message = e.message)
                sendTextMessage(convert(message), session)
            }
        }
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        super.afterConnectionEstablished(session)
        logger.debug("ConnectionEstablished {}", session.remoteAddress)
        sessions.add(session)

        sendSingleSession(lapsCounterService.afterConnectionEstablished(), session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.debug("ConnectionClosed {}", session.remoteAddress)
        sessions.remove(session)
    }

    fun getSessionsCopy(): List<WebSocketSession> = sessions.toList()

    private fun sendSingleSession(messages: List<Message>, session: WebSocketSession) =
        messages.forEach { sendTextMessage(convert(it), session) }

    fun broadcast(messages: List<Message>) = messages.forEach { broadcast(it) }

    fun broadcast(message: Message) {
        val textMessage = convert(message)
        sessions.stream()
            .filter { it.isOpen }
            .forEach { sendTextMessage(textMessage, it) }
    }

    private fun convert(message: Message) =
        try {
            TextMessage(JSON.writeValueAsString(message))
        } catch (e: JsonProcessingException) {
            logger.error("Error creating json message for object: {}. Reason: {}", message, e.message, e)
            throw RuntimeException(e)
        }

    @Synchronized
    private fun sendTextMessage(textMessage: TextMessage, session: WebSocketSession) =
        try {
            session.sendMessage(textMessage)
        } catch (e: IOException) {
            logger.error("Error while sending messages to ws client. Reason: {}", e.message, e)
        }

    companion object {
        private val JSON = jacksonObjectMapper()
    }
}
