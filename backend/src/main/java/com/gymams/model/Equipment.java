package com.gymams.model;

import jakarta.persistence.*;

@Entity
@Table(name = "equipment", uniqueConstraints = @UniqueConstraint(columnNames = "equipment_code"))
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Business-facing identifier shown in the UI, e.g. EQ001, EQ002 ... */
    @Column(name = "equipment_code", nullable = false, unique = true, length = 10)
    private String equipmentCode;

    @Column(name = "equipment_name", nullable = false, length = 60)
    private String equipmentName;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_interval", nullable = false, length = 20)
    private MaintenanceInterval maintenanceInterval;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EquipmentStatus status;

    public Equipment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public MaintenanceInterval getMaintenanceInterval() { return maintenanceInterval; }
    public void setMaintenanceInterval(MaintenanceInterval maintenanceInterval) { this.maintenanceInterval = maintenanceInterval; }

    public EquipmentStatus getStatus() { return status; }
    public void setStatus(EquipmentStatus status) { this.status = status; }
}
