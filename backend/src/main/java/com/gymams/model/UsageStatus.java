package com.gymams.model;

/**
 * Usage-based PREVENTIVE maintenance status for a piece of equipment.
 * This is entirely separate from EquipmentStatus (which reflects actual
 * damage/repair state driven by Module 4 — Repair Request Management).
 * A HIGH usage status never implies damage; it only means the equipment
 * has approached or reached its configured usage-hour threshold and is
 * due for preventive servicing.
 *
 * Never stored — always derived on read from (usageHours, limitHours).
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
     * @param usageHours accumulated usage for the period (day or month)
     * @param limitHours configured threshold for that same period, or null if unconfigured
     */
    public static UsageStatus fromUsage(double usageHours, Double limitHours) {
        if (limitHours == null || limitHours <= 0) return NORMAL;
        if (usageHours >= limitHours) return MAINTENANCE_DUE;
        if (usageHours >= limitHours * NEAR_LIMIT_RATIO) return NEAR_LIMIT;
        return NORMAL;
    }

    /** Worse of two statuses, e.g. combining the daily and monthly reading for one equipment. */
    public static UsageStatus worseOf(UsageStatus a, UsageStatus b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
