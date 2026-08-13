package com.gymams.model;

/**
 * Canonical equipment status codes. The enum name (e.g. OPERATIONAL) is
 * what gets stored in MySQL and sent over the API — the frontend owns
 * the human-readable label, same pattern as Role/ROLE_LABELS.
 */
public enum EquipmentStatus {
    OPERATIONAL("Operational"),
    MAINTENANCE_DUE("Maintenance Due"),
    UNDER_MAINTENANCE("Under Maintenance"),
    OUT_OF_SERVICE("Out of Service");

    private final String label;

    EquipmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
