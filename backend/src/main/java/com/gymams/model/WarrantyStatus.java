package com.gymams.model;

/**
 * Computed warranty status — NEVER persisted on the Warranty entity itself.
 * Derived purely from expiryDate at read time by WarrantyService, the same
 * way UsageStatus is derived from usage hours rather than stored: storing
 * a "status" column here would just be a cache that silently goes stale
 * every day as expiryDate approaches, so it's recomputed on every read
 * instead.
 */
public enum WarrantyStatus {
    ACTIVE("Active"),
    EXPIRING_SOON("Expiring Soon"),
    EXPIRED("Expired");

    private final String label;

    WarrantyStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}