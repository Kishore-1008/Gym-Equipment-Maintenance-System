package com.gymams.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Body for POST /api/usage/batch — one date, many equipment/usage-hours pairs, saved in one operation. */
public class BatchUsageRequest {

    @NotBlank(message = "Select a date.")
    private String usageDate;

    @NotEmpty(message = "Enter at least one equipment's usage hours.")
    @Valid
    private List<Entry> entries;

    public String getUsageDate() { return usageDate; }
    public void setUsageDate(String usageDate) { this.usageDate = usageDate; }

    public List<Entry> getEntries() { return entries; }
    public void setEntries(List<Entry> entries) { this.entries = entries; }

    public static class Entry {
        @NotBlank(message = "Select equipment.")
        private String equipmentId;

        @NotNull(message = "Enter usage hours.")
        @DecimalMin(value = "0", inclusive = true, message = "Usage hours can't be negative.")
        private Double usageHours;

        private String notes;

        public String getEquipmentId() { return equipmentId; }
        public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

        public Double getUsageHours() { return usageHours; }
        public void setUsageHours(Double usageHours) { this.usageHours = usageHours; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
