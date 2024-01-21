package org.roborace.lapscounter.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.websocket.ClientEndpoint
import jakarta.websocket.CloseReason
import jakarta.websocket.CloseReason.CloseCodes
import jakarta.websocket.ContainerProvider
import jakarta.websocket.OnClose
import jakarta.websocket.OnError
import jakarta.websocket.OnMessage
import jakarta.websocket.OnOpen
import jakarta.websocket.Session
import org.roborace.lapscounter.domain.State
import org.roborace.lapscounter.domain.Type
import org.roborace.lapscounter.domain.api.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

@ClientEndpoint
class WebsocketClient(endpointURI: URI, val name: String) {

    private val userSession: Session = ContainerProvider.getWebSocketContainer()
        .connectToServer(this, endpointURI)

    private val messages: Queue<Message> = ConcurrentLinkedQueue()

    var lastMessage: Message = Message(type = Type.ERROR)
    var state: State? = null

    @OnOpen
    fun onOpen(userSession: Session) {
        log.info("[{}] Opening websocket userSession = [{}]", name, userSession.id)
    }

    @OnClose
    fun onClose(userSession: Session, reason: CloseReason) {
        log.info("[{}] Closing websocket userSession = [{}], reason = [{}]", name, userSession.id, reason)
    }

    @OnError
    fun onError(t: Throwable) {
        log.error("[{}] Websocket error = [{}]", name, t.message)
    }

    @OnMessage
    fun onMessage(message: String) {
        log.info("[{}] Message received = [{}]", name, message)
        messages.add(objectMapper.readValue(message, Message::class.java))
    }

    fun sendMessage(message: String) {
        log.info("[{}] Send message = [{}]", name, message)
        userSession.asyncRemote.sendText(message)
    }

    fun closeClient() {
        log.info("Close client $name!")
        if (userSession.isOpen) {
            try {
                userSession.close(CloseReason(CloseCodes.NORMAL_CLOSURE, "close client"))
            } catch (e: IOException) {
                log.warn("Error closing session: ${e.message}", e)
            }
        }
    }

    fun pollMessage(): Message = messages.poll()

    fun hasMessage() = messages.isNotEmpty()

    fun hasNoMessages(): Boolean = messages.isEmpty()

    fun hasMessageWithType(type: Type): Boolean {
        while (messages.isNotEmpty()) {
            lastMessage = messages.poll()
            if (lastMessage.type == Type.STATE) {
                state = lastMessage.state
            }
            if (lastMessage.type == type) return true
        }
        return false
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(WebsocketClient::class.java)
        private val objectMapper = jacksonObjectMapper()
    }
}