package com.gymams.controller;

import com.gymams.dto.EquipmentRequest;
import com.gymams.dto.EquipmentResponse;
import com.gymams.dto.EquipmentStatusRequest;
import com.gymams.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GET is open to any authenticated user (see SecurityConfig); write
 * operations are additionally restricted to ADMIN there, so this
 * controller doesn't need its own role checks.
 */
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public List<EquipmentResponse> getAll() {
        return equipmentService.findAll();
    }

    @PostMapping
    public ResponseEntity<EquipmentResponse> create(@Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.create(request));
    }

    @PutMapping("/{equipmentId}")
    public EquipmentResponse update(@PathVariable String equipmentId, @Valid @RequestBody EquipmentRequest request) {
        return equipmentService.update(equipmentId, request);
    }

    /**
     * Admin's dedicated "Change Equipment Status" action (Module 4) — lets
     * the Admin move equipment between OPERATIONAL / UNDER_MAINTENANCE /
     * OUT_OF_SERVICE during the repair workflow without resubmitting the
     * full Add/Edit Equipment form.
     */
    @PatchMapping("/{equipmentId}/status")
    public EquipmentResponse updateStatus(@PathVariable String equipmentId, @Valid @RequestBody EquipmentStatusRequest request) {
        return equipmentService.updateStatus(equipmentId, request);
    }

    @DeleteMapping("/{equipmentId}")
    public ResponseEntity<Void> delete(@PathVariable String equipmentId) {
        equipmentService.delete(equipmentId);
        return ResponseEntity.noContent().build();
    }
}
