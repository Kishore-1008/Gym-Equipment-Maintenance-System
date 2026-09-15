package com.gymams.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gymams.dto.EquipmentRequest;
import com.gymams.dto.EquipmentResponse;
import com.gymams.dto.EquipmentStatusRequest;
import com.gymams.exception.ApiException;
import com.gymams.model.Equipment;
import com.gymams.model.EquipmentStatus;
import com.gymams.model.MaintenanceInterval;
import com.gymams.model.MaintenanceStatus;
import com.gymams.model.RepairStatus;
import com.gymams.repository.EquipmentRepository;
import com.gymams.repository.MaintenanceScheduleRepository;
import com.gymams.repository.RepairHistoryRepository;
import com.gymams.repository.RepairRequestRepository;
import com.gymams.repository.UsageRecordRepository;
import com.gymams.repository.WarrantyRepository;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;

@Service
public class EquipmentService {

    private static final String CODE_PREFIX = "EQ";
    private static final Pattern CODE_PATTERN = Pattern.compile("^EQ(\\d+)$");

    /**
     * Repair statuses that make equipment unavailable.
     */
    private static final List<RepairStatus> ACTIVE_REPAIR_STATUSES =
            List.of(
                    RepairStatus.APPROVED,
                    RepairStatus.ASSIGNED,
                    RepairStatus.IN_PROGRESS
            );

    private final EquipmentRepository equipmentRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final MaintenanceScheduleRepository maintenanceScheduleRepository;
    private final RepairHistoryRepository repairHistoryRepository;
    private final WarrantyRepository warrantyRepository;
    private final EntityManager entityManager;

    public EquipmentService(
            EquipmentRepository equipmentRepository,
            UsageRecordRepository usageRecordRepository,
            RepairRequestRepository repairRequestRepository,
            MaintenanceScheduleRepository maintenanceScheduleRepository,
            RepairHistoryRepository repairHistoryRepository,
            WarrantyRepository warrantyRepository,
            EntityManager entityManager) {

        this.equipmentRepository = equipmentRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.repairRequestRepository = repairRequestRepository;
        this.maintenanceScheduleRepository = maintenanceScheduleRepository;
        this.repairHistoryRepository = repairHistoryRepository;
        this.warrantyRepository = warrantyRepository;
        this.entityManager = entityManager;
    }

    public List<EquipmentResponse> findAll() {
        return equipmentRepository.findAllByOrderByEquipmentCodeAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {

        String name = validateName(request.getEquipmentName());

        MaintenanceInterval interval =
                validateInterval(request.getMaintenanceInterval());

        EquipmentStatus status =
                (request.getStatus() == null || request.getStatus().isBlank())
                        ? EquipmentStatus.OPERATIONAL
                        : validateStatus(request.getStatus());

        Equipment equipment = new Equipment();

        equipment.setEquipmentCode(generateNextCode());
        equipment.setEquipmentName(name);
        equipment.setCategory(EquipmentCatalog.categoryFor(name));
        equipment.setMaintenanceInterval(interval);
        equipment.setStatus(status);
        equipment.setMonthlyUsageLimitHours(
                request.getMonthlyUsageLimitHours()
        );

        return toResponse(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentResponse update(
            String equipmentCode,
            EquipmentRequest request) {

        Equipment equipment =
                equipmentRepository
                        .findByEquipmentCodeIgnoreCase(equipmentCode)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Equipment not found."
                        ));

        String name = validateName(request.getEquipmentName());

        MaintenanceInterval interval =
                validateInterval(request.getMaintenanceInterval());

        EquipmentStatus status =
                (request.getStatus() == null || request.getStatus().isBlank())
                        ? equipment.getStatus()
                        : validateStatus(request.getStatus());

        // Category is always derived from the equipment name.
        equipment.setEquipmentName(name);
        equipment.setCategory(EquipmentCatalog.categoryFor(name));
        equipment.setMaintenanceInterval(interval);
        equipment.setStatus(status);
        equipment.setMonthlyUsageLimitHours(
                request.getMonthlyUsageLimitHours()
        );

        return toResponse(equipmentRepository.save(equipment));
    }

    /**
     * Deletes an equipment and all records that depend on it.
     *
     * Deletion order:
     *
     * Usage records
     *      ↓
     * Repair history
     *      ↓
     * Repair requests
     *      ↓
     * Maintenance schedules
     *      ↓
     * Warranty
     *      ↓
     * Equipment
     *
     * If this was the LAST equipment in the system, the repair_request
     * AUTO_INCREMENT value is reset so that the next repair request
     * starts from ID 1 again.
     */
    @Transactional
    public void delete(String equipmentCode) {

        Equipment equipment =
                equipmentRepository
                        .findByEquipmentCodeIgnoreCase(equipmentCode)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Equipment not found."
                        ));

        Long equipmentId = equipment.getId();

        /*
         * Repair history must be deleted BEFORE repair requests because
         * repair_history.repair_request_id references repair_request.
         */
        repairHistoryRepository.deleteAllByEquipment_Id(equipmentId);

        /*
         * Delete usage records belonging to this equipment.
         */
        usageRecordRepository.deleteAllByEquipment_Id(equipmentId);

        /*
         * Delete repair requests belonging to this equipment.
         */
        repairRequestRepository.deleteAllByEquipment_Id(equipmentId);

        /*
         * Delete maintenance schedules belonging to this equipment.
         */
        maintenanceScheduleRepository.deleteAllByEquipment_Id(equipmentId);

        /*
         * Delete warranty records belonging to this equipment.
         */
        warrantyRepository.deleteAllByEquipment_Id(equipmentId);

        /*
         * Now the equipment itself can safely be deleted.
         */
        equipmentRepository.delete(equipment);

        /*
         * Flush the deletes to MySQL before checking whether this was
         * the last equipment.
         */
        equipmentRepository.flush();

        /*
         * If no equipment remains, reset repair request numbering.
         *
         * This means:
         *
         * Equipment count > 0
         *     → keep existing repair request numbering
         *
         * Equipment count = 0
         *     → reset repair_request AUTO_INCREMENT to 1
         */
        if (equipmentRepository.count() == 0) {

            entityManager
                    .createNativeQuery(
                            "ALTER TABLE repair_request AUTO_INCREMENT = 1"
                    )
                    .executeUpdate();
        }
    }

    /**
     * Admin's dedicated Change Equipment Status action.
     */
    @Transactional
    public EquipmentResponse updateStatus(
            String equipmentCode,
            EquipmentStatusRequest request) {

        Equipment equipment =
                equipmentRepository
                        .findByEquipmentCodeIgnoreCase(equipmentCode)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "Equipment not found."
                        ));

        equipment.setStatus(
                validateStatus(request.getStatus())
        );

        return toResponse(equipmentRepository.save(equipment));
    }

    /**
     * Recomputes equipment status from currently active
     * repair and maintenance records.
     *
     * Priority:
     *
     * 1. OUT_OF_SERVICE
     * 2. UNDER_REPAIR
     * 3. UNDER_MAINTENANCE
     * 4. MAINTENANCE_DUE
     * 5. OPERATIONAL
     */
    @Transactional
    public void recomputeStatus(Long equipmentId) {

        Equipment equipment =
                equipmentRepository.findById(equipmentId).orElse(null);

        if (equipment == null) {
            return;
        }

        /*
         * OUT_OF_SERVICE is an explicit Admin decision.
         * Do not automatically overwrite it.
         */
        if (equipment.getStatus() == EquipmentStatus.OUT_OF_SERVICE) {
            return;
        }

        EquipmentStatus next;

        /*
         * Active repair takes priority over maintenance.
         */
        if (repairRequestRepository
                .existsByEquipment_IdAndStatusIn(
                        equipmentId,
                        ACTIVE_REPAIR_STATUSES)) {

            next = EquipmentStatus.UNDER_REPAIR;

        } else if (maintenanceScheduleRepository
                .existsByEquipment_IdAndStatus(
                        equipmentId,
                        MaintenanceStatus.IN_PROGRESS)) {

            next = EquipmentStatus.UNDER_MAINTENANCE;

        } else if (maintenanceScheduleRepository
                .existsByEquipment_IdAndStatus(
                        equipmentId,
                        MaintenanceStatus.SCHEDULED)) {

            next = EquipmentStatus.MAINTENANCE_DUE;

        } else {

            next = EquipmentStatus.OPERATIONAL;
        }

        if (next != equipment.getStatus()) {

            equipment.setStatus(next);
            equipmentRepository.save(equipment);
        }
    }

    /**
     * Reconcile equipment statuses when the application starts.
     */
    @PostConstruct
    public void reconcileAllEquipmentStatuses() {

        equipmentRepository
                .findAll()
                .forEach(equipment ->
                        recomputeStatus(equipment.getId())
                );
    }

    /**
     * Prevent usage logging for equipment that is not available.
     */
    public void requireAvailableForUsage(Equipment equipment) {

        switch (equipment.getStatus()) {

            case UNDER_REPAIR ->
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            equipment.getEquipmentName()
                                    + " ("
                                    + equipment.getEquipmentCode()
                                    + ") is currently under repair and cannot be used."
                    );

            case UNDER_MAINTENANCE ->
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            equipment.getEquipmentName()
                                    + " ("
                                    + equipment.getEquipmentCode()
                                    + ") is currently under maintenance and cannot be used."
                    );

            case OUT_OF_SERVICE ->
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            equipment.getEquipmentName()
                                    + " ("
                                    + equipment.getEquipmentCode()
                                    + ") is out of service and cannot be used."
                    );

            default -> {
                // OPERATIONAL and MAINTENANCE_DUE are usable.
            }
        }
    }

    /* ---------- validation helpers ---------- */

    private String validateName(String name) {

        if (!EquipmentCatalog.isAllowedName(name)) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Select a valid equipment name."
            );
        }

        return name;
    }

    private MaintenanceInterval validateInterval(String interval) {

        try {

            return MaintenanceInterval.valueOf(interval);

        } catch (Exception e) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Select a valid maintenance interval."
            );
        }
    }

    private EquipmentStatus validateStatus(String status) {

        try {

            return EquipmentStatus.valueOf(status);

        } catch (Exception e) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Select a valid status."
            );
        }
    }

    /**
     * Generates the next equipment code.
     *
     * EQ001, EQ002, EQ003...
     *
     * A deleted EQ002 is not reused while higher equipment IDs exist.
     */
    private synchronized String generateNextCode() {

        int max =
                equipmentRepository
                        .findAll()
                        .stream()
                        .map(Equipment::getEquipmentCode)
                        .map(CODE_PATTERN::matcher)
                        .filter(Matcher::matches)
                        .mapToInt(m ->
                                Integer.parseInt(m.group(1))
                        )
                        .max()
                        .orElse(0);

        int next = max + 1;

        return CODE_PREFIX
                + String.format("%03d", next);
    }

    private EquipmentResponse toResponse(Equipment e) {

        return new EquipmentResponse(
                e.getEquipmentCode(),
                e.getEquipmentName(),
                e.getCategory(),
                e.getMaintenanceInterval().name(),
                e.getStatus().name(),
                e.getMonthlyUsageLimitHours()
        );
    }
}