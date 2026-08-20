package com.gymams.dto;

import java.util.List;

/**
 * Backs the entire Gym Manager / Admin Usage Monitoring screen in one
 * call: today's date, the current month label, a per-equipment usage +
 * maintenance-status table, and the four "most/least used" highlights.
 * Deliberately excludes any lifetime-usage statistic per the product
 * requirement — only today and the current month matter here.
 *
 * Each highlight is a LIST, not a single equipment: when several pieces
 * of equipment are tied for the highest (or lowest) usage, every one of
 * them belongs in the highlight — never just the first one encountered.
 */
public class UsageDashboardResponse {
    private String date;
    private String month;
    private List<EquipmentUsageResponse> equipment;

    private List<UsageHighlightResponse> mostUsedToday;
    private List<UsageHighlightResponse> leastUsedToday;
    private List<UsageHighlightResponse> mostUsedMonth;
    private List<UsageHighlightResponse> leastUsedMonth;

    public UsageDashboardResponse(String date, String month, List<EquipmentUsageResponse> equipment,
                                   List<UsageHighlightResponse> mostUsedToday,
                                   List<UsageHighlightResponse> leastUsedToday,
                                   List<UsageHighlightResponse> mostUsedMonth,
                                   List<UsageHighlightResponse> leastUsedMonth) {
        this.date = date;
        this.month = month;
        this.equipment = equipment;
        this.mostUsedToday = mostUsedToday;
        this.leastUsedToday = leastUsedToday;
        this.mostUsedMonth = mostUsedMonth;
        this.leastUsedMonth = leastUsedMonth;
    }

    public String getDate() { return date; }
    public String getMonth() { return month; }
    public List<EquipmentUsageResponse> getEquipment() { return equipment; }
    public List<UsageHighlightResponse> getMostUsedToday() { return mostUsedToday; }
    public List<UsageHighlightResponse> getLeastUsedToday() { return leastUsedToday; }
    public List<UsageHighlightResponse> getMostUsedMonth() { return mostUsedMonth; }
    public List<UsageHighlightResponse> getLeastUsedMonth() { return leastUsedMonth; }
}
