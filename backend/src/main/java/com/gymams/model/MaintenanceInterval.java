package com.gymams.model;

/** Canonical maintenance interval codes — same enum-name/label split as EquipmentStatus. */
public enum MaintenanceInterval {
    ONE_MONTH("1 Month"),
    THREE_MONTHS("3 Months"),
    SIX_MONTHS("6 Months"),
    TWELVE_MONTHS("12 Months");

    private final String label;

    MaintenanceInterval(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
