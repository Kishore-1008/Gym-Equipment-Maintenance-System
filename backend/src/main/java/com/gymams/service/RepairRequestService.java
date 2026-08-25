package com.gymams.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gymams.dto.RepairCompletionDetailsRequest;
import com.gymams.dto.RepairRequestCreateRequest;
import com.gymams.dto.RepairRequestResponse;
import com.gymams.exception.ApiException;
import com.gymams.model.Equipment;
import com.gymams.model.RepairRequest;
import com.gymams.model.RepairStatus;
import com.gymams.model.User;
import com.gymams.repository.EquipmentRepository;
import com.gymams.repository.RepairRequestRepository;

/**
 * Module 4 — Repair Request Management.
 *
 * Workflow: PENDING -> (REJECTED | APPROVED -> ASSIGNED -> IN_PROGRESS -> COMPLETED).
 * Deliberately has no knowledge of MaintenanceSchedule (Module 5) — a repair
 * request is only ever created by a Gym Manager reporting an actual
 * problem, never automatically from usage limits or anywhere else.
 */
@Service
public class RepairRequestService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RepairRequestRepository repairRequestRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserService userService;

    private final EquipmentService equipmentService;
    private final RepairHistoryService repairHistoryService;

    public RepairRequestService(RepairRequestRepository repairRequestRepository,
                                 EquipmentRepository equipmentRepository,
                                 UserService userService,
                                 EquipmentService equipmentService,
                                 RepairHistoryService repairHistoryService) {
        this.repairRequestRepository = repairRequestRepository;
        this.equipmentRepository = equipmentRepository;
        this.userService = userService;
        this.equipmentService = equipmentService;
        this.repairHistoryService = repairHistoryService;
    }

    /* ---------- Gym Manager ---------- */

    @Transactional
    public RepairRequestResponse reportProblem(String username, String fullName, RepairRequestCreateRequest request) {
        Equipment equipment = equipmentRepository.findByEquipmentCodeIgnoreCase(request.getEquipmentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Equipment not found."));

        String description = request.getProblemDescription() == null ? "" : request.getProblemDescription().trim();
        if (description.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a problem description.");
        }

        RepairRequest repairRequest = new RepairRequest();
        repairRequest.setEquipment(equipment);
        repairRequest.setProblemDescription(description);
        repairRequest.setStatus(RepairStatus.PENDING);
        repairRequest.setReportedByUsername(username);
        repairRequest.setReportedByFullName(fullName);

        return toResponse(repairRequestRepository.save(repairRequest));
    }

    public List<RepairRequestResponse> listForManager(String username) {
        return repairRequestRepository.findByReportedByUsernameOrderBySubmittedAtDesc(username).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /* ---------- Admin ---------- */

    public List<RepairRequestResponse> listAll() {
        return repairRequestRepository.findAllByOrderBySubmittedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RepairRequestResponse approve(Long id) {
        RepairRequest repairRequest = getOrThrow(id);
        if (repairRequest.getStatus() != RepairStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending requests can be approved.");
        }
        repairRequest.setStatus(RepairStatus.APPROVED);
        repairRequest.setReviewedAt(LocalDateTime.now());
        RepairRequestResponse response = toResponse(repairRequestRepository.save(repairRequest));

        // Approval is the point the Admin has confirmed the problem is real —
        // equipment becomes unavailable for normal use from here (Issue 10).
        equipmentService.recomputeStatus(repairRequest.getEquipment().getId());
        return response;
    }

    @Transactional
    public RepairRequestResponse reject(Long id, String rejectionReason) {
        RepairRequest repairRequest = getOrThrow(id);
        if (repairRequest.getStatus() != RepairStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending requests can be rejected.");
        }
        String reason = rejectionReason == null ? "" : rejectionReason.trim();
        if (reason.isEmpty()) {
            // Defense in depth — RepairRequestRejectRequest's @NotBlank already enforces this,
            // but a request cannot be rejected without a valid reason under any circumstance.
            throw new ApiException(HttpStatus.BAD_REQUEST, "A rejection reason is required.");
        }
        repairRequest.setStatus(RepairStatus.REJECTED);
        repairRequest.setRejectionReason(reason);
        repairRequest.setReviewedAt(LocalDateTime.now());
        return toResponse(repairRequestRepository.save(repairRequest));
    }

    @Transactional
    public RepairRequestResponse assignTechnician(Long id, String technicianUsername) {
        RepairRequest repairRequest = getOrThrow(id);
        if (repairRequest.getStatus() != RepairStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A technician can only be assigned after the request is approved.");
        }

        User technician = userService.findTechnicianByUsername(technicianUsername);

        repairRequest.setAssignedTechnicianUsername(technician.getUsername());
        repairRequest.setAssignedTechnicianFullName(technician.getFullName());
        repairRequest.setAssignedTechnicianCode(technician.getTechnicianCode());
        repairRequest.setAssignedAt(LocalDateTime.now());
        repairRequest.setStatus(RepairStatus.ASSIGNED);

        RepairRequestResponse response = toResponse(repairRequestRepository.save(repairRequest));

        // Defense in depth: equipment should already be UNDER_REPAIR from
        // approve(), but recompute again here too in case that step ran
        // under an older build (before this sync existed) or the status
        // was manually reset in between — assignment is exactly the
        // moment an Admin is looking at "is this equipment under repair?"
        equipmentService.recomputeStatus(repairRequest.getEquipment().getId());
        return response;
    }

    /* ---------- Technician ---------- */

    public List<RepairRequestResponse> listForTechnician(String username) {
        return repairRequestRepository.findByAssignedTechnicianUsernameOrderByAssignedAtDesc(username).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RepairRequestResponse start(Long id, String username) {
        RepairRequest repairRequest = getOrThrow(id);
        requireAssignedTo(repairRequest, username);
        if (repairRequest.getStatus() != RepairStatus.ASSIGNED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only assigned repairs can be started.");
        }
        repairRequest.setStatus(RepairStatus.IN_PROGRESS);
        repairRequest.setStartedAt(LocalDateTime.now());
        return toResponse(repairRequestRepository.save(repairRequest));
    }

    @Transactional
    public RepairRequestResponse complete(Long id, String username, RepairCompletionDetailsRequest request) {
        RepairRequest repairRequest = getOrThrow(id);
        requireAssignedTo(repairRequest, username);
        if (repairRequest.getStatus() != RepairStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only in-progress repairs can be completed.");
        }

        String repairDetails = request.getRepairDetails() == null ? "" : request.getRepairDetails().trim();
        if (repairDetails.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter repair details / work performed.");
        }
        String partsUsed = request.getPartsUsed() == null ? "" : request.getPartsUsed().trim();
        if (partsUsed.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter parts used, or \"No parts used\" if none.");
        }
        if (request.getRepairCost() == null || request.getRepairCost().signum() < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a repair cost of 0 or more.");
        }
        String completionNotes = request.getCompletionNotes() == null ? null : request.getCompletionNotes().trim();

        // The Repair Request's own completionDetails field keeps exactly
        // its existing meaning and stays visible on the request itself
        // (Module 4 behavior, unchanged) — it's set from the same "repair
        // details / work performed" text the technician just entered.
        repairRequest.setStatus(RepairStatus.COMPLETED);
        repairRequest.setCompletedAt(LocalDateTime.now());
        repairRequest.setCompletionDetails(repairDetails);
        RepairRequestResponse response = toResponse(repairRequestRepository.save(repairRequest));

        // Module 6 — permanent, richer history record. Runs in the same
        // transaction as the status change above: if this fails, the
        // whole completion rolls back rather than leaving the request
        // COMPLETED with no matching history.
        repairHistoryService.recordCompletion(repairRequest, repairDetails, partsUsed, request.getRepairCost(), completionNotes);

        // Re-derive equipment status from scratch rather than blindly
        // setting OPERATIONAL — another active repair or maintenance
        // record on the same equipment must keep it unavailable (Issue 9/11).
        equipmentService.recomputeStatus(repairRequest.getEquipment().getId());
        return response;
    }

    /* ---------- helpers ---------- */

    private RepairRequest getOrThrow(Long id) {
        return repairRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Repair request not found."));
    }

    private void requireAssignedTo(RepairRequest repairRequest, String username) {
        if (repairRequest.getAssignedTechnicianUsername() == null
                || !repairRequest.getAssignedTechnicianUsername().equalsIgnoreCase(username)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This repair request is not assigned to you.");
        }
    }

    private RepairRequestResponse toResponse(RepairRequest r) {
        RepairRequestResponse dto = new RepairRequestResponse();
        dto.setId(r.getId());
        dto.setEquipmentId(r.getEquipment().getEquipmentCode());
        dto.setEquipmentName(r.getEquipment().getEquipmentName());
        dto.setProblemDescription(r.getProblemDescription());
        dto.setStatus(r.getStatus().name());
        dto.setSubmittedByUsername(r.getReportedByUsername());
        dto.setSubmittedByFullName(r.getReportedByFullName());
        dto.setSubmittedDate(format(r.getSubmittedAt()));
        dto.setRejectionReason(r.getRejectionReason());
        dto.setReviewedDate(format(r.getReviewedAt()));
        dto.setAssignedTechnicianUsername(r.getAssignedTechnicianUsername());
        dto.setAssignedTechnicianName(r.getAssignedTechnicianFullName());
        dto.setAssignedTechnicianCode(r.getAssignedTechnicianCode());
        dto.setAssignedDate(format(r.getAssignedAt()));
        dto.setStartedDate(format(r.getStartedAt()));
        dto.setCompletedDate(format(r.getCompletedAt()));
        dto.setCompletionDetails(r.getCompletionDetails());
        return dto;
    }

    private String format(LocalDateTime dt) {
        return dt == null ? null : dt.format(DATE_FMT);
    }
}