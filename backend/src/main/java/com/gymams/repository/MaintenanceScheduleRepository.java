package com.gymams.repository;

import com.gymams.model.MaintenanceSchedule;
import com.gymams.model.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Long> {

    List<MaintenanceSchedule> findAllByOrderByMaintenanceDateDesc();

    /** Technician's own assigned maintenance work (Module 5, "View Assigned Maintenance"). */
    List<MaintenanceSchedule> findByAssignedTechnicianUsernameOrderByMaintenanceDateAsc(String assignedTechnicianUsername);

    /**
     * Whether this equipment currently has a maintenance record in the given
     * status — used by EquipmentService.recomputeStatus() to decide between
     * UNDER_MAINTENANCE (an IN_PROGRESS record) and MAINTENANCE_DUE (a
     * SCHEDULED-but-not-started record). CANCELLED/COMPLETED are terminal
     * and never block.
     */
    boolean existsByEquipment_IdAndStatus(Long equipmentId, MaintenanceStatus status);

    /** Called before an Equipment row is deleted, so maintenance history never dangles on a missing FK. */
    void deleteAllByEquipment_Id(Long equipmentId);
}
