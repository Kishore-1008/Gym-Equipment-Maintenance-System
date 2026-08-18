package com.gymams.dto;

/**
 * One equipment's usage-monitoring row: today's hours, this month's
 * accumulated hours, the single Admin-configured maintenance usage limit
 * (checked against the current month total), and the derived
 * preventive-maintenance status (NORMAL / NEAR_LIMIT / MAINTENANCE_DUE).
 * This status is purely usage-based and never reflects actual damage —
 * that is tracked separately by Module 4 (Repair Request Management).
 */
public class EquipmentUsageResponse {
    private String equipmentId;
    private String equipmentName;
    private String category;
    private double todayUsageHours;
    private double monthUsageHours;
    private Double maintenanceUsageLimitHours;
    private String maintenanceStatus;

    public EquipmentUsageResponse(String equipmentId, String equipmentName, String category,
                                   double todayUsageHours, double monthUsageHours,
                                   Double maintenanceUsageLimitHours,
                                   String maintenanceStatus) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.category = category;
        this.todayUsageHours = todayUsageHours;
        this.monthUsageHours = monthUsageHours;
        this.maintenanceUsageLimitHours = maintenanceUsageLimitHours;
        this.maintenanceStatus = maintenanceStatus;
    }

    public String getEquipmentId() { return equipmentId; }
    public String getEquipmentName() { return equipmentName; }
    public String getCategory() { return category; }
    public double getTodayUsageHours() { return todayUsageHours; }
    public double getMonthUsageHours() { return monthUsageHours; }
    public Double getMaintenanceUsageLimitHours() { return maintenanceUsageLimitHours; }
    public String getMaintenanceStatus() { return maintenanceStatus; }
}
