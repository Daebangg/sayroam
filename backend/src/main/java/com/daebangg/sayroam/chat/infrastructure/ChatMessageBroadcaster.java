package com.daebangg.sayroam.chat.infrastructure;

import com.daebangg.sayroam.chat.domain.event.ChatMessageSent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatMessageBroadcaster {
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageSent event) {
        messagingTemplate.convertAndSend("/topic/rooms/" + event.roomId(), event);
    }
}
