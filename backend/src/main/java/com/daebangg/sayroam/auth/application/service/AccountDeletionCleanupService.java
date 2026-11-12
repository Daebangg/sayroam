package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.notification.application.port.out.NotificationDeletion;
import com.daebangg.sayroam.travelstyle.application.port.out.TravelStyleDeletion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 탈퇴 트랜잭션에 참여해 다른 도메인의 회원 데이터를 정리한다. */
@Service
public class AccountDeletionCleanupService {
    private final TravelStyleDeletion travelStyles;
    private final NotificationDeletion notifications;

    public AccountDeletionCleanupService(TravelStyleDeletion travelStyles, NotificationDeletion notifications) {
        this.travelStyles = travelStyles;
        this.notifications = notifications;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteOwnedData(long userId) {
        travelStyles.deleteForUser(userId);
        notifications.deleteForUser(userId);
    }
}
