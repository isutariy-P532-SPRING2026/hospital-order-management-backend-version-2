package com.healthcare.ordermanagement.client;

import com.healthcare.ordermanagement.pattern.observer.InAppNotificationService;
import com.healthcare.ordermanagement.pattern.strategy.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CHANGE 1 — Client endpoint for runtime strategy switching.
 *
 * GET  /api/triage/strategy         → current strategy name
 * POST /api/triage/strategy  body: { "strategy": "PRIORITY|LOAD_BALANCE|DEADLINE" }
 */
@RestController
@RequestMapping("/api/triage")
@CrossOrigin(origins = "*")
public class TriageStrategyController {

    private final TriageStrategyHolder        holder;
    private final PriorityTriageStrategy      priorityStrategy;
    private final LoadBalancingTriageStrategy  loadBalancingStrategy;
    private final DeadlineFirstTriageStrategy  deadlineFirstStrategy;
    private final InAppNotificationService    inApp;

    public TriageStrategyController(TriageStrategyHolder holder,
                                    PriorityTriageStrategy priorityStrategy,
                                    LoadBalancingTriageStrategy loadBalancingStrategy,
                                    DeadlineFirstTriageStrategy deadlineFirstStrategy,
                                    InAppNotificationService inApp) {
        this.holder               = holder;
        this.priorityStrategy     = priorityStrategy;
        this.loadBalancingStrategy = loadBalancingStrategy;
        this.deadlineFirstStrategy = deadlineFirstStrategy;
        this.inApp                = inApp;
    }

    @GetMapping("/strategy")
    public ResponseEntity<Map<String, String>> getStrategy() {
        return ResponseEntity.ok(Map.of("strategy", holder.getActiveName()));
    }

    @PostMapping("/strategy")
    public ResponseEntity<?> setStrategy(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("strategy", "").toUpperCase();
        TriageStrategy next = switch (name) {
            case "PRIORITY"     -> priorityStrategy;
            case "LOAD_BALANCE" -> loadBalancingStrategy;
            case "DEADLINE"     -> deadlineFirstStrategy;
            default -> null;
        };
        if (next == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Unknown strategy. Use PRIORITY, LOAD_BALANCE, or DEADLINE."));
        }
        holder.setStrategy(next);

        // Notify in-app that strategy was changed (shows in notification panel)
        inApp.notifySystem("STRATEGY_CHANGED",
            "Triage strategy changed to: " + name);

        System.out.printf("%n  [TRIAGE STRATEGY] Active strategy switched to: %s%n%n", name);
        return ResponseEntity.ok(Map.of("strategy", name, "message", "Strategy updated to " + name));
    }
}