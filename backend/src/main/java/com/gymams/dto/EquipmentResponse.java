package com.gymams.dto;

public class EquipmentResponse {
    private String equipmentId;
    private String equipmentName;
    private String category;
    private String maintenanceInterval;
    private String status;
    private Double maintenanceUsageLimitHours;

    public EquipmentResponse(String equipmentId, String equipmentName, String category,
                              String maintenanceInterval, String status,
                              Double maintenanceUsageLimitHours) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.category = category;
        this.maintenanceInterval = maintenanceInterval;
        this.status = status;
        this.maintenanceUsageLimitHours = maintenanceUsageLimitHours;
    }

    public String getEquipmentId() { return equipmentId; }
    public String getEquipmentName() { return equipmentName; }
    public String getCategory() { return category; }
    public String getMaintenanceInterval() { return maintenanceInterval; }
    public String getStatus() { return status; }
    public Double getMaintenanceUsageLimitHours() { return maintenanceUsageLimitHours; }
}
