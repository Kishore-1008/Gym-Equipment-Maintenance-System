package com.gymams.dto;

/**
 * One equipment entry within a Most/Least Used highlight. A plain list of
 * these — rather than a single equipment — is what lets the dashboard
 * show every equipment tied for the highest (or lowest) usage instead of
 * arbitrarily picking just one when several share the same hours.
 */
public class UsageHighlightResponse {
    private String equipmentId;
    private String equipmentName;
    private double hours;

    public UsageHighlightResponse(String equipmentId, String equipmentName, double hours) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.hours = hours;
    }

    public String getEquipmentId() { return equipmentId; }
    public String getEquipmentName() { return equipmentName; }
    public double getHours() { return hours; }
}
