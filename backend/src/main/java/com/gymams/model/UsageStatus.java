package com.gymams.model;

/**
 * Usage-based PREVENTIVE maintenance status for a piece of equipment,
 * derived from THIS MONTH'S accumulated usage hours against the
 * Admin-configured monthly usage limit (there is no separate daily
 * limit). Entirely separate from EquipmentStatus (which reflects actual
 * damage/repair state driven by Module 4 — Repair Request Management):
 * a MAINTENANCE_DUE usage status never implies damage, never changes
 * EquipmentStatus, never auto-creates a repair request, and never
 * auto-assigns a technician.
 *
 * Never stored — always derived on read from (monthUsageHours, limitHours).
 * NORMAL is also the result whenever no limit has been configured.
 */
public enum UsageStatus {
    NORMAL("Normal"),
    NEAR_LIMIT("Maintenance Due Soon"),
    MAINTENANCE_DUE("Maintenance Due");

    /** usage / limit ratio at/above this -> NEAR_LIMIT (below MAINTENANCE_DUE). */
    public static final double NEAR_LIMIT_RATIO = 0.8;

    private final String label;

    UsageStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * @param monthUsageHours accumulated usage for the current month
     * @param limitHours configured monthly usage limit, or null if unconfigured
     */
    public static UsageStatus fromUsage(double monthUsageHours, Double limitHours) {
        if (limitHours == null || limitHours <= 0) return NORMAL;
        if (monthUsageHours >= limitHours) return MAINTENANCE_DUE;
        if (monthUsageHours >= limitHours * NEAR_LIMIT_RATIO) return NEAR_LIMIT;
        return NORMAL;
    }
}
