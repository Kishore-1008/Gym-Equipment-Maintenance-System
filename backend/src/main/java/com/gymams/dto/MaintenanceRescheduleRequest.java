package com.gymams.dto;

import jakarta.validation.constraints.NotNull;

/** Admin's "Reschedule Maintenance" action — new date for a still-open maintenance record. */
public class MaintenanceRescheduleRequest {

    @NotNull(message = "Select a new maintenance date.")
    private String maintenanceDate; // ISO yyyy-MM-dd

    public String getMaintenanceDate() { return maintenanceDate; }
    public void setMaintenanceDate(String maintenanceDate) { this.maintenanceDate = maintenanceDate; }
}
