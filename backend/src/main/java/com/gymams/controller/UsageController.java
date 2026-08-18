package com.gymams.controller;

import com.gymams.dto.BatchUsageRequest;
import com.gymams.dto.UsageDashboardResponse;
import com.gymams.dto.UsageRecordRequest;
import com.gymams.dto.UsageRecordResponse;
import com.gymams.exception.ApiException;
import com.gymams.service.UsageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Read access (GET) is ADMIN + GYM_MANAGER; POST/PUT (data entry) is
 * GYM_MANAGER only; DELETE is ADMIN only — all enforced in
 * SecurityConfig, matching the equipment controller's pattern.
 */
@RestController
@RequestMapping("/api/usage")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    /** Usage Table for one date — GET /api/usage?date= (also used to prefill batch entry). */
    @GetMapping
    public List<UsageRecordResponse> table(@RequestParam(required = false) String date) {
        return usageService.tableForDate(parseDate(date));
    }

    /**
     * Usage Monitoring dashboard — GET /api/usage/dashboard?date=
     * Today's usage, this month's usage, most/least used (today & month),
     * and each equipment's usage-based maintenance status, all in one call.
     */
    @GetMapping("/dashboard")
    public UsageDashboardResponse dashboard(@RequestParam(required = false) String date) {
        return usageService.dashboard(parseDate(date));
    }

    /** Single upsert — GYM_MANAGER only. */
    @PostMapping
    public ResponseEntity<UsageRecordResponse> save(@Valid @RequestBody UsageRecordRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usageService.upsert(request, authentication.getName()));
    }

    /** Batch upsert so a Gym Manager can log every machine's hours for the day in one screen — GYM_MANAGER only. */
    @PostMapping("/batch")
    public ResponseEntity<List<UsageRecordResponse>> saveBatch(@Valid @RequestBody BatchUsageRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usageService.batchUpsert(request, authentication.getName()));
    }

    /** Edit a single existing reading's usage hours — GYM_MANAGER only. */
    @PutMapping("/{id}")
    public UsageRecordResponse update(@PathVariable Long id,
                                       @Valid @RequestBody UsageRecordRequest request,
                                       Authentication authentication) {
        return usageService.updateById(id, request, authentication.getName());
    }

    /** ADMIN only. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        usageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a valid date.");
        }
    }
}
