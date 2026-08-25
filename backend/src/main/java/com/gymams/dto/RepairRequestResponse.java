package com.gymams.dto;

public class RepairRequestResponse {

    private Long id;
    private String equipmentId;
    private String equipmentName;
    private String problemDescription;
    private String status;

    private String submittedByUsername;
    private String submittedByFullName;
    private String submittedDate;

    private String rejectionReason;
    private String reviewedDate;

    private String assignedTechnicianUsername;
    private String assignedTechnicianName;
    private String assignedTechnicianCode;
    private String assignedDate;

    private String startedDate;
    private String completedDate;
    private String completionDetails;

    public RepairRequestResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSubmittedByUsername() { return submittedByUsername; }
    public void setSubmittedByUsername(String submittedByUsername) { this.submittedByUsername = submittedByUsername; }

    public String getSubmittedByFullName() { return submittedByFullName; }
    public void setSubmittedByFullName(String submittedByFullName) { this.submittedByFullName = submittedByFullName; }

    public String getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(String submittedDate) { this.submittedDate = submittedDate; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getReviewedDate() { return reviewedDate; }
    public void setReviewedDate(String reviewedDate) { this.reviewedDate = reviewedDate; }

    public String getAssignedTechnicianUsername() { return assignedTechnicianUsername; }
    public void setAssignedTechnicianUsername(String assignedTechnicianUsername) { this.assignedTechnicianUsername = assignedTechnicianUsername; }

    public String getAssignedTechnicianName() { return assignedTechnicianName; }
    public void setAssignedTechnicianName(String assignedTechnicianName) { this.assignedTechnicianName = assignedTechnicianName; }

    public String getAssignedTechnicianCode() { return assignedTechnicianCode; }
    public void setAssignedTechnicianCode(String assignedTechnicianCode) { this.assignedTechnicianCode = assignedTechnicianCode; }

    public String getAssignedDate() { return assignedDate; }
    public void setAssignedDate(String assignedDate) { this.assignedDate = assignedDate; }

    public String getStartedDate() { return startedDate; }
    public void setStartedDate(String startedDate) { this.startedDate = startedDate; }

    public String getCompletedDate() { return completedDate; }
    public void setCompletedDate(String completedDate) { this.completedDate = completedDate; }

    public String getCompletionDetails() { return completionDetails; }
    public void setCompletionDetails(String completionDetails) { this.completionDetails = completionDetails; }
}
