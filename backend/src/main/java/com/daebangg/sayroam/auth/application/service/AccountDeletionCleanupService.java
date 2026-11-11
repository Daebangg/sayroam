package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.port.in.AccountDeletionCleanup;
import com.daebangg.sayroam.notification.application.port.out.NotificationDeletion;
import com.daebangg.sayroam.travelstyle.application.port.out.TravelStyleDeletion;
import com.daebangg.sayroam.user.application.port.out.LoginMethodDeletion;
import com.daebangg.sayroam.user.application.port.out.ProfileImageDeletion;
import org.springframework.stereotype.Service;

/** 탈퇴 시 정리할 데이터의 범위와 실행 순서를 정의한다. 저장 기술과 트랜잭션 구현은 알지 않는다. */
@Service
public class AccountDeletionCleanupService implements AccountDeletionCleanup {
    private final LoginMethodDeletion loginMethods;
    private final TravelStyleDeletion travelStyles;
    private final NotificationDeletion notifications;
    private final ProfileImageDeletion profileImages;

    public AccountDeletionCleanupService(LoginMethodDeletion loginMethods, TravelStyleDeletion travelStyles,
                                         NotificationDeletion notifications, ProfileImageDeletion profileImages) {
        this.loginMethods = loginMethods;
        this.travelStyles = travelStyles;
        this.notifications = notifications;
        this.profileImages = profileImages;
    }

    @Override
    public void deleteOwnedData(long userId) {
        loginMethods.deleteForUser(userId);
        travelStyles.deleteForUser(userId);
        notifications.deleteForUser(userId);
        profileImages.deleteForUser(userId);
    }
}
