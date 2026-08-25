package com.gymams.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gymams.dto.RepairHistoryResponse;
import com.gymams.exception.ApiException;
import com.gymams.model.RepairHistory;
import com.gymams.model.RepairRequest;
import com.gymams.repository.RepairHistoryRepository;

/**
 * Module 6 — Repair History.
 *
 * A permanent, read-only-for-Admin record of each completed repair.
 * recordCompletion() is called from RepairRequestService.complete() at the
 * exact moment a repair request transitions to COMPLETED — this service
 * never creates a history row on its own initiative, so it can't drift out
 * of sync with what "completed" actually means in Module 4.
 */
@Service
public class RepairHistoryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RepairHistoryRepository repairHistoryRepository;

    public RepairHistoryService(RepairHistoryRepository repairHistoryRepository) {
        this.repairHistoryRepository = repairHistoryRepository;
    }

    /**
     * Creates the permanent history record for a just-completed repair
     * request. Called from within RepairRequestService.complete()'s own
     * transaction — if this throws, the whole completion rolls back rather
     * than leaving the request COMPLETED with no matching history.
     *
     * Guards against ever creating a second record for the same repair
     * request (defense in depth alongside the DB's own unique constraint
     * on repair_request_id) — in normal operation this can't happen anyway
     * since complete() only accepts IN_PROGRESS requests and a request can
     * only pass through IN_PROGRESS -> COMPLETED once.
     */
    @Transactional
    public void recordCompletion(RepairRequest repairRequest, String repairDetails, String partsUsed,
                                  BigDecimal repairCost, String completionNotes) {
        if (repairHistoryRepository.existsByRepairRequest_Id(repairRequest.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "A repair history record already exists for this repair request.");
        }

        RepairHistory history = new RepairHistory();
        history.setRepairRequest(repairRequest);
        history.setEquipment(repairRequest.getEquipment());
        history.setEquipmentName(repairRequest.getEquipment().getEquipmentName());
        history.setEquipmentCode(repairRequest.getEquipment().getEquipmentCode());
        history.setProblemDescription(repairRequest.getProblemDescription());
        history.setTechnicianUsername(repairRequest.getAssignedTechnicianUsername());
        history.setTechnicianFullName(repairRequest.getAssignedTechnicianFullName());
        history.setTechnicianCode(repairRequest.getAssignedTechnicianCode());
        history.setRepairDetails(repairDetails);
        history.setPartsUsed(partsUsed);
        history.setRepairCost(repairCost);
        history.setCompletionNotes(completionNotes);
        history.setCompletedAt(repairRequest.getCompletedAt() != null ? repairRequest.getCompletedAt() : LocalDateTime.now());

        repairHistoryRepository.save(history);
    }

    /** Admin's Repair History table — most recently completed first, full detail included so "View Details" needs no extra request. */
    public List<RepairHistoryResponse> listAll() {
        return repairHistoryRepository.findAllByOrderByCompletedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private RepairHistoryResponse toResponse(RepairHistory h) {
        RepairHistoryResponse dto = new RepairHistoryResponse();
        dto.setId(h.getId());
        dto.setRepairRequestId(h.getRepairRequest().getId());
        dto.setEquipmentId(h.getEquipmentCode());
        dto.setEquipmentName(h.getEquipmentName());
        dto.setTechnicianUsername(h.getTechnicianUsername());
        dto.setTechnicianName(h.getTechnicianFullName());
        dto.setTechnicianCode(h.getTechnicianCode());
        dto.setProblemDescription(h.getProblemDescription());
        dto.setRepairDetails(h.getRepairDetails());
        dto.setPartsUsed(h.getPartsUsed());
        dto.setRepairCost(h.getRepairCost());
        dto.setCompletionNotes(h.getCompletionNotes());
        dto.setCompletedDate(h.getCompletedAt() == null ? null : h.getCompletedAt().format(DATE_FMT));
        return dto;
    }
}