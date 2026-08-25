package com.gymams.controller;

import com.gymams.dto.CompletionDetailsRequest;
import com.gymams.dto.RepairRequestCreateRequest;
import com.gymams.dto.RepairRequestRejectRequest;
import com.gymams.dto.RepairRequestResponse;
import com.gymams.dto.TechnicianAssignRequest;
import com.gymams.service.RepairRequestService;
import com.gymams.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Module 4 — Repair Request Management.
 * Role checks are enforced in SecurityConfig by exact path + HTTP method;
 * this controller additionally trusts only the verified JWT identity
 * (Authentication) for "who is acting", never a client-supplied username.
 */
@RestController
@RequestMapping("/api/repair-requests")
public class RepairRequestController {

    private final RepairRequestService repairRequestService;
    private final UserService userService;

    public RepairRequestController(RepairRequestService repairRequestService, UserService userService) {
        this.repairRequestService = repairRequestService;
        this.userService = userService;
    }

    /* ---------- Gym Manager ---------- */

    @PostMapping
    public ResponseEntity<RepairRequestResponse> reportProblem(Authentication authentication,
                                                                 @Valid @RequestBody RepairRequestCreateRequest request) {
        String fullName = fullNameOf(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(repairRequestService.reportProblem(authentication.getName(), fullName, request));
    }

    @GetMapping("/my")
    public List<RepairRequestResponse> myRequests(Authentication authentication) {
        return repairRequestService.listForManager(authentication.getName());
    }

    /* ---------- Admin ---------- */

    @GetMapping
    public List<RepairRequestResponse> all() {
        return repairRequestService.listAll();
    }

    @PutMapping("/{id}/approve")
    public RepairRequestResponse approve(@PathVariable Long id) {
        return repairRequestService.approve(id);
    }

    @PutMapping("/{id}/reject")
    public RepairRequestResponse reject(@PathVariable Long id, @Valid @RequestBody RepairRequestRejectRequest request) {
        return repairRequestService.reject(id, request.getRejectionReason());
    }

    @PutMapping("/{id}/assign")
    public RepairRequestResponse assign(@PathVariable Long id, @Valid @RequestBody TechnicianAssignRequest request) {
        return repairRequestService.assignTechnician(id, request.getTechnicianUsername());
    }

    /* ---------- Technician ---------- */

    @GetMapping("/assigned")
    public List<RepairRequestResponse> assignedToMe(Authentication authentication) {
        return repairRequestService.listForTechnician(authentication.getName());
    }

    @PutMapping("/{id}/start")
    public RepairRequestResponse start(@PathVariable Long id, Authentication authentication) {
        return repairRequestService.start(id, authentication.getName());
    }

    @PutMapping("/{id}/complete")
    public RepairRequestResponse complete(@PathVariable Long id, Authentication authentication,
                                           @Valid @RequestBody CompletionDetailsRequest request) {
        return repairRequestService.complete(id, authentication.getName(), request.getCompletionDetails());
    }

    /**
     * The JWT only carries username + role (see JwtUtil), so the display-friendly
     * full name is resolved from the verified username via UserService — never
     * trusted from the request body.
     */
    private String fullNameOf(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).getFullName();
    }
}
