package com.gymams.dto;

/** One point in an equipment's usage history/trend — a date (or period) and its session count. */
public class UsageHistoryPointResponse {
    private String label;
    private int sessionCount;

    public UsageHistoryPointResponse(String label, int sessionCount) {
        this.label = label;
        this.sessionCount = sessionCount;
    }

    public String getLabel() { return label; }
    public int getSessionCount() { return sessionCount; }
}
