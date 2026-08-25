package com.gymams.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Technician's "Mark Completed" submission for a repair request (Module 6).
 * Deliberately a separate DTO from the shared CompletionDetailsRequest used
 * by Maintenance completion (Module 5) — that one is untouched by this
 * module and still takes a single completionDetails field.
 */
public class RepairCompletionDetailsRequest {

    @NotBlank(message = "Enter repair details / work performed.")
    private String repairDetails;

    @NotBlank(message = "Enter parts used, or \"No parts used\" if none.")
    private String partsUsed;

    @NotNull(message = "Enter the repair cost (0 if none).")
    @DecimalMin(value = "0.0", message = "Repair cost cannot be negative.")
    private BigDecimal repairCost;

    /** Optional. */
    private String completionNotes;

    public String getRepairDetails() { return repairDetails; }
    public void setRepairDetails(String repairDetails) { this.repairDetails = repairDetails; }

    public String getPartsUsed() { return partsUsed; }
    public void setPartsUsed(String partsUsed) { this.partsUsed = partsUsed; }

    public BigDecimal getRepairCost() { return repairCost; }
    public void setRepairCost(BigDecimal repairCost) { this.repairCost = repairCost; }

    public String getCompletionNotes() { return completionNotes; }
    public void setCompletionNotes(String completionNotes) { this.completionNotes = completionNotes; }
}