package com.healthcare.ordermanagement.client;

import com.healthcare.ordermanagement.manager.OrderManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CHANGE 3 — Replay endpoint.
 *
 * POST /api/audit/replay/{index}
 * Re-executes the command at position {index} in the audit history.
 * Main use case: re-submit an accidentally cancelled order.
 * After replay, TriagingEngine queue is synced by OrderManager.
 */
@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class ReplayController {

    private final OrderManager orderManager;

    public ReplayController(OrderManager orderManager) {
        this.orderManager = orderManager;
    }

    @PostMapping("/replay/{index}")
    public ResponseEntity<?> replayCommand(@PathVariable int index) {
        boolean success = orderManager.replayCommand(index);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Command at index " + index + " replayed"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid index: " + index));
    }
}