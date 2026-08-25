package com.gymams.controller;

import com.gymams.dto.TechnicianResponse;
import com.gymams.model.User;
import com.gymams.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ADMIN-only (enforced in SecurityConfig). Backs the technician-selection
 * dropdown used by both Module 4 ("Assign a Technician") and Module 5
 * ("Assign Technician") — a single source of truth for the "Name — TECH001"
 * display label so the frontend never re-derives it.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/technicians")
    public List<TechnicianResponse> technicians() {
        return userService.findAllTechnicians().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private TechnicianResponse toResponse(User user) {
        return new TechnicianResponse(user.getUsername(), user.getFullName(), user.getTechnicianCode());
    }
}
