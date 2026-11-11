package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.notification.application.port.out.NotificationDeletion;
import com.daebangg.sayroam.travelstyle.application.port.out.TravelStyleDeletion;
import com.daebangg.sayroam.user.application.port.out.LoginMethodDeletion;
import com.daebangg.sayroam.user.application.port.out.ProfileImageDeletion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AccountDeletionCleanupServiceTests {
    private final LoginMethodDeletion loginMethods = mock(LoginMethodDeletion.class);
    private final TravelStyleDeletion travelStyles = mock(TravelStyleDeletion.class);
    private final NotificationDeletion notifications = mock(NotificationDeletion.class);
    private final ProfileImageDeletion profileImages = mock(ProfileImageDeletion.class);
    private final AccountDeletionCleanupService service = new AccountDeletionCleanupService(loginMethods, travelStyles, notifications, profileImages);

    @Test
    void delegatesEveryCleanupToTheSameMemberId() {
        service.deleteOwnedData(55);

        verify(loginMethods).deleteForUser(55);
        verify(travelStyles).deleteForUser(55);
        verify(notifications).deleteForUser(55);
        verify(profileImages).deleteForUser(55);
    }

    @Test
    void propagatesTheFirstFailureWithoutStartingOtherCleanup() {
        IllegalStateException failure = new IllegalStateException("Test storage failure");
        doThrow(failure).when(loginMethods).deleteForUser(55);

        assertThatThrownBy(() -> service.deleteOwnedData(55)).isSameAs(failure);
        verifyNoInteractions(travelStyles, notifications, profileImages);
    }

    @Test
    void propagatesLaterFailureToTheOuterTransactionAndStopsRemainingCleanup() {
        IllegalStateException failure = new IllegalStateException("Test storage failure");
        doThrow(failure).when(travelStyles).deleteForUser(55);

        assertThatThrownBy(() -> service.deleteOwnedData(55)).isSameAs(failure);
        verify(loginMethods).deleteForUser(55);
        verifyNoInteractions(notifications, profileImages);
    }
}
