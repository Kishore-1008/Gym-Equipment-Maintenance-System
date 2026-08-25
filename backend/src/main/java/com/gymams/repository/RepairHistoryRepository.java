package com.gymams.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gymams.model.RepairHistory;

public interface RepairHistoryRepository extends JpaRepository<RepairHistory, Long> {

    /** Admin's Repair History table — most recently completed first. */
    List<RepairHistory> findAllByOrderByCompletedAtDesc();

    /** Duplicate-prevention guard in RepairHistoryService.recordCompletion(). */
    boolean existsByRepairRequest_Id(Long repairRequestId);

    /**
     * Called before an Equipment row is deleted, so repair history never
     * dangles on a missing FK. Must run before RepairRequestRepository's
     * equivalent cascade-delete, since this table's repair_request_id FK
     * points at rows that delete would otherwise remove first.
     */
    void deleteAllByEquipment_Id(Long equipmentId);
}