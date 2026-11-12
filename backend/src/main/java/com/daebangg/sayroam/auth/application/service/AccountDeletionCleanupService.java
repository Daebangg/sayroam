package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.notification.application.port.out.NotificationDeletion;
import com.daebangg.sayroam.travelstyle.application.port.out.TravelStyleDeletion;
import com.daebangg.sayroam.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 탈퇴 시 정리할 데이터의 범위와 실행 순서를 정의한다. 저장 기술과 트랜잭션 구현은 알지 않는다. */
@Service
public class AccountDeletionCleanupService {
    private final UserRepository users;
    private final TravelStyleDeletion travelStyles;
    private final NotificationDeletion notifications;

    public AccountDeletionCleanupService(UserRepository users, TravelStyleDeletion travelStyles,
                                         NotificationDeletion notifications) {
        this.users = users;
        this.travelStyles = travelStyles;
        this.notifications = notifications;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteOwnedData(long userId) {
        users.deleteLoginMethods(userId);
        travelStyles.deleteForUser(userId);
        notifications.deleteForUser(userId);
    }
}
