package com.gymams.dto;

/**
 * One row of the Usage Monitoring table: EQUIPMENT | TODAY | THIS MONTH |
 * MAINTENANCE LIMIT | STATUS. The derived maintenanceStatus compares
 * monthUsageHours against monthlyUsageLimitHours only — it is purely
 * usage-based and never reflects actual damage, which Module 4 tracks
 * separately.
 */
public class EquipmentUsageResponse {
    private String equipmentId;
    private String equipmentName;
    private String category;
    private double todayUsageHours;
    private double monthUsageHours;
    private Double monthlyUsageLimitHours;
    private String maintenanceStatus;

    public EquipmentUsageResponse(String equipmentId, String equipmentName, String category,
                                   double todayUsageHours, double monthUsageHours,
                                   Double monthlyUsageLimitHours,
                                   String maintenanceStatus) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.category = category;
        this.todayUsageHours = todayUsageHours;
        this.monthUsageHours = monthUsageHours;
        this.monthlyUsageLimitHours = monthlyUsageLimitHours;
        this.maintenanceStatus = maintenanceStatus;
    }

    public String getEquipmentId() { return equipmentId; }
    public String getEquipmentName() { return equipmentName; }
    public String getCategory() { return category; }
    public double getTodayUsageHours() { return todayUsageHours; }
    public double getMonthUsageHours() { return monthUsageHours; }
    public Double getMonthlyUsageLimitHours() { return monthlyUsageLimitHours; }
    public String getMaintenanceStatus() { return maintenanceStatus; }
}
