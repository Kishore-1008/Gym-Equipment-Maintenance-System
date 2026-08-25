package com.gymams.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
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
import com.gymams.repository.RepairRequestRepository;
import com.gymams.repository.UsageRecordRepository;

@Service
public class EquipmentService {

    private static final String CODE_PREFIX = "EQ";
    private static final Pattern CODE_PATTERN = Pattern.compile("^EQ(\\d+)$");

    /**
     * Repair statuses that make equipment unavailable. PENDING is
     * deliberately excluded — the Admin hasn't confirmed the problem yet,
     * so equipment stays usable until at least APPROVED (see Issue 10:
     * "do not automatically mark the equipment... unless the application
     * explicitly supports immediately taking unsafe equipment out of
     * service" — this app doesn't, the Admin's approval is the trigger).
     * REJECTED/COMPLETED are terminal and never block.
     */
    private static final List<RepairStatus> ACTIVE_REPAIR_STATUSES =
            List.of(RepairStatus.APPROVED, RepairStatus.ASSIGNED, RepairStatus.IN_PROGRESS);

    private final EquipmentRepository equipmentRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final MaintenanceScheduleRepository maintenanceScheduleRepository;

    public EquipmentService(
            EquipmentRepository equipmentRepository,
            UsageRecordRepository usageRecordRepository,
            RepairRequestRepository repairRequestRepository,
            MaintenanceScheduleRepository maintenanceScheduleRepository) {

        this.equipmentRepository = equipmentRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.repairRequestRepository = repairRequestRepository;
        this.maintenanceScheduleRepository = maintenanceScheduleRepository;
    }

    public List<EquipmentResponse> findAll() {
        return equipmentRepository.findAllByOrderByEquipmentCodeAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        String name = validateName(request.getEquipmentName());
        MaintenanceInterval interval = validateInterval(request.getMaintenanceInterval());
        EquipmentStatus status = (request.getStatus() == null || request.getStatus().isBlank())
                ? EquipmentStatus.OPERATIONAL
                : validateStatus(request.getStatus());

        Equipment equipment = new Equipment();
        equipment.setEquipmentCode(generateNextCode());
        equipment.setEquipmentName(name);
        equipment.setCategory(EquipmentCatalog.categoryFor(name));
        equipment.setMaintenanceInterval(interval);
        equipment.setStatus(status);
        equipment.setMonthlyUsageLimitHours(request.getMonthlyUsageLimitHours());

        return toResponse(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentResponse update(String equipmentCode, EquipmentRequest request) {
        Equipment equipment = equipmentRepository.findByEquipmentCodeIgnoreCase(equipmentCode)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Equipment not found."
                ));

        String name = validateName(request.getEquipmentName());
        MaintenanceInterval interval = validateInterval(request.getMaintenanceInterval());
        EquipmentStatus status = (request.getStatus() == null || request.getStatus().isBlank())
                ? equipment.getStatus()
                : validateStatus(request.getStatus());

        // Category is always re-derived from the (possibly new) name —
        // never trusted from the request body.
        equipment.setEquipmentName(name);
        equipment.setCategory(EquipmentCatalog.categoryFor(name));
        equipment.setMaintenanceInterval(interval);
        equipment.setStatus(status);
        equipment.setMonthlyUsageLimitHours(request.getMonthlyUsageLimitHours());

        return toResponse(equipmentRepository.save(equipment));
    }

    @Transactional
    public void delete(String equipmentCode) {
        Equipment equipment = equipmentRepository.findByEquipmentCodeIgnoreCase(equipmentCode)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Equipment not found."
                ));

        // Delete every dependent record first — usage history, repair requests
        // (Module 4), and maintenance schedules (Module 5) all hold a
        // non-nullable FK to this equipment, so database integrity would break
        // if we deleted the equipment first.
        usageRecordRepository.deleteAllByEquipment_Id(equipment.getId());
        repairRequestRepository.deleteAllByEquipment_Id(equipment.getId());
        maintenanceScheduleRepository.deleteAllByEquipment_Id(equipment.getId());

        // Now delete the equipment itself.
        equipmentRepository.delete(equipment);
    }

    /**
     * Admin's dedicated "Change Equipment Status" action (Module 4) — used
     * during the repair workflow (e.g. OPERATIONAL -> UNDER_MAINTENANCE ->
     * OPERATIONAL, or -> OUT_OF_SERVICE) without resubmitting the full
     * Equipment edit form. The Admin always decides; nothing calls this
     * automatically.
     */
    @Transactional
    public EquipmentResponse updateStatus(String equipmentCode, EquipmentStatusRequest request) {
        Equipment equipment = equipmentRepository.findByEquipmentCodeIgnoreCase(equipmentCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Equipment not found."));

        equipment.setStatus(validateStatus(request.getStatus()));
        return toResponse(equipmentRepository.save(equipment));
    }

    /**
     * Recomputes and (if needed) saves this equipment's status from its
     * currently active repair/maintenance records. Called by
     * RepairRequestService and MaintenanceService after every workflow
     * transition (approve, assign, start, complete, cancel) instead of each
     * of those services setting equipment.status directly — recomputing
     * from scratch every time, rather than incrementally patching, is what
     * makes overlapping repair/maintenance conditions resolve correctly
     * (Issue 11): nothing is ever "remembered" and restored by guesswork,
     * it's re-derived from what's actually still open.
     *
     * Priority (highest first):
     *   1. OUT_OF_SERVICE currently set -> left untouched. This is a
     *      deliberate Admin override (set only via updateStatus()/the
     *      equipment edit form) and is never auto-cleared or overwritten
     *      here, even once every repair/maintenance record closes out.
     *   2. Any repair request APPROVED/ASSIGNED/IN_PROGRESS -> UNDER_REPAIR.
     *      Repair always wins over maintenance, so an active repair is
     *      never silently overwritten by a maintenance-due/in-progress
     *      condition on the same equipment.
     *   3. Any maintenance record IN_PROGRESS -> UNDER_MAINTENANCE.
     *   4. Any maintenance record SCHEDULED (not yet started) -> MAINTENANCE_DUE.
     *   5. Otherwise -> OPERATIONAL.
     */
    @Transactional
    public void recomputeStatus(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId).orElse(null);
        if (equipment == null) {
            return; // equipment was deleted concurrently — nothing to recompute
        }

        if (equipment.getStatus() == EquipmentStatus.OUT_OF_SERVICE) {
            return;
        }

        EquipmentStatus next;
        if (repairRequestRepository.existsByEquipment_IdAndStatusIn(equipmentId, ACTIVE_REPAIR_STATUSES)) {
            next = EquipmentStatus.UNDER_REPAIR;
        } else if (maintenanceScheduleRepository.existsByEquipment_IdAndStatus(equipmentId, MaintenanceStatus.IN_PROGRESS)) {
            next = EquipmentStatus.UNDER_MAINTENANCE;
        } else if (maintenanceScheduleRepository.existsByEquipment_IdAndStatus(equipmentId, MaintenanceStatus.SCHEDULED)) {
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
     * One-time startup reconciliation: recomputes every piece of
     * equipment's status against its current repair/maintenance records.
     *
     * Why this is needed: recomputeStatus() only ever runs when
     * RepairRequestService/MaintenanceService call it, at the moment a
     * workflow transition happens. Any repair request that was approved
     * or assigned *before* that wiring existed never triggered a
     * recompute — its equipment's status column is stuck at whatever it
     * was before, even though the repair request itself is legitimately
     * ASSIGNED/IN_PROGRESS right now. Same self-healing idea as
     * UserService.backfillMissingTechnicianCodes(): reconcile stale
     * historical data once at boot, using the exact same rules
     * recomputeStatus() already applies everywhere else, rather than
     * requiring the Admin to manually notice and fix each one.
     *
     * Safe to run on every startup — recomputeStatus() is idempotent
     * (a no-op save skip when nothing actually changed) and never
     * touches equipment currently OUT_OF_SERVICE.
     */
    @PostConstruct
    public void reconcileAllEquipmentStatuses() {
        equipmentRepository.findAll().forEach(equipment -> recomputeStatus(equipment.getId()));
    }

    /**
     * Usage Monitoring guard (Issue 12): equipment that isn't normally
     * usable must reject new usage entries with a clear, specific reason.
     * Called by UsageService before creating/updating a reading — never
     * silently swallowed into a generic error.
     */
    public void requireAvailableForUsage(Equipment equipment) {
        switch (equipment.getStatus()) {
            case UNDER_REPAIR -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    equipment.getEquipmentName() + " (" + equipment.getEquipmentCode() + ") is currently under repair and cannot be used.");
            case UNDER_MAINTENANCE -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    equipment.getEquipmentName() + " (" + equipment.getEquipmentCode() + ") is currently under maintenance and cannot be used.");
            case OUT_OF_SERVICE -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    equipment.getEquipmentName() + " (" + equipment.getEquipmentCode() + ") is out of service and cannot be used.");
            default -> { /* OPERATIONAL and MAINTENANCE_DUE are both usable */ }
        }
    }

    /* ---------- validation helpers (backend is the authority) ---------- */

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
     * Generates the next EQ### code by scanning existing codes for the
     * highest numeric suffix — so a deleted EQ010 never gets reissued
     * while EQ011+ already exist, and gaps never collide.
     * Synchronized so two concurrent Add-Equipment submissions can't
     * race to the same code (fine for this app's expected load; a
     * DB sequence/counter table would be the next step at higher scale).
     */
    private synchronized String generateNextCode() {
        int max = equipmentRepository.findAll().stream()
                .map(Equipment::getEquipmentCode)
                .map(CODE_PATTERN::matcher)
                .filter(Matcher::matches)
                .mapToInt(m -> Integer.parseInt(m.group(1)))
                .max()
                .orElse(0);

        int next = max + 1;
        return CODE_PREFIX + String.format("%03d", next);
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