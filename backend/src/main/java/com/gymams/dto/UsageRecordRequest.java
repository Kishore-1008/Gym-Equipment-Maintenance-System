package com.gymams.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Body for POST /api/usage (single upsert) and PUT /api/usage/{id} (edit an existing record's count). */
public class UsageRecordRequest {

    @NotBlank(message = "Select equipment.")
    private String equipmentId;

    /** ISO date (yyyy-MM-dd). Required on create; ignored on PUT (the id already identifies the day). */
    private String usageDate;

    @NotNull(message = "Enter a session count.")
    @Min(value = 0, message = "Session count can't be negative.")
    private Integer sessionCount;

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getUsageDate() { return usageDate; }
    public void setUsageDate(String usageDate) { this.usageDate = usageDate; }

    public Integer getSessionCount() { return sessionCount; }
    public void setSessionCount(Integer sessionCount) { this.sessionCount = sessionCount; }
}
