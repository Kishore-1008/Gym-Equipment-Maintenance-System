package com.gymams.dto;

public class WarrantyResponse {

    private Long id;
    private String warrantyCode;

    private String equipmentId;
    private String equipmentName;

    private String provider;
    private String warrantyType;

    private String startDate;
    private String expiryDate;

    private String coverageDetails;
    private String contactInformation;

    /** Computed by WarrantyService from expiryDate — never stored. ACTIVE / EXPIRING_SOON / EXPIRED. */
    private String status;

    /**
     * Signed day count relative to today: positive = days until expiry,
     * negative = days since it expired. The frontend turns this into
     * "142 days remaining" / "Expired 20 days ago" without needing its
     * own date math.
     */
    private long daysRemaining;

    public WarrantyResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWarrantyCode() { return warrantyCode; }
    public void setWarrantyCode(String warrantyCode) { this.warrantyCode = warrantyCode; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(long daysRemaining) { this.daysRemaining = daysRemaining; }
}