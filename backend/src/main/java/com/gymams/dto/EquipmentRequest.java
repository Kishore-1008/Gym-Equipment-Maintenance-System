package com.gymams.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public class EquipmentRequest {

    @NotBlank(message = "Select an equipment name.")
    private String equipmentName;

    @NotBlank(message = "Select a maintenance interval.")
    private String maintenanceInterval;

    /** Optional. Defaults to OPERATIONAL server-side when creating and left as-is when editing. */
    private String status;

    /**
     * Optional preventive-maintenance usage-hour limit — Admin only, checked
     * against the equipment's current-month usage total. Null/omitted clears
     * the limit (usage status stays NORMAL).
     */
    @DecimalMin(value = "0", inclusive = true, message = "Maintenance usage limit can't be negative.")
    private Double maintenanceUsageLimitHours;

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getMaintenanceInterval() { return maintenanceInterval; }
    public void setMaintenanceInterval(String maintenanceInterval) { this.maintenanceInterval = maintenanceInterval; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getMaintenanceUsageLimitHours() { return maintenanceUsageLimitHours; }
    public void setMaintenanceUsageLimitHours(Double maintenanceUsageLimitHours) { this.maintenanceUsageLimitHours = maintenanceUsageLimitHours; }
}
