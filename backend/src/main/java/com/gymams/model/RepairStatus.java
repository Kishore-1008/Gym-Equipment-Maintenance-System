package com.gymams.model;

/**
 * Repair request workflow status (Module 4 — Repair Request Management).
 * Enum name is what gets stored in MySQL and sent over the API — the
 * frontend owns the human-readable label, same pattern as EquipmentStatus.
 *
 * Workflow:
 *   PENDING -> (Admin reviews) -> REJECTED (terminal, mandatory reason)
 *                              -> APPROVED -> ASSIGNED -> IN_PROGRESS -> COMPLETED (terminal)
 */
public enum RepairStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    private final String label;

    RepairStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
