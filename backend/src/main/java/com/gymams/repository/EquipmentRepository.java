package com.gymams.repository;

import com.gymams.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    Optional<Equipment> findByEquipmentCodeIgnoreCase(String equipmentCode);
    List<Equipment> findAllByOrderByEquipmentCodeAsc();
}
