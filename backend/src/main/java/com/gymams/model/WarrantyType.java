package com.gymams.model;

/** Canonical warranty types — same enum-name/label split as MaintenanceType/EquipmentStatus. */
public enum WarrantyType {
    MANUFACTURER("Manufacturer"),
    EXTENDED("Extended"),
    DEALER("Dealer"),
    OTHER("Other");

    private final String label;

    WarrantyType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}