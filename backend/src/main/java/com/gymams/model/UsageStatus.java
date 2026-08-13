package com.gymams.model;

/**
 * Usage status is never stored — it's always derived from a day's
 * session_count. The two thresholds below are the only numbers that
 * define the mapping; change them here to retune what counts as
 * "Normal" vs "High" for the whole app.
 */
public enum UsageStatus {
    NOT_USED("Not Used"),
    NORMAL("Normal"),
    HIGH("High");

    /** sessionCount == 0 -> NOT_USED. Below this -> NORMAL. */
    public static final int HIGH_THRESHOLD = 7;

    private final String label;

    UsageStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static UsageStatus fromSessionCount(int sessionCount) {
        if (sessionCount <= 0) return NOT_USED;
        if (sessionCount >= HIGH_THRESHOLD) return HIGH;
        return NORMAL;
    }
}
