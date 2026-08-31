package com.gymams.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gymams.dto.WarrantyRequest;
import com.gymams.dto.WarrantyResponse;
import com.gymams.exception.ApiException;
import com.gymams.model.Equipment;
import com.gymams.model.Warranty;
import com.gymams.model.WarrantyType;
import com.gymams.repository.EquipmentRepository;
import com.gymams.repository.WarrantyRepository;

/**
 * Module 7 — Warranty Management.
 *
 * References the existing Equipment entity rather than duplicating
 * equipment fields (see Warranty.java). Warranty status is never stored —
 * ACTIVE / EXPIRING_SOON / EXPIRED is computed from expiryDate on every
 * read here, the same way UsageStatus is derived rather than cached.
 */
@Service
public class WarrantyService {

    private static final String CODE_PREFIX = "WAR";
    private static final Pattern CODE_PATTERN = Pattern.compile("^WAR(\\d+)$");
    private static final long EXPIRING_SOON_THRESHOLD_DAYS = 30;

    private final WarrantyRepository warrantyRepository;
    private final EquipmentRepository equipmentRepository;

    public WarrantyService(WarrantyRepository warrantyRepository, EquipmentRepository equipmentRepository) {
        this.warrantyRepository = warrantyRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @Transactional
    public WarrantyResponse create(WarrantyRequest request) {
        Equipment equipment = findEquipment(request.getEquipmentId());
        WarrantyType type = parseType(request.getWarrantyType());
        LocalDate start = parseDate(request.getStartDate(), "Enter a valid start date.");
        LocalDate expiry = parseDate(request.getExpiryDate(), "Enter a valid expiry date.");
        requireExpiryAfterStart(start, expiry);

        String provider = requireNonBlank(request.getProvider(), "Enter the warranty provider.");

        Warranty warranty = new Warranty();
        warranty.setWarrantyCode(generateNextCode());
        warranty.setEquipment(equipment);
        warranty.setProvider(provider);
        warranty.setWarrantyType(type);
        warranty.setStartDate(start);
        warranty.setExpiryDate(expiry);
        warranty.setCoverageDetails(blankToNull(request.getCoverageDetails()));
        warranty.setContactInformation(blankToNull(request.getContactInformation()));

        return toResponse(warrantyRepository.save(warranty));
    }

    @Transactional
    public WarrantyResponse update(Long id, WarrantyRequest request) {
        Warranty warranty = warrantyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Warranty not found."));

        Equipment equipment = findEquipment(request.getEquipmentId());
        WarrantyType type = parseType(request.getWarrantyType());
        LocalDate start = parseDate(request.getStartDate(), "Enter a valid start date.");
        LocalDate expiry = parseDate(request.getExpiryDate(), "Enter a valid expiry date.");
        requireExpiryAfterStart(start, expiry);

        String provider = requireNonBlank(request.getProvider(), "Enter the warranty provider.");

        // warrantyCode is never reassigned on update — same convention as
        // Equipment's equipmentCode and User's technicianCode, both of
        // which are permanent identifiers set once at creation.
        warranty.setEquipment(equipment);
        warranty.setProvider(provider);
        warranty.setWarrantyType(type);
        warranty.setStartDate(start);
        warranty.setExpiryDate(expiry);
        warranty.setCoverageDetails(blankToNull(request.getCoverageDetails()));
        warranty.setContactInformation(blankToNull(request.getContactInformation()));

        return toResponse(warrantyRepository.save(warranty));
    }

    public List<WarrantyResponse> findAll() {
        return warrantyRepository.findAllByOrderByExpiryDateAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public WarrantyResponse findById(Long id) {
        Warranty warranty = warrantyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Warranty not found."));
        return toResponse(warranty);
    }

    /* ---------- helpers ---------- */

    private Equipment findEquipment(String equipmentCode) {
        if (equipmentCode == null || equipmentCode.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select equipment.");
        }
        return equipmentRepository.findByEquipmentCodeIgnoreCase(equipmentCode.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Equipment not found."));
    }

    private WarrantyType parseType(String raw) {
        try {
            return WarrantyType.valueOf(raw);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select a valid warranty type.");
        }
    }

    private LocalDate parseDate(String raw, String errorMessage) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, errorMessage);
        }
    }

    private void requireExpiryAfterStart(LocalDate start, LocalDate expiry) {
        if (!expiry.isAfter(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expiry date must be after the start date.");
        }
    }

    private String requireNonBlank(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, errorMessage);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    /**
     * Generates the next WAR### code by scanning existing codes for the
     * highest numeric suffix — identical convention to
     * EquipmentService.generateNextCode() and UserService's TECH### code
     * generation: a deleted WAR005 never gets reissued while WAR006+
     * already exist.
     */
    private synchronized String generateNextCode() {
        int max = warrantyRepository.findAll().stream()
                .map(Warranty::getWarrantyCode)
                .map(CODE_PATTERN::matcher)
                .filter(Matcher::matches)
                .mapToInt(m -> Integer.parseInt(m.group(1)))
                .max()
                .orElse(0);

        int next = max + 1;
        return CODE_PREFIX + String.format("%03d", next);
    }

    private WarrantyResponse toResponse(Warranty w) {
        WarrantyResponse dto = new WarrantyResponse();
        dto.setId(w.getId());
        dto.setWarrantyCode(w.getWarrantyCode());
        dto.setEquipmentId(w.getEquipment().getEquipmentCode());
        dto.setEquipmentName(w.getEquipment().getEquipmentName());
        dto.setProvider(w.getProvider());
        dto.setWarrantyType(w.getWarrantyType().name());
        dto.setStartDate(w.getStartDate().toString());
        dto.setExpiryDate(w.getExpiryDate().toString());
        dto.setCoverageDetails(w.getCoverageDetails());
        dto.setContactInformation(w.getContactInformation());

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), w.getExpiryDate());
        dto.setDaysRemaining(daysRemaining);
        if (daysRemaining < 0) {
            dto.setStatus("EXPIRED");
        } else if (daysRemaining <= EXPIRING_SOON_THRESHOLD_DAYS) {
            dto.setStatus("EXPIRING_SOON");
        } else {
            dto.setStatus("ACTIVE");
        }

        return dto;
    }
}