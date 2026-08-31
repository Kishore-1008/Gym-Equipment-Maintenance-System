package com.gymams.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gymams.model.Warranty;

public interface WarrantyRepository extends JpaRepository<Warranty, Long> {

    /** Admin's Warranty Management table — most recently added/updated logic lives in the service; this just gives a stable base ordering. */
    List<Warranty> findAllByOrderByExpiryDateAsc();

    /** Called before an Equipment row is deleted, so warranty records never dangle on a missing FK. */
    void deleteAllByEquipment_Id(Long equipmentId);
}