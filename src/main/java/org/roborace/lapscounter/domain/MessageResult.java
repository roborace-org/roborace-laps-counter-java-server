package org.roborace.lapscounter.domain;

import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResult {

    private List<Message> messages;
    private ResponseType responseType;

    private MessageResult(Message message, ResponseType responseType) {
        this.messages = Collections.singletonList(message);
        this.responseType = responseType;
    }

    public static MessageResult single(Message message) {
        return new MessageResult(message, ResponseType.SINGLE);
    }

    public static MessageResult broadcast(Message message) {
        return new MessageResult(message, ResponseType.BROADCAST);
    }

    public static MessageResult broadcast() {
        return new MessageResult(new ArrayList<>(), ResponseType.BROADCAST);
    }

    public void add(Message message) {
        messages.add(message);
    }

    public void addAll(List<Message> messages) {
        this.messages.addAll(messages);
    }
}
