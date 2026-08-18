package com.gymams.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Body for POST /api/usage (single upsert) and PUT /api/usage/{id} (edit an existing day's reading). */
public class UsageRecordRequest {

    @NotBlank(message = "Select equipment.")
    private String equipmentId;

    /** ISO date (yyyy-MM-dd). Required on create; ignored on PUT (the id already identifies the day). */
    private String usageDate;

    @NotNull(message = "Enter usage hours.")
    @DecimalMin(value = "0", inclusive = true, message = "Usage hours can't be negative.")
    private Double usageHours;

    private String notes;

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getUsageDate() { return usageDate; }
    public void setUsageDate(String usageDate) { this.usageDate = usageDate; }

    public Double getUsageHours() { return usageHours; }
    public void setUsageHours(Double usageHours) { this.usageHours = usageHours; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
