package com.healthcare.ordermanagement.client;

import com.healthcare.ordermanagement.pattern.observer.InAppNotificationService;
import com.healthcare.ordermanagement.pattern.observer.NotificationMessage;
import com.healthcare.ordermanagement.pattern.observer.NotificationPreferences;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CHANGE 2a — REST endpoints for notification preferences, badge, and message list.
 *
 * GET  /api/notifications/preferences         → channel flags
 * POST /api/notifications/preferences         → update flags
 * GET  /api/notifications/badge               → unread count
 * GET  /api/notifications/messages            → full message list (for panel)
 * POST /api/notifications/messages/read       → mark all read (clear badge)
 * DELETE /api/notifications/messages/{id}     → delete one message
 * DELETE /api/notifications/messages          → clear all
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationPreferenceController {

    private final NotificationPreferences preferences;
    private final InAppNotificationService inApp;

    public NotificationPreferenceController(NotificationPreferences preferences,
                                            InAppNotificationService inApp) {
        this.preferences = preferences;
        this.inApp       = inApp;
    }

    @GetMapping("/preferences")
    public ResponseEntity<Map<String, Boolean>> getPreferences() {
        return ResponseEntity.ok(Map.of(
            "console", preferences.isConsoleEnabled(),
            "inApp",   preferences.isInAppEnabled(),
            "email",   preferences.isEmailEnabled()
        ));
    }

    @PostMapping("/preferences")
    public ResponseEntity<Map<String, Boolean>> updatePreferences(@RequestBody Map<String, Boolean> body) {
        if (body.containsKey("console")) preferences.setConsoleEnabled(body.get("console"));
        if (body.containsKey("inApp"))   preferences.setInAppEnabled(body.get("inApp"));
        if (body.containsKey("email"))   preferences.setEmailEnabled(body.get("email"));
        return ResponseEntity.ok(Map.of(
            "console", preferences.isConsoleEnabled(),
            "inApp",   preferences.isInAppEnabled(),
            "email",   preferences.isEmailEnabled()
        ));
    }

    @GetMapping("/badge")
    public ResponseEntity<Map<String, Integer>> getBadge() {
        return ResponseEntity.ok(Map.of("count", inApp.getBadgeCount()));
    }

    @GetMapping("/messages")
    public ResponseEntity<List<NotificationMessage>> getMessages() {
        return ResponseEntity.ok(inApp.getMessages());
    }

    @PostMapping("/messages/read")
    public ResponseEntity<Map<String, Integer>> markRead() {
        inApp.markAllRead();
        return ResponseEntity.ok(Map.of("count", 0));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable String id) {
        boolean deleted = inApp.deleteMessage(id);
        if (deleted) return ResponseEntity.ok(Map.of("deleted", id));
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/messages")
    public ResponseEntity<Map<String, String>> clearAll() {
        inApp.clearAll();
        return ResponseEntity.ok(Map.of("message", "All notifications cleared"));
    }
}