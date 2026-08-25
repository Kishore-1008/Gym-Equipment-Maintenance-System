package com.gymams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Admin's "Schedule Maintenance" form submission (Module 5). */
public class MaintenanceScheduleCreateRequest {

    @NotBlank(message = "Select equipment.")
    private String equipmentId;

    @NotNull(message = "Select a maintenance date.")
    private String maintenanceDate; // ISO yyyy-MM-dd

    @NotBlank(message = "Select a maintenance type.")
    private String maintenanceType;

    private String description;

    @NotBlank(message = "Select a technician.")
    private String technicianUsername;

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getMaintenanceDate() { return maintenanceDate; }
    public void setMaintenanceDate(String maintenanceDate) { this.maintenanceDate = maintenanceDate; }

    public String getMaintenanceType() { return maintenanceType; }
    public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTechnicianUsername() { return technicianUsername; }
    public void setTechnicianUsername(String technicianUsername) { this.technicianUsername = technicianUsername; }
}
