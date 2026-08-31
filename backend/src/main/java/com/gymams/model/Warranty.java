package com.gymams.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Module 7 — Warranty Management.
 *
 * References the existing Equipment entity via a real FK rather than
 * duplicating equipment name/category/etc. here — same convention as
 * RepairRequest/MaintenanceSchedule/RepairHistory, all of which hold a
 * direct @ManyToOne to Equipment. An equipment item may have zero, one,
 * or several warranty records (e.g. an expired manufacturer warranty
 * followed later by a separately purchased extended warranty) — this is
 * intentionally NOT a one-to-one relationship.
 *
 * Warranty status (ACTIVE / EXPIRING_SOON / EXPIRED) is deliberately not
 * a column here — see WarrantyStatus.java — it's computed from expiryDate
 * on every read in WarrantyService instead of stored.
 */
@Entity
@Table(name = "warranty")
public class Warranty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Business-facing identifier shown in the UI, e.g. WAR001, WAR002 ... */
    @Column(name = "warranty_code", nullable = false, unique = true, length = 10)
    private String warrantyCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "provider", nullable = false, length = 100)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "warranty_type", nullable = false, length = 20)
    private WarrantyType warrantyType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "coverage_details", length = 1000)
    private String coverageDetails;

    @Column(name = "contact_information", length = 200)
    private String contactInformation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Warranty() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWarrantyCode() { return warrantyCode; }
    public void setWarrantyCode(String warrantyCode) { this.warrantyCode = warrantyCode; }

    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public WarrantyType getWarrantyType() { return warrantyType; }
    public void setWarrantyType(WarrantyType warrantyType) { this.warrantyType = warrantyType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getCoverageDetails() { return coverageDetails; }
    public void setCoverageDetails(String coverageDetails) { this.coverageDetails = coverageDetails; }

    public String getContactInformation() { return contactInformation; }
    public void setContactInformation(String contactInformation) { this.contactInformation = contactInformation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}