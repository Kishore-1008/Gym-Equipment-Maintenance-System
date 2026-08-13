package com.gymams.repository;

import com.gymams.model.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {

    Optional<UsageRecord> findByEquipment_EquipmentCodeIgnoreCaseAndUsageDate(String equipmentCode, LocalDate usageDate);

    List<UsageRecord> findAllByOrderByUsageDateDesc();

    List<UsageRecord> findAllByUsageDateOrderByEquipment_EquipmentCodeAsc(LocalDate usageDate);

    List<UsageRecord> findAllByEquipment_EquipmentCodeIgnoreCaseOrderByUsageDateDesc(String equipmentCode);

    List<UsageRecord> findAllByUsageDateBetweenOrderByUsageDateDesc(LocalDate from, LocalDate to);

    List<UsageRecord> findAllByEquipment_EquipmentCodeIgnoreCaseAndUsageDateBetweenOrderByUsageDateAsc(
            String equipmentCode, LocalDate from, LocalDate to);
}
