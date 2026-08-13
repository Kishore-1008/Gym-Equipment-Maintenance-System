package com.gymams.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Body for POST /api/usage/batch — one date, many equipment/session-count pairs, saved in one operation. */
public class BatchUsageRequest {

    @NotBlank(message = "Select a date.")
    private String usageDate;

    @NotEmpty(message = "Enter at least one equipment's sessions.")
    @Valid
    private List<Entry> entries;

    public String getUsageDate() { return usageDate; }
    public void setUsageDate(String usageDate) { this.usageDate = usageDate; }

    public List<Entry> getEntries() { return entries; }
    public void setEntries(List<Entry> entries) { this.entries = entries; }

    public static class Entry {
        @NotBlank(message = "Select equipment.")
        private String equipmentId;

        @NotNull(message = "Enter a session count.")
        @Min(value = 0, message = "Session count can't be negative.")
        private Integer sessionCount;

        public String getEquipmentId() { return equipmentId; }
        public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

        public Integer getSessionCount() { return sessionCount; }
        public void setSessionCount(Integer sessionCount) { this.sessionCount = sessionCount; }
    }
}
