package com.gymams.dto;

/** One row of the Top Used Equipment ranking. */
public class TopUsageResponse {
    private String equipmentId;
    private String equipmentName;
    private String zone;
    private int totalSessions;

    public TopUsageResponse(String equipmentId, String equipmentName, String zone, int totalSessions) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.zone = zone;
        this.totalSessions = totalSessions;
    }

    public String getEquipmentId() { return equipmentId; }
    public String getEquipmentName() { return equipmentName; }
    public String getZone() { return zone; }
    public int getTotalSessions() { return totalSessions; }
}
