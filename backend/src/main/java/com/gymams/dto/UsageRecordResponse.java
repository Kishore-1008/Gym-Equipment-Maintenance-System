package com.gymams.dto;

/** One equipment's logged usage-hours reading for one day. */
public class UsageRecordResponse {
    private Long id;
    private String equipmentId;
    private String equipmentName;
    private String category;
    private String usageDate;
    private double usageHours;
    private String notes;
    private String recordedBy;
    private String recordedAt;
    private String updatedAt;

    public UsageRecordResponse(Long id, String equipmentId, String equipmentName, String category,
                                String usageDate, double usageHours, String notes,
                                String recordedBy, String recordedAt, String updatedAt) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.category = category;
        this.usageDate = usageDate;
        this.usageHours = usageHours;
        this.notes = notes;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getEquipmentId() { return equipmentId; }
    public String getEquipmentName() { return equipmentName; }
    public String getCategory() { return category; }
    public String getUsageDate() { return usageDate; }
    public double getUsageHours() { return usageHours; }
    public String getNotes() { return notes; }
    public String getRecordedBy() { return recordedBy; }
    public String getRecordedAt() { return recordedAt; }
    public String getUpdatedAt() { return updatedAt; }
}
