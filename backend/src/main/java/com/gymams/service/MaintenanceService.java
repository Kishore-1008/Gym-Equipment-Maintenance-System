package com.gymams.service;

import com.gymams.dto.MaintenanceRescheduleRequest;
import com.gymams.dto.MaintenanceScheduleCreateRequest;
import com.gymams.dto.MaintenanceScheduleResponse;
import com.gymams.exception.ApiException;
import com.gymams.model.Equipment;
import com.gymams.model.MaintenanceSchedule;
import com.gymams.model.MaintenanceStatus;
import com.gymams.model.MaintenanceType;
import com.gymams.model.User;
import com.gymams.repository.EquipmentRepository;
import com.gymams.repository.MaintenanceScheduleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Module 5 — Maintenance Management.
 *
 * Entirely Admin-driven scheduled/preventive servicing — the Gym Manager
 * has no role here, and nothing here is ever created automatically from
 * usage limits (see EquipmentService / UsageStatus for that entirely
 * separate, non-persisted concept). Workflow: SCHEDULED -> IN_PROGRESS ->
 * COMPLETED, with SCHEDULED -> CANCELLED as an Admin-only side exit.
 */
@Service
public class MaintenanceService {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MaintenanceScheduleRepository maintenanceScheduleRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserService userService;

    private final EquipmentService equipmentService;

    public MaintenanceService(MaintenanceScheduleRepository maintenanceScheduleRepository,
                               EquipmentRepository equipmentRepository,
                               UserService userService,
                               EquipmentService equipmentService) {
        this.maintenanceScheduleRepository = maintenanceScheduleRepository;
        this.equipmentRepository = equipmentRepository;
        this.userService = userService;
        this.equipmentService = equipmentService;
    }

    /* ---------- Admin ---------- */

    @Transactional
    public MaintenanceScheduleResponse schedule(String adminUsername, MaintenanceScheduleCreateRequest request) {
        Equipment equipment = equipmentRepository.findByEquipmentCodeIgnoreCase(request.getEquipmentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Equipment not found."));

        LocalDate date = parseDate(request.getMaintenanceDate());
        MaintenanceType type = parseType(request.getMaintenanceType());
        User technician = userService.findTechnicianByUsername(request.getTechnicianUsername());

        MaintenanceSchedule schedule = new MaintenanceSchedule();
        schedule.setEquipment(equipment);
        schedule.setMaintenanceDate(date);
        schedule.setMaintenanceType(type);
        schedule.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        schedule.setStatus(MaintenanceStatus.SCHEDULED);
        schedule.setAssignedTechnicianUsername(technician.getUsername());
        schedule.setAssignedTechnicianFullName(technician.getFullName());
        schedule.setAssignedTechnicianCode(technician.getTechnicianCode());
        schedule.setScheduledByUsername(adminUsername);

        MaintenanceScheduleResponse response = toResponse(maintenanceScheduleRepository.save(schedule));

        // A newly scheduled (not yet started) maintenance record makes the
        // equipment MAINTENANCE_DUE, unless an active repair already takes
        // priority — recomputeStatus() resolves that (Issue 9/11).
        equipmentService.recomputeStatus(equipment.getId());
        return response;
    }

    public List<MaintenanceScheduleResponse> listAll() {
        return maintenanceScheduleRepository.findAllByOrderByMaintenanceDateDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MaintenanceScheduleResponse reschedule(Long id, MaintenanceRescheduleRequest request) {
        MaintenanceSchedule schedule = getOrThrow(id);
        if (schedule.getStatus() == MaintenanceStatus.COMPLETED || schedule.getStatus() == MaintenanceStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Completed or cancelled maintenance cannot be rescheduled.");
        }
        schedule.setMaintenanceDate(parseDate(request.getMaintenanceDate()));
        return toResponse(maintenanceScheduleRepository.save(schedule));
    }

    @Transactional
    public MaintenanceScheduleResponse cancel(Long id) {
        MaintenanceSchedule schedule = getOrThrow(id);
        if (schedule.getStatus() == MaintenanceStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Completed maintenance cannot be cancelled.");
        }
        if (schedule.getStatus() == MaintenanceStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This maintenance is already cancelled.");
        }
        schedule.setStatus(MaintenanceStatus.CANCELLED);
        schedule.setCancelledAt(LocalDateTime.now());
        MaintenanceScheduleResponse response = toResponse(maintenanceScheduleRepository.save(schedule));

        // This maintenance no longer blocks anything — recompute in case
        // that was the only thing keeping the equipment unavailable.
        equipmentService.recomputeStatus(schedule.getEquipment().getId());
        return response;
    }

    /* ---------- Technician ---------- */

    public List<MaintenanceScheduleResponse> listForTechnician(String username) {
        return maintenanceScheduleRepository.findByAssignedTechnicianUsernameOrderByMaintenanceDateAsc(username).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MaintenanceScheduleResponse start(Long id, String username) {
        MaintenanceSchedule schedule = getOrThrow(id);
        requireAssignedTo(schedule, username);
        if (schedule.getStatus() != MaintenanceStatus.SCHEDULED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only scheduled maintenance can be started.");
        }
        schedule.setStatus(MaintenanceStatus.IN_PROGRESS);
        schedule.setStartedAt(LocalDateTime.now());
        MaintenanceScheduleResponse response = toResponse(maintenanceScheduleRepository.save(schedule));

        // "Maintenance begins" -> UNDER_MAINTENANCE, unless an active repair
        // already takes priority (Issue 9).
        equipmentService.recomputeStatus(schedule.getEquipment().getId());
        return response;
    }

    @Transactional
    public MaintenanceScheduleResponse complete(Long id, String username, String completionDetails) {
        MaintenanceSchedule schedule = getOrThrow(id);
        requireAssignedTo(schedule, username);
        if (schedule.getStatus() != MaintenanceStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only in-progress maintenance can be completed.");
        }
        String details = completionDetails == null ? "" : completionDetails.trim();
        if (details.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter completion details.");
        }
        schedule.setStatus(MaintenanceStatus.COMPLETED);
        schedule.setCompletedAt(LocalDateTime.now());
        schedule.setCompletionDetails(details);
        MaintenanceScheduleResponse response = toResponse(maintenanceScheduleRepository.save(schedule));

        // Re-derive equipment status from scratch — don't blindly set
        // OPERATIONAL; another active repair must keep it unavailable (Issue 11).
        equipmentService.recomputeStatus(schedule.getEquipment().getId());
        return response;
    }

    /* ---------- helpers ---------- */

    private MaintenanceSchedule getOrThrow(Long id) {
        return maintenanceScheduleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Maintenance record not found."));
    }

    private void requireAssignedTo(MaintenanceSchedule schedule, String username) {
        if (schedule.getAssignedTechnicianUsername() == null
                || !schedule.getAssignedTechnicianUsername().equalsIgnoreCase(username)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This maintenance task is not assigned to you.");
        }
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a valid maintenance date.");
        }
    }

    private MaintenanceType parseType(String raw) {
        try {
            return MaintenanceType.valueOf(raw);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select a valid maintenance type.");
        }
    }

    private MaintenanceScheduleResponse toResponse(MaintenanceSchedule s) {
        MaintenanceScheduleResponse dto = new MaintenanceScheduleResponse();
        dto.setId(s.getId());
        dto.setEquipmentId(s.getEquipment().getEquipmentCode());
        dto.setEquipmentName(s.getEquipment().getEquipmentName());
        dto.setMaintenanceType(s.getMaintenanceType().name());
        dto.setMaintenanceDate(s.getMaintenanceDate() == null ? null : s.getMaintenanceDate().toString());
        dto.setDescription(s.getDescription());
        dto.setStatus(s.getStatus().name());
        dto.setAssignedTechnicianUsername(s.getAssignedTechnicianUsername());
        dto.setAssignedTechnicianName(s.getAssignedTechnicianFullName());
        dto.setAssignedTechnicianCode(s.getAssignedTechnicianCode());
        dto.setScheduledByUsername(s.getScheduledByUsername());
        dto.setStartedDate(format(s.getStartedAt()));
        dto.setCompletedDate(format(s.getCompletedAt()));
        dto.setCompletionDetails(s.getCompletionDetails());
        dto.setCancelledDate(format(s.getCancelledAt()));
        return dto;
    }

    private String format(LocalDateTime dt) {
        return dt == null ? null : dt.format(DATETIME_FMT);
    }
}
