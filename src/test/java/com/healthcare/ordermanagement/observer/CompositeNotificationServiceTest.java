package com.healthcare.ordermanagement.observer;

import com.healthcare.ordermanagement.domain.Order;
import com.healthcare.ordermanagement.domain.OrderType;
import com.healthcare.ordermanagement.domain.Priority;
import com.healthcare.ordermanagement.pattern.observer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositeNotificationServiceTest {

    @Mock ConsoleNotificationService console;
    @Mock InAppNotificationService   inApp;
    @Mock EmailNotificationService   email;

    private NotificationPreferences    preferences;
    private CompositeNotificationService composite;

    @BeforeEach
    void setUp() {
        preferences = new NotificationPreferences();
        composite   = new CompositeNotificationService(console, inApp, email, preferences);
    }

    @Test
    void allEnabled_allReceiveNotification() {
        // Arrange
        Order order = new Order("LAB-001", OrderType.LAB, "P", "Dr", "d", Priority.ROUTINE);
        // Act
        composite.notify(order, "ORDER_SUBMITTED");
        // Assert
        verify(console, times(1)).notify(order, "ORDER_SUBMITTED");
        verify(inApp,   times(1)).notify(order, "ORDER_SUBMITTED");
        verify(email,   times(1)).notify(order, "ORDER_SUBMITTED");
    }

    @Test
    void emailDisabled_consoleAndInAppStillFire() {
        // Arrange
        preferences.setEmailEnabled(false);
        Order order = new Order("LAB-001", OrderType.LAB, "P", "Dr", "d", Priority.ROUTINE);
        // Act
        composite.notify(order, "ORDER_SUBMITTED");
        // Assert
        verify(console, times(1)).notify(any(), any());
        verify(inApp,   times(1)).notify(any(), any());
        verify(email,   never()).notify(any(), any());
    }

    @Test
    void allDisabled_noneReceive() {
        // Arrange
        preferences.setConsoleEnabled(false);
        preferences.setInAppEnabled(false);
        preferences.setEmailEnabled(false);
        Order order = new Order("LAB-001", OrderType.LAB, "P", "Dr", "d", Priority.ROUTINE);
        // Act
        composite.notify(order, "ORDER_SUBMITTED");
        // Assert
        verify(console, never()).notify(any(), any());
        verify(inApp,   never()).notify(any(), any());
        verify(email,   never()).notify(any(), any());
    }

    @Test
    void onlyInApp_enabled_onlyInAppFires() {
        // Arrange
        preferences.setConsoleEnabled(false);
        preferences.setEmailEnabled(false);
        Order order = new Order("LAB-001", OrderType.LAB, "P", "Dr", "d", Priority.ROUTINE);
        // Act
        composite.notify(order, "ORDER_CLAIMED");
        // Assert
        verify(console, never()).notify(any(), any());
        verify(inApp,   times(1)).notify(any(), any());
        verify(email,   never()).notify(any(), any());
    }
}