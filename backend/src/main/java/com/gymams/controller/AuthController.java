package com.gymams.controller;

import com.gymams.dto.AuthResponse;
import com.gymams.dto.ChangePasswordRequest;
import com.gymams.dto.LoginRequest;
import com.gymams.dto.MessageResponse;
import com.gymams.dto.RegisterRequest;
import com.gymams.exception.ApiException;
import com.gymams.model.User;
import com.gymams.security.JwtUtil;
import com.gymams.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /** No token is issued on register — the user still has to log in, matching the existing UX. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(null, user.getFullName(), user.getUsername(), user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());

        if (!userService.matchesPassword(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token, user.getFullName(), user.getUsername(), user.getRole().name()));
    }

    /**
     * Available to any authenticated user (ADMIN, GYM_MANAGER, or TECHNICIAN) for
     * their own account only. The username comes from the verified JWT via
     * Spring Security's Authentication, never from the request body — a user
     * cannot change anyone else's password through this endpoint.
     */
    @PutMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(Authentication authentication,
                                                            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully."));
    }
}
