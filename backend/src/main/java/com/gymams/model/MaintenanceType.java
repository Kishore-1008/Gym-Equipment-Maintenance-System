package com.gymams.model;

/** Canonical scheduled/preventive maintenance types — same enum-name/label split as EquipmentStatus. */
public enum MaintenanceType {
    ROUTINE_MAINTENANCE("Routine Maintenance"),
    PREVENTIVE_MAINTENANCE("Preventive Maintenance"),
    INSPECTION("Inspection"),
    CLEANING("Cleaning");

    private final String label;

    MaintenanceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
