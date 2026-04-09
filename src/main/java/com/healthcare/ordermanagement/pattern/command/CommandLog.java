package com.healthcare.ordermanagement.pattern.command;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CHANGE 3: added undoLastAndGet() (returns the undone command so
 * OrderManager can sync TriagingEngine), replayAt() for command replay,
 * and recordNote() for non-undoable audit entries (used by StatAuditDecorator).
 */
@Component
public class CommandLog {

    private final List<CommandLogEntry> log         = new ArrayList<>();
    private final List<Command>         undoStack   = new ArrayList<>();
    private final List<Command>         allCommands = new ArrayList<>(); // for replay

    // Record an executed command (audit entry + undo stack + replay history)
    public void record(Command command) {
        log.add(new CommandLogEntry(
            command.getCommandType(), command.getOrderId(), command.getActor()
        ));
        undoStack.add(command);
        allCommands.add(command);
    }

    // Record a non-undoable audit annotation (e.g. STAT_AUDIT from StatAuditDecorator)
    public void recordNote(String type, String orderId, String actor) {
        log.add(new CommandLogEntry(type, orderId, actor));
        // intentionally NOT added to undoStack or allCommands — notes are informational only
    }

    public List<CommandLogEntry> getAll() {
        return Collections.unmodifiableList(log);
    }

    // Kept for backward compat (AuditController still works)
    public boolean undoLast() {
        return undoLastAndGet() != null;
    }

    // Returns the undone Command so OrderManager can sync TriagingEngine
    public Command undoLastAndGet() {
        if (undoStack.isEmpty()) return null;
        Command last = undoStack.remove(undoStack.size() - 1);
        last.undo();
        return last;
    }

    // Re-executes the command at `index` in the full history.
    // Returns the replayed Command, or null if index is invalid.
    public Command replayAt(int index) {
        if (index < 0 || index >= allCommands.size()) return null;
        Command cmd = allCommands.get(index);
        cmd.execute();
        // Add a replay entry to the visible audit trail
        log.add(new CommandLogEntry(
            "REPLAY:" + cmd.getCommandType(), cmd.getOrderId(), cmd.getActor()
        ));
        // The replayed command goes onto undo stack so it can be immediately undone
        undoStack.add(cmd);
        allCommands.add(cmd);
        return cmd;
    }

    public int size() { return allCommands.size(); }
}