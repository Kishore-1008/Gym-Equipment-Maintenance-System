package com.gymams.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gymams.dto.RepairHistoryResponse;
import com.gymams.service.RepairHistoryService;

/**
 * Module 6 — Repair History. ADMIN-only (enforced in SecurityConfig) and
 * strictly read-only — there is no create/update/delete endpoint here.
 * Records are only ever created internally by RepairRequestService.complete()
 * via RepairHistoryService.recordCompletion().
 */
@RestController
@RequestMapping("/api/repair-history")
public class RepairHistoryController {

    private final RepairHistoryService repairHistoryService;

    public RepairHistoryController(RepairHistoryService repairHistoryService) {
        this.repairHistoryService = repairHistoryService;
    }

    @GetMapping
    public List<RepairHistoryResponse> all() {
        return repairHistoryService.listAll();
    }
}