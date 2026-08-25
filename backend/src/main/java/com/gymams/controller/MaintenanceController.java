package com.gymams.controller;

import com.gymams.dto.CompletionDetailsRequest;
import com.gymams.dto.MaintenanceRescheduleRequest;
import com.gymams.dto.MaintenanceScheduleCreateRequest;
import com.gymams.dto.MaintenanceScheduleResponse;
import com.gymams.service.MaintenanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Module 5 — Maintenance Management. Entirely Admin + Technician; the Gym
 * Manager has no endpoint here at all. Role checks are enforced in
 * SecurityConfig by exact path + HTTP method.
 */
@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    /* ---------- Admin ---------- */

    @PostMapping
    public ResponseEntity<MaintenanceScheduleResponse> schedule(Authentication authentication,
                                                                  @Valid @RequestBody MaintenanceScheduleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.schedule(authentication.getName(), request));
    }

    @GetMapping
    public List<MaintenanceScheduleResponse> all() {
        return maintenanceService.listAll();
    }

    @PutMapping("/{id}/reschedule")
    public MaintenanceScheduleResponse reschedule(@PathVariable Long id, @Valid @RequestBody MaintenanceRescheduleRequest request) {
        return maintenanceService.reschedule(id, request);
    }

    @PutMapping("/{id}/cancel")
    public MaintenanceScheduleResponse cancel(@PathVariable Long id) {
        return maintenanceService.cancel(id);
    }

    /* ---------- Technician ---------- */

    @GetMapping("/assigned")
    public List<MaintenanceScheduleResponse> assignedToMe(Authentication authentication) {
        return maintenanceService.listForTechnician(authentication.getName());
    }

    @PutMapping("/{id}/start")
    public MaintenanceScheduleResponse start(@PathVariable Long id, Authentication authentication) {
        return maintenanceService.start(id, authentication.getName());
    }

    @PutMapping("/{id}/complete")
    public MaintenanceScheduleResponse complete(@PathVariable Long id, Authentication authentication,
                                                 @Valid @RequestBody CompletionDetailsRequest request) {
        return maintenanceService.complete(id, authentication.getName(), request.getCompletionDetails());
    }
}
