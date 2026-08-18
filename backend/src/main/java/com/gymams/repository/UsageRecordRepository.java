package com.gymams.repository;

import com.gymams.model.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {

    Optional<UsageRecord> findByEquipment_EquipmentCodeIgnoreCaseAndUsageDate(String equipmentCode, LocalDate usageDate);

    /** All equipment's readings for one specific day — backs the "Today's Usage" table and batch-entry prefill. */
    List<UsageRecord> findAllByUsageDateOrderByEquipment_EquipmentCodeAsc(LocalDate usageDate);

    /** All readings within a date range (used to sum the current month) across all equipment. */
    List<UsageRecord> findAllByUsageDateBetweenOrderByUsageDateDesc(LocalDate from, LocalDate to);
}
