package com.gymams.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Module 4 — Repair Request Management.
 *
 * Reported by a Gym Manager for an ACTUAL PROBLEM/FAULT/DAMAGE on a piece
 * of equipment. Reviewed (approved/rejected) and assigned by the Admin,
 * then worked by the assigned Technician. Deliberately unrelated to
 * MaintenanceSchedule (Module 5) — usage-based "maintenance due" status
 * never creates or influences a RepairRequest.
 *
 * The reporter and the assigned technician are stored as denormalized
 * username/full-name (and, for the technician, technician code) fields
 * rather than a JPA relationship to User — the same "audit trail, not a
 * hard ownership FK" pattern UsageRecord already uses for recordedBy, so a
 * user account is never blocked from being managed by rows referencing it.
 */
@Entity
@Table(name = "repair_request")
public class RepairRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "problem_description", nullable = false, length = 1000)
    private String problemDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RepairStatus status;

    @Column(name = "reported_by_username", nullable = false, length = 20)
    private String reportedByUsername;

    @Column(name = "reported_by_full_name", nullable = false, length = 100)
    private String reportedByFullName;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    /** Mandatory when status = REJECTED, otherwise null. */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "assigned_technician_username", length = 20)
    private String assignedTechnicianUsername;

    @Column(name = "assigned_technician_full_name", length = 100)
    private String assignedTechnicianFullName;

    @Column(name = "assigned_technician_code", length = 10)
    private String assignedTechnicianCode;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Entered by the Technician when marking the repair COMPLETED. */
    @Column(name = "completion_details", length = 1000)
    private String completionDetails;

    public RepairRequest() {}

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) submittedAt = LocalDateTime.now();
        if (status == null) status = RepairStatus.PENDING;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }

    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }

    public RepairStatus getStatus() { return status; }
    public void setStatus(RepairStatus status) { this.status = status; }

    public String getReportedByUsername() { return reportedByUsername; }
    public void setReportedByUsername(String reportedByUsername) { this.reportedByUsername = reportedByUsername; }

    public String getReportedByFullName() { return reportedByFullName; }
    public void setReportedByFullName(String reportedByFullName) { this.reportedByFullName = reportedByFullName; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getAssignedTechnicianUsername() { return assignedTechnicianUsername; }
    public void setAssignedTechnicianUsername(String assignedTechnicianUsername) { this.assignedTechnicianUsername = assignedTechnicianUsername; }

    public String getAssignedTechnicianFullName() { return assignedTechnicianFullName; }
    public void setAssignedTechnicianFullName(String assignedTechnicianFullName) { this.assignedTechnicianFullName = assignedTechnicianFullName; }

    public String getAssignedTechnicianCode() { return assignedTechnicianCode; }
    public void setAssignedTechnicianCode(String assignedTechnicianCode) { this.assignedTechnicianCode = assignedTechnicianCode; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getCompletionDetails() { return completionDetails; }
    public void setCompletionDetails(String completionDetails) { this.completionDetails = completionDetails; }
}
