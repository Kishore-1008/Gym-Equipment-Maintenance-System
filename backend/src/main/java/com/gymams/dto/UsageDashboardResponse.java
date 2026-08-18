package com.gymams.dto;

import java.util.List;

/**
 * Backs the entire Gym Manager / Admin Usage Monitoring screen in one
 * call: today's date, the current month label, a per-equipment usage +
 * maintenance-status table, and the four "most/least used" highlights.
 * Deliberately excludes any lifetime-usage statistic per the product
 * requirement — only today and the current month matter here.
 */
public class UsageDashboardResponse {
    private String date;
    private String month;
    private List<EquipmentUsageResponse> equipment;

    private String mostUsedTodayEquipmentId;
    private String mostUsedTodayEquipmentName;
    private Double mostUsedTodayHours;

    private String leastUsedTodayEquipmentId;
    private String leastUsedTodayEquipmentName;
    private Double leastUsedTodayHours;

    private String mostUsedMonthEquipmentId;
    private String mostUsedMonthEquipmentName;
    private Double mostUsedMonthHours;

    private String leastUsedMonthEquipmentId;
    private String leastUsedMonthEquipmentName;
    private Double leastUsedMonthHours;

    public UsageDashboardResponse(String date, String month, List<EquipmentUsageResponse> equipment,
                                   String mostUsedTodayEquipmentId, String mostUsedTodayEquipmentName, Double mostUsedTodayHours,
                                   String leastUsedTodayEquipmentId, String leastUsedTodayEquipmentName, Double leastUsedTodayHours,
                                   String mostUsedMonthEquipmentId, String mostUsedMonthEquipmentName, Double mostUsedMonthHours,
                                   String leastUsedMonthEquipmentId, String leastUsedMonthEquipmentName, Double leastUsedMonthHours) {
        this.date = date;
        this.month = month;
        this.equipment = equipment;
        this.mostUsedTodayEquipmentId = mostUsedTodayEquipmentId;
        this.mostUsedTodayEquipmentName = mostUsedTodayEquipmentName;
        this.mostUsedTodayHours = mostUsedTodayHours;
        this.leastUsedTodayEquipmentId = leastUsedTodayEquipmentId;
        this.leastUsedTodayEquipmentName = leastUsedTodayEquipmentName;
        this.leastUsedTodayHours = leastUsedTodayHours;
        this.mostUsedMonthEquipmentId = mostUsedMonthEquipmentId;
        this.mostUsedMonthEquipmentName = mostUsedMonthEquipmentName;
        this.mostUsedMonthHours = mostUsedMonthHours;
        this.leastUsedMonthEquipmentId = leastUsedMonthEquipmentId;
        this.leastUsedMonthEquipmentName = leastUsedMonthEquipmentName;
        this.leastUsedMonthHours = leastUsedMonthHours;
    }

    public String getDate() { return date; }
    public String getMonth() { return month; }
    public List<EquipmentUsageResponse> getEquipment() { return equipment; }
    public String getMostUsedTodayEquipmentId() { return mostUsedTodayEquipmentId; }
    public String getMostUsedTodayEquipmentName() { return mostUsedTodayEquipmentName; }
    public Double getMostUsedTodayHours() { return mostUsedTodayHours; }
    public String getLeastUsedTodayEquipmentId() { return leastUsedTodayEquipmentId; }
    public String getLeastUsedTodayEquipmentName() { return leastUsedTodayEquipmentName; }
    public Double getLeastUsedTodayHours() { return leastUsedTodayHours; }
    public String getMostUsedMonthEquipmentId() { return mostUsedMonthEquipmentId; }
    public String getMostUsedMonthEquipmentName() { return mostUsedMonthEquipmentName; }
    public Double getMostUsedMonthHours() { return mostUsedMonthHours; }
    public String getLeastUsedMonthEquipmentId() { return leastUsedMonthEquipmentId; }
    public String getLeastUsedMonthEquipmentName() { return leastUsedMonthEquipmentName; }
    public Double getLeastUsedMonthHours() { return leastUsedMonthHours; }
}
