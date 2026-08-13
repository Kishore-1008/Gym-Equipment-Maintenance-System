package com.gymams.service;

import com.gymams.dto.EquipmentRequest;
import com.gymams.dto.EquipmentResponse;
import com.gymams.exception.ApiException;
import com.gymams.model.Equipment;
import com.gymams.model.EquipmentStatus;
import com.gymams.model.MaintenanceInterval;
import com.gymams.repository.EquipmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EquipmentService {

    private static final String CODE_PREFIX = "EQ";
    private static final Pattern CODE_PATTERN = Pattern.compile("^EQ(\\d+)$");

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
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

        return toResponse(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentResponse update(String equipmentCode, EquipmentRequest request) {
        Equipment equipment = equipmentRepository.findByEquipmentCodeIgnoreCase(equipmentCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Equipment not found."));

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

        return toResponse(equipmentRepository.save(equipment));
    }

    @Transactional
    public void delete(String equipmentCode) {
        Equipment equipment = equipmentRepository.findByEquipmentCodeIgnoreCase(equipmentCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Equipment not found."));
        equipmentRepository.delete(equipment);
    }

    /* ---------- validation helpers (backend is the authority) ---------- */

    private String validateName(String name) {
        if (!EquipmentCatalog.isAllowedName(name)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select a valid equipment name.");
        }
        return name;
    }

    private MaintenanceInterval validateInterval(String interval) {
        try {
            return MaintenanceInterval.valueOf(interval);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select a valid maintenance interval.");
        }
    }

    private EquipmentStatus validateStatus(String status) {
        try {
            return EquipmentStatus.valueOf(status);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select a valid status.");
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
                e.getStatus().name()
        );
    }
}
