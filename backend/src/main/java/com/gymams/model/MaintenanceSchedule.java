package com.gymams.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Module 5 — Maintenance Management.
 *
 * SCHEDULED/PREVENTIVE servicing, created and assigned entirely by the
 * Admin — the Gym Manager has no role here. Deliberately unrelated to
 * RepairRequest (Module 4): usage limits shown in Usage Monitoring may
 * indicate maintenance is "due", but nothing ever auto-creates a row here
 * or auto-assigns a technician — the Admin always decides.
 */
@Entity
@Table(name = "maintenance_schedule")
public class MaintenanceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 30)
    private MaintenanceType maintenanceType;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MaintenanceStatus status;

    @Column(name = "assigned_technician_username", nullable = false, length = 20)
    private String assignedTechnicianUsername;

    @Column(name = "assigned_technician_full_name", nullable = false, length = 100)
    private String assignedTechnicianFullName;

    @Column(name = "assigned_technician_code", length = 10)
    private String assignedTechnicianCode;

    @Column(name = "scheduled_by_username", nullable = false, length = 20)
    private String scheduledByUsername;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Entered by the Technician when marking maintenance COMPLETED. */
    @Column(name = "completion_details", length = 1000)
    private String completionDetails;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public MaintenanceSchedule() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) status = MaintenanceStatus.SCHEDULED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }

    public LocalDate getMaintenanceDate() { return maintenanceDate; }
    public void setMaintenanceDate(LocalDate maintenanceDate) { this.maintenanceDate = maintenanceDate; }

    public MaintenanceType getMaintenanceType() { return maintenanceType; }
    public void setMaintenanceType(MaintenanceType maintenanceType) { this.maintenanceType = maintenanceType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public MaintenanceStatus getStatus() { return status; }
    public void setStatus(MaintenanceStatus status) { this.status = status; }

    public String getAssignedTechnicianUsername() { return assignedTechnicianUsername; }
    public void setAssignedTechnicianUsername(String assignedTechnicianUsername) { this.assignedTechnicianUsername = assignedTechnicianUsername; }

    public String getAssignedTechnicianFullName() { return assignedTechnicianFullName; }
    public void setAssignedTechnicianFullName(String assignedTechnicianFullName) { this.assignedTechnicianFullName = assignedTechnicianFullName; }

    public String getAssignedTechnicianCode() { return assignedTechnicianCode; }
    public void setAssignedTechnicianCode(String assignedTechnicianCode) { this.assignedTechnicianCode = assignedTechnicianCode; }

    public String getScheduledByUsername() { return scheduledByUsername; }
    public void setScheduledByUsername(String scheduledByUsername) { this.scheduledByUsername = scheduledByUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getCompletionDetails() { return completionDetails; }
    public void setCompletionDetails(String completionDetails) { this.completionDetails = completionDetails; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
