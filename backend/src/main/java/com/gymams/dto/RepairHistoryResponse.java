package com.gymams.dto;

import java.math.BigDecimal;

public class RepairHistoryResponse {

    private Long id;
    private Long repairRequestId;

    private String equipmentId;
    private String equipmentName;

    private String technicianUsername;
    private String technicianName;
    private String technicianCode;

    private String problemDescription;
    private String repairDetails;
    private String partsUsed;
    private BigDecimal repairCost;
    private String completionNotes;

    private String completedDate;

    public RepairHistoryResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRepairRequestId() { return repairRequestId; }
    public void setRepairRequestId(Long repairRequestId) { this.repairRequestId = repairRequestId; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getTechnicianUsername() { return technicianUsername; }
    public void setTechnicianUsername(String technicianUsername) { this.technicianUsername = technicianUsername; }

    public String getTechnicianName() { return technicianName; }
    public void setTechnicianName(String technicianName) { this.technicianName = technicianName; }

    public String getTechnicianCode() { return technicianCode; }
    public void setTechnicianCode(String technicianCode) { this.technicianCode = technicianCode; }

    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }

    public String getRepairDetails() { return repairDetails; }
    public void setRepairDetails(String repairDetails) { this.repairDetails = repairDetails; }

    public String getPartsUsed() { return partsUsed; }
    public void setPartsUsed(String partsUsed) { this.partsUsed = partsUsed; }

    public BigDecimal getRepairCost() { return repairCost; }
    public void setRepairCost(BigDecimal repairCost) { this.repairCost = repairCost; }

    public String getCompletionNotes() { return completionNotes; }
    public void setCompletionNotes(String completionNotes) { this.completionNotes = completionNotes; }

    public String getCompletedDate() { return completedDate; }
    public void setCompletedDate(String completedDate) { this.completedDate = completedDate; }
}