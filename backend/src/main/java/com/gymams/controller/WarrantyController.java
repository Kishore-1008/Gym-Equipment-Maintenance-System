package com.gymams.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gymams.dto.WarrantyRequest;
import com.gymams.dto.WarrantyResponse;
import com.gymams.service.WarrantyService;

import jakarta.validation.Valid;

/**
 * Module 7 — Warranty Management. ADMIN-only (enforced in SecurityConfig),
 * same URL-naming convention as the rest of the app (/api/<resource>,
 * plural, matching /api/equipment, /api/repair-requests, /api/maintenance).
 */
@RestController
@RequestMapping("/api/warranties")
public class WarrantyController {

    private final WarrantyService warrantyService;

    public WarrantyController(WarrantyService warrantyService) {
        this.warrantyService = warrantyService;
    }

    @PostMapping
    public ResponseEntity<WarrantyResponse> create(@Valid @RequestBody WarrantyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warrantyService.create(request));
    }

    @GetMapping
    public List<WarrantyResponse> all() {
        return warrantyService.findAll();
    }

    @GetMapping("/{id}")
    public WarrantyResponse byId(@PathVariable Long id) {
        return warrantyService.findById(id);
    }

    @PutMapping("/{id}")
    public WarrantyResponse update(@PathVariable Long id, @Valid @RequestBody WarrantyRequest request) {
        return warrantyService.update(id, request);
    }
}