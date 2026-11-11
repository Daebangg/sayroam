package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.chat.application.port.out.ChatConnectionCloser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class StompSessionExpirySchedulerTest {
    private static final String SESSION_ID = "session-1";
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-07T10:00:00Z");

    private final TaskScheduler taskScheduler = mock(TaskScheduler.class);
    private final ChatConnectionCloser sessionCloser = mock(ChatConnectionCloser.class);
    private final StompSessionExpiryScheduler scheduler =
            new StompSessionExpiryScheduler(taskScheduler, sessionCloser);

    @Test
    void 만료_전에_연결을_끊으면_예약을_취소한다() {
        // given
        ScheduledFuture<?> expiration = mock(ScheduledFuture.class);
        doReturn(expiration).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
        ChatPrincipal principal = new ChatPrincipal(7L, 0L, EXPIRES_AT);
        scheduler.onConnected(new SessionConnectedEvent(this, sessionMessage(), principal));

        // when
        scheduler.onDisconnected(new SessionDisconnectEvent(this, sessionMessage(), SESSION_ID,
                CloseStatus.NORMAL, principal));

        // then
        then(expiration).should().cancel(false);
    }

    @Test
    void 토큰_만료_시각에_종료를_예약한다() {
        // given
        doReturn(mock(ScheduledFuture.class)).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        // when
        scheduler.onConnected(new SessionConnectedEvent(this, sessionMessage(),
                new ChatPrincipal(7L, 0L, EXPIRES_AT)));

        // then
        then(taskScheduler).should().schedule(any(Runnable.class), eq(EXPIRES_AT));
    }

    @Test
    void 만료_작업이_실행되면_예약_항목을_제거한다() {
        // given
        ScheduledFuture<?> expiration = mock(ScheduledFuture.class);
        doReturn(expiration).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
        ChatPrincipal principal = new ChatPrincipal(7L, 0L, EXPIRES_AT);
        scheduler.onConnected(new SessionConnectedEvent(this, sessionMessage(), principal));
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        then(taskScheduler).should().schedule(task.capture(), eq(EXPIRES_AT));

        // when
        task.getValue().run();
        scheduler.onDisconnected(new SessionDisconnectEvent(this, sessionMessage(), SESSION_ID,
                CloseStatus.NORMAL, principal));

        // then
        then(expiration).should(never()).cancel(false);
    }

    private Message<byte[]> sessionMessage() {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId(SESSION_ID);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
