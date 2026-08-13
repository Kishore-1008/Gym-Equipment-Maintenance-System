package com.gymams.dto;

public class UsageRecordResponse {
    private Long id;
    private String equipmentId;
    private String equipmentName;
    private String zone;
    private String usageDate;
    private int sessionCount;
    /** NOT_USED / NORMAL / HIGH — derived from sessionCount, never stored. */
    private String status;
    private String recordedBy;
    private String recordedAt;
    private String updatedAt;

    public UsageRecordResponse(Long id, String equipmentId, String equipmentName, String zone,
                                String usageDate, int sessionCount, String status,
                                String recordedBy, String recordedAt, String updatedAt) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.zone = zone;
        this.usageDate = usageDate;
        this.sessionCount = sessionCount;
        this.status = status;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getEquipmentId() { return equipmentId; }
    public String getEquipmentName() { return equipmentName; }
    public String getZone() { return zone; }
    public String getUsageDate() { return usageDate; }
    public int getSessionCount() { return sessionCount; }
    public String getStatus() { return status; }
    public String getRecordedBy() { return recordedBy; }
    public String getRecordedAt() { return recordedAt; }
    public String getUpdatedAt() { return updatedAt; }
}
