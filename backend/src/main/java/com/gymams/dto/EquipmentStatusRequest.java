package com.gymams.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Admin's dedicated "Change Equipment Status" action (Module 4), used during
 * the repair workflow (e.g. OPERATIONAL -> UNDER_MAINTENANCE -> OPERATIONAL,
 * or -> OUT_OF_SERVICE) without needing to resubmit the full Equipment
 * edit form.
 */
public class EquipmentStatusRequest {

    @NotBlank(message = "Select a status.")
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
