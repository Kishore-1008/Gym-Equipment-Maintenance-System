package com.gymams.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gymams.model.UsageRecord;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {

    Optional<UsageRecord> findByEquipment_EquipmentCodeIgnoreCaseAndUsageDate(
            String equipmentCode, LocalDate usageDate);

    /**
     * All equipment's readings for one specific day —
     * backs the "Today's Usage" table and batch-entry prefill.
     */
    List<UsageRecord> findAllByUsageDateOrderByEquipment_EquipmentCodeAsc(
            LocalDate usageDate);

    /**
     * All readings within a date range.
     */
    List<UsageRecord> findAllByUsageDateBetweenOrderByUsageDateDesc(
            LocalDate from, LocalDate to);

    /**
     * Delete all usage records belonging to an equipment.
     * This is required before deleting the equipment itself
     * because usage_record has a foreign key to equipment.
     */
    void deleteAllByEquipment_Id(Long equipmentId);
}