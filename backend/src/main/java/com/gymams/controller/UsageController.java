package com.gymams.controller;

import com.gymams.dto.BatchUsageRequest;
import com.gymams.dto.TopUsageResponse;
import com.gymams.dto.UsageHistoryPointResponse;
import com.gymams.dto.UsageRecordRequest;
import com.gymams.dto.UsageRecordResponse;
import com.gymams.dto.UsageSummaryResponse;
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

    /** B. Usage Table — GET /api/usage?zone=&date= */
    @GetMapping
    public List<UsageRecordResponse> table(
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String date) {
        return usageService.tableForDate(zone, parseDate(date));
    }

    /** A. Summary cards — GET /api/usage/summary?date= */
    @GetMapping("/summary")
    public UsageSummaryResponse summary(@RequestParam(required = false) String date) {
        return usageService.summary(parseDate(date));
    }

    /** C. Usage History / Trend — GET /api/usage/history?equipmentId=&groupBy=day|week|month&days= */
    @GetMapping("/history")
    public List<UsageHistoryPointResponse> history(
            @RequestParam String equipmentId,
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestParam(defaultValue = "30") int days) {
        return usageService.history(equipmentId, groupBy, days);
    }

    /** D. Top Used Equipment — GET /api/usage/top?days=&limit= */
    @GetMapping("/top")
    public List<TopUsageResponse> top(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "5") int limit) {
        return usageService.topUsed(days, limit);
    }

    /** Single upsert — GYM_MANAGER only. */
    @PostMapping
    public ResponseEntity<UsageRecordResponse> save(@Valid @RequestBody UsageRecordRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usageService.upsert(request, authentication.getName()));
    }

    /** Batch upsert for a whole zone in one screen — GYM_MANAGER only. */
    @PostMapping("/batch")
    public ResponseEntity<List<UsageRecordResponse>> saveBatch(@Valid @RequestBody BatchUsageRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usageService.batchUpsert(request, authentication.getName()));
    }

    /** Edit a single existing record's session count — GYM_MANAGER only. */
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
