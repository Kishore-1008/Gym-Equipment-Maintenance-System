package com.gymams.service;

import com.gymams.dto.BatchUsageRequest;
import com.gymams.dto.EquipmentUsageResponse;
import com.gymams.dto.UsageDashboardResponse;
import com.gymams.dto.UsageHighlightResponse;
import com.gymams.dto.UsageRecordRequest;
import com.gymams.dto.UsageRecordResponse;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Usage Monitoring — daily USAGE HOURS entered manually by the Gym
 * Manager (no sessions, no sensors/IoT, no timers). Backs:
 *   - Today's Usage / This Month's Usage
 *   - Most/Least Used Equipment (today and this month)
 *   - Batch Usage Entry
 *   - Usage-based preventive maintenance status (separate from Module 4
 *     repair/damage tracking)
 */
@Service
public class UsageService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final UsageRecordRepository usageRecordRepository;
    private final EquipmentRepository equipmentRepository;

    public UsageService(UsageRecordRepository usageRecordRepository, EquipmentRepository equipmentRepository) {
        this.usageRecordRepository = usageRecordRepository;
        this.equipmentRepository = equipmentRepository;
    }

    /* ============================================================
       Usage Table — every piece of equipment for one selected date.
       Equipment with no logged reading yet for that date still
       appears, at 0 hours, rather than being silently omitted. Used
       both to render a plain table and to prefill the batch-entry form.
       ============================================================ */

    public List<UsageRecordResponse> tableForDate(LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();

        Map<String, UsageRecord> byEquipmentCode = usageRecordRepository
                .findAllByUsageDateOrderByEquipment_EquipmentCodeAsc(effectiveDate).stream()
                .collect(Collectors.toMap(r -> r.getEquipment().getEquipmentCode(), r -> r));

        return equipmentRepository.findAllByOrderByEquipmentCodeAsc().stream()
                .map(eq -> {
                    UsageRecord record = byEquipmentCode.get(eq.getEquipmentCode());
                    return record != null ? toResponse(record) : syntheticZeroResponse(eq, effectiveDate);
                })
                .collect(Collectors.toList());
    }

    /* ============================================================
       Dashboard — everything the Usage Monitoring screen needs for
       one selected date: per-equipment today/month hours + maintenance
       status, plus the four most/least-used highlights. No lifetime
       usage statistic is ever produced here.
       ============================================================ */

    public UsageDashboardResponse dashboard(LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        LocalDate monthStart = effectiveDate.withDayOfMonth(1);
        LocalDate monthEnd = effectiveDate.withDayOfMonth(effectiveDate.lengthOfMonth());

        Map<String, Double> todayByEquipment = usageRecordRepository
                .findAllByUsageDateOrderByEquipment_EquipmentCodeAsc(effectiveDate).stream()
                .collect(Collectors.toMap(r -> r.getEquipment().getEquipmentCode(), UsageRecord::getUsageHours));

        Map<String, Double> monthByEquipment = usageRecordRepository
                .findAllByUsageDateBetweenOrderByUsageDateDesc(monthStart, monthEnd).stream()
                .collect(Collectors.groupingBy(r -> r.getEquipment().getEquipmentCode(),
                        Collectors.summingDouble(UsageRecord::getUsageHours)));

        List<Equipment> allEquipment = equipmentRepository.findAllByOrderByEquipmentCodeAsc();

        List<EquipmentUsageResponse> rows = allEquipment.stream()
                .map(eq -> {
                    double today = todayByEquipment.getOrDefault(eq.getEquipmentCode(), 0.0);
                    double month = monthByEquipment.getOrDefault(eq.getEquipmentCode(), 0.0);
                    UsageStatus status = UsageStatus.fromUsage(month, eq.getMonthlyUsageLimitHours());

                    return new EquipmentUsageResponse(
                            eq.getEquipmentCode(), eq.getEquipmentName(), eq.getCategory(),
                            today, month,
                            eq.getMonthlyUsageLimitHours(),
                            status.name()
                    );
                })
                .collect(Collectors.toList());

        Optional<Double> maxToday = rows.stream().map(EquipmentUsageResponse::getTodayUsageHours).max(Double::compare);
        Optional<Double> minToday = rows.stream().map(EquipmentUsageResponse::getTodayUsageHours).min(Double::compare);
        Optional<Double> maxMonth = rows.stream().map(EquipmentUsageResponse::getMonthUsageHours).max(Double::compare);
        Optional<Double> minMonth = rows.stream().map(EquipmentUsageResponse::getMonthUsageHours).min(Double::compare);

        List<UsageHighlightResponse> mostUsedToday = highlightsAt(rows, EquipmentUsageResponse::getTodayUsageHours, maxToday);
        List<UsageHighlightResponse> leastUsedToday = highlightsAt(rows, EquipmentUsageResponse::getTodayUsageHours, minToday);
        List<UsageHighlightResponse> mostUsedMonth = highlightsAt(rows, EquipmentUsageResponse::getMonthUsageHours, maxMonth);
        List<UsageHighlightResponse> leastUsedMonth = highlightsAt(rows, EquipmentUsageResponse::getMonthUsageHours, minMonth);

        return new UsageDashboardResponse(
                effectiveDate.toString(),
                monthStart.format(MONTH_LABEL),
                rows,
                mostUsedToday,
                leastUsedToday,
                mostUsedMonth,
                leastUsedMonth
        );
    }

    /**
     * Every equipment whose value (today's hours or month's hours) equals the
     * winning value — not just the first one found. This is what makes ties
     * show up as multiple entries instead of Stream.max()/min() silently
     * collapsing them to a single arbitrary equipment. Values are compared
     * to the nearest 0.01 hour so that floating-point summation of daily
     * readings (e.g. repeated 0.1-hour entries) can't hide a genuine tie.
     */
    private List<UsageHighlightResponse> highlightsAt(List<EquipmentUsageResponse> rows,
                                                        java.util.function.ToDoubleFunction<EquipmentUsageResponse> hoursOf,
                                                        Optional<Double> winningValue) {
        if (winningValue.isEmpty()) return List.of();
        long target = Math.round(winningValue.get() * 100);

        return rows.stream()
                .filter(r -> Math.round(hoursOf.applyAsDouble(r) * 100) == target)
                .map(r -> new UsageHighlightResponse(r.getEquipmentId(), r.getEquipmentName(), hoursOf.applyAsDouble(r)))
                .collect(Collectors.toList());
    }

    /* ============================================================
       Mutations
       ============================================================ */

    /**
     * Create-or-update by (equipment, date): if a reading already exists
     * for that day it is updated in place, never duplicated.
     */
    @Transactional
    public UsageRecordResponse upsert(UsageRecordRequest request, String recordedBy) {
        Equipment equipment = findEquipment(request.getEquipmentId());
        LocalDate date = parseDateOrToday(request.getUsageDate());
        double usageHours = requireNonNegative(request.getUsageHours());

        UsageRecord record = usageRecordRepository
                .findByEquipment_EquipmentCodeIgnoreCaseAndUsageDate(equipment.getEquipmentCode(), date)
                .orElseGet(UsageRecord::new);

        record.setEquipment(equipment);
        record.setUsageDate(date);
        record.setUsageHours(usageHours);
        record.setNotes(request.getNotes());
        record.setRecordedBy(recordedBy);

        return toResponse(usageRecordRepository.save(record));
    }

    /**
     * Batch save — one date, many equipment/usage-hours pairs, all in one
     * transaction. Lets a Gym Manager log every machine's hours for the
     * day on a single screen instead of visiting separate equipment pages.
     */
    @Transactional
    public List<UsageRecordResponse> batchUpsert(BatchUsageRequest request, String recordedBy) {
        LocalDate date = parseDateOrToday(request.getUsageDate());
        List<UsageRecordResponse> saved = new ArrayList<>();

        for (BatchUsageRequest.Entry entry : request.getEntries()) {
            Equipment equipment = findEquipment(entry.getEquipmentId());
            double usageHours = requireNonNegative(entry.getUsageHours());

            UsageRecord record = usageRecordRepository
                    .findByEquipment_EquipmentCodeIgnoreCaseAndUsageDate(equipment.getEquipmentCode(), date)
                    .orElseGet(UsageRecord::new);

            record.setEquipment(equipment);
            record.setUsageDate(date);
            record.setUsageHours(usageHours);
            record.setNotes(entry.getNotes());
            record.setRecordedBy(recordedBy);

            saved.add(toResponse(usageRecordRepository.save(record)));
        }

        return saved;
    }

    /** Edits an existing reading's usage hours only — equipment and date are the record's identity. */
    @Transactional
    public UsageRecordResponse updateById(Long id, UsageRecordRequest request, String requestedBy) {
        UsageRecord record = usageRecordRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usage record not found."));

        record.setUsageHours(requireNonNegative(request.getUsageHours()));
        if (request.getNotes() != null) record.setNotes(request.getNotes());
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

    private double requireNonNegative(Double value) {
        if (value == null || value < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Usage hours must be zero or greater.");
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
                r.getUsageHours(),
                r.getNotes(),
                r.getRecordedBy(),
                r.getRecordedAt().toString(),
                r.getUpdatedAt() == null ? null : r.getUpdatedAt().toString()
        );
    }

    /** No reading logged yet for this equipment/date — shown as 0 hours rather than omitted. */
    private UsageRecordResponse syntheticZeroResponse(Equipment eq, LocalDate date) {
        return new UsageRecordResponse(
                null,
                eq.getEquipmentCode(),
                eq.getEquipmentName(),
                eq.getCategory(),
                date.toString(),
                0.0,
                null,
                null,
                null,
                null
        );
    }
}
