package com.gymams.dto;

/** Backs the Usage Monitoring summary cards for a single selected date. */
public class UsageSummaryResponse {
    private String date;
    private int totalSessions;
    private int totalEquipment;
    private String mostUsedEquipmentId;
    private String mostUsedEquipmentName;
    private Integer mostUsedSessions;
    private String leastUsedEquipmentId;
    private String leastUsedEquipmentName;
    private Integer leastUsedSessions;

    public UsageSummaryResponse(String date, int totalSessions, int totalEquipment,
                                 String mostUsedEquipmentId, String mostUsedEquipmentName, Integer mostUsedSessions,
                                 String leastUsedEquipmentId, String leastUsedEquipmentName, Integer leastUsedSessions) {
        this.date = date;
        this.totalSessions = totalSessions;
        this.totalEquipment = totalEquipment;
        this.mostUsedEquipmentId = mostUsedEquipmentId;
        this.mostUsedEquipmentName = mostUsedEquipmentName;
        this.mostUsedSessions = mostUsedSessions;
        this.leastUsedEquipmentId = leastUsedEquipmentId;
        this.leastUsedEquipmentName = leastUsedEquipmentName;
        this.leastUsedSessions = leastUsedSessions;
    }

    public String getDate() { return date; }
    public int getTotalSessions() { return totalSessions; }
    public int getTotalEquipment() { return totalEquipment; }
    public String getMostUsedEquipmentId() { return mostUsedEquipmentId; }
    public String getMostUsedEquipmentName() { return mostUsedEquipmentName; }
    public Integer getMostUsedSessions() { return mostUsedSessions; }
    public String getLeastUsedEquipmentId() { return leastUsedEquipmentId; }
    public String getLeastUsedEquipmentName() { return leastUsedEquipmentName; }
    public Integer getLeastUsedSessions() { return leastUsedSessions; }
}
