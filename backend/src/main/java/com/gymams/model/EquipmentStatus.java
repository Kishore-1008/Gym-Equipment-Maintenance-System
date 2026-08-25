package com.gymams.model;

/**
 * Canonical equipment status codes. The enum name (e.g. OPERATIONAL) is
 * what gets stored in MySQL and sent over the API — the frontend owns
 * the human-readable label, same pattern as Role/ROLE_LABELS.
 *
 * OPERATIONAL / MAINTENANCE_DUE / UNDER_MAINTENANCE / UNDER_REPAIR are all
 * automatically recomputed by EquipmentService.recomputeStatus() from the
 * equipment's currently active RepairRequest / MaintenanceSchedule rows —
 * see that method for the full priority rules (repair takes priority over
 * maintenance; either condition takes priority over plain OPERATIONAL).
 * OUT_OF_SERVICE is the one exception: it is only ever set manually by the
 * Admin (via the equipment edit form or the status quick-change action) and
 * recomputeStatus() never overwrites it — it's a deliberate override that
 * only the Admin can clear.
 */
public enum EquipmentStatus {
    OPERATIONAL("Operational"),
    MAINTENANCE_DUE("Maintenance Due"),
    UNDER_MAINTENANCE("Under Maintenance"),
    UNDER_REPAIR("Under Repair"),
    OUT_OF_SERVICE("Out of Service");

    private final String label;

    EquipmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
