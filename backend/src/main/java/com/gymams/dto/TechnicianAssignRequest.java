package com.gymams.dto;

import jakarta.validation.constraints.NotBlank;

/** Admin selects a technician (by username) for a repair request or maintenance schedule. */
public class TechnicianAssignRequest {

    @NotBlank(message = "Select a technician.")
    private String technicianUsername;

    public String getTechnicianUsername() { return technicianUsername; }
    public void setTechnicianUsername(String technicianUsername) { this.technicianUsername = technicianUsername; }
}
