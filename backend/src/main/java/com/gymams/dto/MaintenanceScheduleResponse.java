package com.gymams.dto;

public class MaintenanceScheduleResponse {

    private Long id;
    private String equipmentId;
    private String equipmentName;
    private String maintenanceType;
    private String maintenanceDate;
    private String description;
    private String status;

    private String assignedTechnicianUsername;
    private String assignedTechnicianName;
    private String assignedTechnicianCode;

    private String scheduledByUsername;
    private String startedDate;
    private String completedDate;
    private String completionDetails;
    private String cancelledDate;

    public MaintenanceScheduleResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getMaintenanceType() { return maintenanceType; }
    public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }

    public String getMaintenanceDate() { return maintenanceDate; }
    public void setMaintenanceDate(String maintenanceDate) { this.maintenanceDate = maintenanceDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedTechnicianUsername() { return assignedTechnicianUsername; }
    public void setAssignedTechnicianUsername(String assignedTechnicianUsername) { this.assignedTechnicianUsername = assignedTechnicianUsername; }

    public String getAssignedTechnicianName() { return assignedTechnicianName; }
    public void setAssignedTechnicianName(String assignedTechnicianName) { this.assignedTechnicianName = assignedTechnicianName; }

    public String getAssignedTechnicianCode() { return assignedTechnicianCode; }
    public void setAssignedTechnicianCode(String assignedTechnicianCode) { this.assignedTechnicianCode = assignedTechnicianCode; }

    public String getScheduledByUsername() { return scheduledByUsername; }
    public void setScheduledByUsername(String scheduledByUsername) { this.scheduledByUsername = scheduledByUsername; }

    public String getStartedDate() { return startedDate; }
    public void setStartedDate(String startedDate) { this.startedDate = startedDate; }

    public String getCompletedDate() { return completedDate; }
    public void setCompletedDate(String completedDate) { this.completedDate = completedDate; }

    public String getCompletionDetails() { return completionDetails; }
    public void setCompletionDetails(String completionDetails) { this.completionDetails = completionDetails; }

    public String getCancelledDate() { return cancelledDate; }
    public void setCancelledDate(String cancelledDate) { this.cancelledDate = cancelledDate; }
}
