package com.gymams.service;

import com.gymams.dto.BatchUsageRequest;
import com.gymams.dto.TopUsageResponse;
import com.gymams.dto.UsageHistoryPointResponse;
import com.gymams.dto.UsageRecordRequest;
import com.gymams.dto.UsageRecordResponse;
import com.gymams.dto.UsageSummaryResponse;
import com.gymams.exception.ApiException;
import com.gymams.model.Equipment;
import com.gymams.model.UsageRecord;
import com.gymams.model.UsageStatus;
import com.gymams.repository.EquipmentRepository;
import com.gymams.repository.UsageRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsageService {

    private final UsageRecordRepository usageRecordRepository;
    private final EquipmentRepository equipmentRepository;

    public UsageService(UsageRecordRepository usageRecordRepository, EquipmentRepository equipmentRepository) {
        this.usageRecordRepository = usageRecordRepository;
        this.equipmentRepository = equipmentRepository;
    }

    /* ============================================================
       B. Usage Table — every piece of equipment (optionally filtered
       by zone) for one selected date. Equipment with no logged
       record yet for that date still appears, at 0 sessions, rather
       than being silently omitted.
       ============================================================ */

    public List<UsageRecordResponse> tableForDate(String zone, LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();

        Map<String, UsageRecord> byEquipmentCode = usageRecordRepository
                .findAllByUsageDateOrderByEquipment_EquipmentCodeAsc(effectiveDate).stream()
                .collect(Collectors.toMap(r -> r.getEquipment().getEquipmentCode(), r -> r));

        return equipmentRepository.findAllByOrderByEquipmentCodeAsc().stream()
                .filter(eq -> zone == null || zone.isBlank() || eq.getCategory().equalsIgnoreCase(zone))
                .map(eq -> {
                    UsageRecord record = byEquipmentCode.get(eq.getEquipmentCode());
                    if (record != null) {
                        return toResponse(record);
                    }
                    return syntheticZeroResponse(eq, effectiveDate);
                })
                .collect(Collectors.toList());
    }

    /* ============================================================
       A. Summary cards — gym-wide stats for one selected date.
       ============================================================ */

    public UsageSummaryResponse summary(LocalDate date) {
        List<UsageRecordResponse> table = tableForDate(null, date);
        LocalDate effectiveDate = date != null ? date : LocalDate.now();

        int totalSessions = table.stream().mapToInt(UsageRecordResponse::getSessionCount).sum();
        int totalEquipment = table.size();

        Optional<UsageRecordResponse> most = table.stream()
                .max(Comparator.comparingInt(UsageRecordResponse::getSessionCount));
        Optional<UsageRecordResponse> least = table.stream()
                .min(Comparator.comparingInt(UsageRecordResponse::getSessionCount));

        return new UsageSummaryResponse(
                effectiveDate.toString(),
                totalSessions,
                totalEquipment,
                most.map(UsageRecordResponse::getEquipmentId).orElse(null),
                most.map(UsageRecordResponse::getEquipmentName).orElse(null),
                most.map(UsageRecordResponse::getSessionCount).orElse(null),
                least.map(UsageRecordResponse::getEquipmentId).orElse(null),
                least.map(UsageRecordResponse::getEquipmentName).orElse(null),
                least.map(UsageRecordResponse::getSessionCount).orElse(null)
        );
    }

    /* ============================================================
       C. Usage History / Trend — one equipment, across many days,
       optionally grouped into weekly or monthly totals.
       ============================================================ */

    public List<UsageHistoryPointResponse> history(String equipmentId, String groupBy, int days) {
        Equipment equipment = findEquipment(equipmentId);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(days, 1) - 1L);

        List<UsageRecord> records = usageRecordRepository
                .findAllByEquipment_EquipmentCodeIgnoreCaseAndUsageDateBetweenOrderByUsageDateAsc(
                        equipment.getEquipmentCode(), from, to);

        String normalizedGroupBy = groupBy == null ? "day" : groupBy.toLowerCase();

        if ("week".equals(normalizedGroupBy)) {
            Map<String, Integer> byWeek = new java.util.TreeMap<>();
            for (UsageRecord r : records) {
                String key = r.getUsageDate().get(IsoFields.WEEK_BASED_YEAR) + "-W"
                        + String.format("%02d", r.getUsageDate().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
                byWeek.merge(key, r.getSessionCount(), Integer::sum);
            }
            List<UsageHistoryPointResponse> points = byWeek.entrySet().stream()
                    .map(e -> new UsageHistoryPointResponse(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            java.util.Collections.reverse(points);
            return points;
        }

        if ("month".equals(normalizedGroupBy)) {
            Map<String, Integer> byMonth = new java.util.TreeMap<>();
            for (UsageRecord r : records) {
                String key = r.getUsageDate().toString().substring(0, 7); // YYYY-MM
                byMonth.merge(key, r.getSessionCount(), Integer::sum);
            }
            List<UsageHistoryPointResponse> points = byMonth.entrySet().stream()
                    .map(e -> new UsageHistoryPointResponse(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            java.util.Collections.reverse(points);
            return points;
        }

        // day (default): most recent first
        List<UsageRecord> desc = new ArrayList<>(records);
        desc.sort(Comparator.comparing(UsageRecord::getUsageDate).reversed());
        return desc.stream()
                .map(r -> new UsageHistoryPointResponse(r.getUsageDate().toString(), r.getSessionCount()))
                .collect(Collectors.toList());
    }

    /* ============================================================
       D. Top Used Equipment — ranked by total sessions over a window.
       ============================================================ */

    public List<TopUsageResponse> topUsed(int days, int limit) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(days, 1) - 1L);

        Map<Equipment, Integer> totals = usageRecordRepository
                .findAllByUsageDateBetweenOrderByUsageDateDesc(from, to).stream()
                .collect(Collectors.groupingBy(UsageRecord::getEquipment,
                        Collectors.summingInt(UsageRecord::getSessionCount)));

        return totals.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(Math.max(limit, 1))
                .map(e -> new TopUsageResponse(
                        e.getKey().getEquipmentCode(), e.getKey().getEquipmentName(),
                        e.getKey().getCategory(), e.getValue()))
                .collect(Collectors.toList());
    }

    /* ============================================================
       Mutations
       ============================================================ */

    /**
     * Create-or-update by (equipment, date): if a record already exists
     * for that day it is updated in place, never duplicated.
     */
    @Transactional
    public UsageRecordResponse upsert(UsageRecordRequest request, String recordedBy) {
        Equipment equipment = findEquipment(request.getEquipmentId());
        LocalDate date = parseDateOrToday(request.getUsageDate());
        int sessionCount = requireNonNegative(request.getSessionCount());

        UsageRecord record = usageRecordRepository
                .findByEquipment_EquipmentCodeIgnoreCaseAndUsageDate(equipment.getEquipmentCode(), date)
                .orElseGet(UsageRecord::new);

        record.setEquipment(equipment);
        record.setUsageDate(date);
        record.setSessionCount(sessionCount);
        record.setRecordedBy(recordedBy);

        return toResponse(usageRecordRepository.save(record));
    }

    /**
     * Batch save — one date, many equipment/session-count pairs, all in
     * one transaction. Lets a Gym Manager log an entire zone without
     * navigating to separate equipment pages.
     */
    @Transactional
    public List<UsageRecordResponse> batchUpsert(BatchUsageRequest request, String recordedBy) {
        LocalDate date = parseDateOrToday(request.getUsageDate());
        List<UsageRecordResponse> saved = new ArrayList<>();

        for (BatchUsageRequest.Entry entry : request.getEntries()) {
            Equipment equipment = findEquipment(entry.getEquipmentId());
            int sessionCount = requireNonNegative(entry.getSessionCount());

            UsageRecord record = usageRecordRepository
                    .findByEquipment_EquipmentCodeIgnoreCaseAndUsageDate(equipment.getEquipmentCode(), date)
                    .orElseGet(UsageRecord::new);

            record.setEquipment(equipment);
            record.setUsageDate(date);
            record.setSessionCount(sessionCount);
            record.setRecordedBy(recordedBy);

            saved.add(toResponse(usageRecordRepository.save(record)));
        }

        return saved;
    }

    /** Edits an existing record's session count only — equipment and date are the record's identity. */
    @Transactional
    public UsageRecordResponse updateById(Long id, UsageRecordRequest request, String requestedBy) {
        UsageRecord record = usageRecordRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usage record not found."));

        record.setSessionCount(requireNonNegative(request.getSessionCount()));
        record.setRecordedBy(requestedBy);

        return toResponse(usageRecordRepository.save(record));
    }

    @Transactional
    public void delete(Long id) {
        if (!usageRecordRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Usage record not found.");
        }
        usageRecordRepository.deleteById(id);
    }

    /* ---------- helpers ---------- */

    private Equipment findEquipment(String equipmentCode) {
        return equipmentRepository.findByEquipmentCodeIgnoreCase(equipmentCode)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Select a valid equipment."));
    }

    private LocalDate parseDateOrToday(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(isoDate);
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a valid date.");
        }
    }

    private int requireNonNegative(Integer value) {
        if (value == null || value < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Session count must be zero or greater.");
        }
        return value;
    }

    private UsageRecordResponse toResponse(UsageRecord r) {
        return new UsageRecordResponse(
                r.getId(),
                r.getEquipment().getEquipmentCode(),
                r.getEquipment().getEquipmentName(),
                r.getEquipment().getCategory(),
                r.getUsageDate().toString(),
                r.getSessionCount(),
                UsageStatus.fromSessionCount(r.getSessionCount()).name(),
                r.getRecordedBy(),
                r.getRecordedAt().toString(),
                r.getUpdatedAt() == null ? null : r.getUpdatedAt().toString()
        );
    }

    /** No record logged yet for this equipment/date — shown as 0 sessions rather than omitted. */
    private UsageRecordResponse syntheticZeroResponse(Equipment eq, LocalDate date) {
        return new UsageRecordResponse(
                null,
                eq.getEquipmentCode(),
                eq.getEquipmentName(),
                eq.getCategory(),
                date.toString(),
                0,
                UsageStatus.NOT_USED.name(),
                null,
                null,
                null
        );
    }
}
