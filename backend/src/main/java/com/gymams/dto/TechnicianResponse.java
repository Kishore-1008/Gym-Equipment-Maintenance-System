package com.gymams.dto;

/**
 * One entry in the technician-selection dropdown (Module 4 "Assign a
 * Technician" / Module 5 "Assign Technician"). displayLabel is pre-formatted
 * as "Name — TECH001" per the spec so the frontend never has to rebuild it.
 */
public class TechnicianResponse {

    private String username;
    private String fullName;
    private String technicianCode;
    private String displayLabel;

    public TechnicianResponse(String username, String fullName, String technicianCode) {
        this.username = username;
        this.fullName = fullName;
        this.technicianCode = technicianCode;
        this.displayLabel = fullName + " — " + technicianCode;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getTechnicianCode() { return technicianCode; }
    public String getDisplayLabel() { return displayLabel; }
}
