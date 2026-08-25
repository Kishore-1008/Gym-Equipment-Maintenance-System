package com.gymams.repository;

import com.gymams.model.RepairRequest;
import com.gymams.model.RepairStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RepairRequestRepository extends JpaRepository<RepairRequest, Long> {

    List<RepairRequest> findAllByOrderBySubmittedAtDesc();

    /** Gym Manager's own submitted requests (Module 4, "View Submitted Repair Requests"). */
    List<RepairRequest> findByReportedByUsernameOrderBySubmittedAtDesc(String reportedByUsername);

    /** Technician's own assigned repair work (Module 4, "View Assigned Repair Requests"). */
    List<RepairRequest> findByAssignedTechnicianUsernameOrderByAssignedAtDesc(String assignedTechnicianUsername);

    /**
     * Whether this equipment currently has any repair request in one of the
     * given statuses — used by EquipmentService.recomputeStatus() to decide
     * whether the equipment should show UNDER_REPAIR. Typically called with
     * {APPROVED, ASSIGNED, IN_PROGRESS} (PENDING doesn't block usage yet —
     * the Admin hasn't confirmed the problem; REJECTED/COMPLETED are terminal).
     */
    boolean existsByEquipment_IdAndStatusIn(Long equipmentId, Collection<RepairStatus> statuses);

    /** Called before an Equipment row is deleted, so repair history never dangles on a missing FK. */
    void deleteAllByEquipment_Id(Long equipmentId);
}
