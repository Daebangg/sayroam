package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.notification.application.port.out.NotificationDeletion;
import com.daebangg.sayroam.travelstyle.application.port.out.TravelStyleDeletion;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AccountDeletionCleanupServiceTests {
    private final TravelStyleDeletion travelStyles = mock(TravelStyleDeletion.class);
    private final NotificationDeletion notifications = mock(NotificationDeletion.class);
    private final AccountDeletionCleanupService service = new AccountDeletionCleanupService(travelStyles, notifications);

    @Test
    void delegatesEveryCleanupToTheSameMemberId() {
        service.deleteOwnedData(55);

        InOrder order = inOrder(travelStyles, notifications);
        order.verify(travelStyles).deleteForUser(55);
        order.verify(notifications).deleteForUser(55);
    }

    @Test
    void propagatesFailureToTheOuterTransactionAndStopsRemainingCleanup() {
        IllegalStateException failure = new IllegalStateException("Test storage failure");
        doThrow(failure).when(travelStyles).deleteForUser(55);

        assertThatThrownBy(() -> service.deleteOwnedData(55)).isSameAs(failure);
        verifyNoInteractions(notifications);
    }
}
