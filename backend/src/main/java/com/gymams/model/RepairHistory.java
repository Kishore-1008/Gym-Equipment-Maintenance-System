package com.gymams.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Module 6 — Repair History.
 *
 * A permanent, read-only-for-Admin record created the moment a Technician
 * completes a repair (see RepairRequestService.complete() ->
 * RepairHistoryService.recordCompletion()). Exactly one history row per
 * completed RepairRequest — enforced both here (unique repair_request_id)
 * and in RepairHistoryService (existsByRepairRequest_Id guard) so a
 * completed repair can never accumulate duplicate history records.
 *
 * Denormalizes equipment/technician/problem details at completion time —
 * same "audit trail, not a hard FK to User" pattern RepairRequest and
 * MaintenanceSchedule already use — so this record stays fully readable
 * even if the technician's account or the equipment's live fields change
 * later. It still keeps a real FK to both RepairRequest (its origin) and
 * Equipment (for direct querying/cascade-delete), matching how
 * RepairRequest and MaintenanceSchedule already reference Equipment.
 */
@Entity
@Table(name = "repair_history")
public class RepairHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repair_request_id", nullable = false, unique = true)
    private RepairRequest repairRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "equipment_name", nullable = false, length = 100)
    private String equipmentName;

    @Column(name = "equipment_code", nullable = false, length = 20)
    private String equipmentCode;

    @Column(name = "problem_description", nullable = false, length = 1000)
    private String problemDescription;

    @Column(name = "technician_username", nullable = false, length = 20)
    private String technicianUsername;

    @Column(name = "technician_full_name", nullable = false, length = 100)
    private String technicianFullName;

    @Column(name = "technician_code", length = 10)
    private String technicianCode;

    /** Repair details / work performed. */
    @Column(name = "repair_details", nullable = false, length = 1000)
    private String repairDetails;

    /** Free-text — e.g. "Treadmill belt", "Bolts and screws", "No parts used". */
    @Column(name = "parts_used", nullable = false, length = 500)
    private String partsUsed;

    @Column(name = "repair_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal repairCost;

    @Column(name = "completion_notes", length = 1000)
    private String completionNotes;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RepairHistory() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RepairRequest getRepairRequest() { return repairRequest; }
    public void setRepairRequest(RepairRequest repairRequest) { this.repairRequest = repairRequest; }

    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }

    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }

    public String getTechnicianUsername() { return technicianUsername; }
    public void setTechnicianUsername(String technicianUsername) { this.technicianUsername = technicianUsername; }

    public String getTechnicianFullName() { return technicianFullName; }
    public void setTechnicianFullName(String technicianFullName) { this.technicianFullName = technicianFullName; }

    public String getTechnicianCode() { return technicianCode; }
    public void setTechnicianCode(String technicianCode) { this.technicianCode = technicianCode; }

    public String getRepairDetails() { return repairDetails; }
    public void setRepairDetails(String repairDetails) { this.repairDetails = repairDetails; }

    public String getPartsUsed() { return partsUsed; }
    public void setPartsUsed(String partsUsed) { this.partsUsed = partsUsed; }

    public BigDecimal getRepairCost() { return repairCost; }
    public void setRepairCost(BigDecimal repairCost) { this.repairCost = repairCost; }

    public String getCompletionNotes() { return completionNotes; }
    public void setCompletionNotes(String completionNotes) { this.completionNotes = completionNotes; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}