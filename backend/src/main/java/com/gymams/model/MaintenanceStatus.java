package com.gymams.model;

/**
 * Maintenance schedule workflow status (Module 5 — Maintenance Management).
 * Entirely separate from RepairStatus — a maintenance record never reflects
 * damage and a repair request never reflects scheduled/preventive servicing.
 *
 * Workflow:
 *   SCHEDULED -> IN_PROGRESS -> COMPLETED (terminal)
 *   SCHEDULED -> CANCELLED (terminal, Admin only, before work starts)
 */
public enum MaintenanceStatus {
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    MaintenanceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
