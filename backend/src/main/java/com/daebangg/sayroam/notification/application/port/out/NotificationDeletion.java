package com.daebangg.sayroam.notification.application.port.out;

public interface NotificationDeletion {
    /** 기존 탈퇴 트랜잭션에 참여해 해당 회원이 수신자인 알림만 삭제한다. */
    void deleteForUser(long userId);
}
