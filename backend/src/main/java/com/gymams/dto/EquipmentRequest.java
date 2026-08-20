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
     * Optional monthly usage-hour limit — Admin only, configured on the
     * Add/Edit Equipment form. Null/omitted means no limit configured
     * (usage status stays NORMAL). There is no separate daily limit.
     */
    @DecimalMin(value = "0", inclusive = true, message = "Monthly usage limit can't be negative.")
    private Double monthlyUsageLimitHours;

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getMaintenanceInterval() { return maintenanceInterval; }
    public void setMaintenanceInterval(String maintenanceInterval) { this.maintenanceInterval = maintenanceInterval; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getMonthlyUsageLimitHours() { return monthlyUsageLimitHours; }
    public void setMonthlyUsageLimitHours(Double monthlyUsageLimitHours) { this.monthlyUsageLimitHours = monthlyUsageLimitHours; }
}
