package com.gymams.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One equipment's TOTAL USAGE HOURS for one day, manually entered by the
 * Gym Manager. (equipment, usageDate) is the business key — at most one
 * record per equipment per day, updated in place rather than re-logged,
 * so historical days are never lost or duplicated.
 *
 * Deliberately just a daily hours reading: no sessions, no start/end
 * time, no IoT/sensor/meter data, no automatic tracking of any kind.
 */
@Entity
@Table(name = "usage_record", uniqueConstraints = @UniqueConstraint(columnNames = {"equipment_id", "usage_date"}))
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "usage_hours", nullable = false)
    private double usageHours;

    @Column(name = "notes", length = 255)
    private String notes;

    /** Username of whoever last saved this record — audit trail only, not an ownership restriction. */
    @Column(name = "recorded_by", nullable = false, length = 20)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UsageRecord() {}

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
        updatedAt = recordedAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }

    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate usageDate) { this.usageDate = usageDate; }

    public double getUsageHours() { return usageHours; }
    public void setUsageHours(double usageHours) { this.usageHours = usageHours; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
