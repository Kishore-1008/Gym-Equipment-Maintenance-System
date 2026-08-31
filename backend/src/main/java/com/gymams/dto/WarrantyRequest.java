package com.gymams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Add Warranty and Update Warranty submit the same set of fields, so this
 * one DTO backs both endpoints — same convention as
 * MaintenanceScheduleCreateRequest, which similarly doesn't need a
 * separate "update" shape since nothing here is a workflow-status field.
 */
public class WarrantyRequest {

    @NotBlank(message = "Select equipment.")
    private String equipmentId;

    @NotBlank(message = "Enter the warranty provider.")
    private String provider;

    @NotBlank(message = "Select a warranty type.")
    private String warrantyType;

    @NotNull(message = "Select a start date.")
    private String startDate; // ISO yyyy-MM-dd

    @NotNull(message = "Select an expiry date.")
    private String expiryDate; // ISO yyyy-MM-dd

    private String coverageDetails;

    private String contactInformation;

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getWarrantyType() { return warrantyType; }
    public void setWarrantyType(String warrantyType) { this.warrantyType = warrantyType; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getCoverageDetails() { return coverageDetails; }
    public void setCoverageDetails(String coverageDetails) { this.coverageDetails = coverageDetails; }

    public String getContactInformation() { return contactInformation; }
    public void setContactInformation(String contactInformation) { this.contactInformation = contactInformation; }
}