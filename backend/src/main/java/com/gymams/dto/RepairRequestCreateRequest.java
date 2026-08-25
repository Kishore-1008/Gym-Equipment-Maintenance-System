package com.gymams.dto;

import jakarta.validation.constraints.NotBlank;

/** Gym Manager's "Report a Problem" form submission. */
public class RepairRequestCreateRequest {

    @NotBlank(message = "Select equipment.")
    private String equipmentId;

    @NotBlank(message = "Enter a problem description.")
    private String problemDescription;

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }
}
