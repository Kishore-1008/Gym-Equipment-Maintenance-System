package com.gymams.service;

import com.gymams.dto.ChangePasswordRequest;
import com.gymams.dto.RegisterRequest;
import com.gymams.exception.ApiException;
import com.gymams.model.Role;
import com.gymams.model.User;
import com.gymams.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final String TECHNICIAN_CODE_PREFIX = "TECH";
    private static final Pattern TECHNICIAN_CODE_PATTERN = Pattern.compile("^TECH(\\d+)$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * One-time startup fix for technicians created before the technicianCode
     * field existed (they have technicianCode = null).
     *
     * Root cause: generateNextTechnicianCode() correctly ignores null codes
     * when computing the next code for a *new* registration — but that same
     * filtering meant an existing null-code technician (e.g. "Siva") was
     * invisible to the max-scan, so the next technician registered
     * ("Rahul") got TECH001 instead of TECH002, and Siva was left at null
     * forever.
     *
     * Fix: if any TECHNICIAN currently has a null/blank code, renumber
     * every technician sequentially in creation order (oldest id first) —
     * so Siva (created first) gets TECH001 and Rahul (created second) gets
     * TECH002, matching who actually joined first. This only runs when an
     * inconsistency (a null code) is detected, so it's a no-op on every
     * later startup once fixed, and it never touches the gap-preserving
     * behavior generateNextTechnicianCode() already provides for ordinary
     * deletions going forward (that logic is untouched).
     *
     * Caveat: if any of these technicians were already assigned to a
     * repair/maintenance record before this fix ran, that record's
     * denormalized technician-code snapshot (assignedTechnicianCode) will
     * keep showing the old, pre-backfill code. That's a one-time, one-way
     * side effect of correcting historical data — new assignments made
     * after this fix runs are correct.
     *
     * Not @Transactional: @PostConstruct runs before Spring wraps this bean
     * in its transactional proxy, so a @Transactional annotation here
     * wouldn't actually be honored. Each userRepository.save() below is
     * still transactional on its own (Spring Data JPA repositories are
     * @Transactional per-method by default), which is sufficient for this
     * idempotent, self-healing fix.
     */
    @PostConstruct
    public void backfillMissingTechnicianCodes() {
        List<User> technicians = userRepository.findAllByRoleOrderByIdAsc(Role.TECHNICIAN);

        boolean hasMissingCode = technicians.stream()
                .anyMatch(u -> u.getTechnicianCode() == null || u.getTechnicianCode().isBlank());
        if (!hasMissingCode) {
            return;
        }

        int seq = 1;
        for (User technician : technicians) {
            String expectedCode = TECHNICIAN_CODE_PREFIX + String.format("%03d", seq);
            if (!expectedCode.equals(technician.getTechnicianCode())) {
                technician.setTechnicianCode(expectedCode);
                userRepository.save(technician);
            }
            seq++;
        }
    }

    @Transactional
    public User register(RegisterRequest request) {
        String username = request.getUsername().trim();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "That username is already taken.");
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select a valid role.");
        }

        String hash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getFullName().trim(), username, hash, role);

        // Only TECHNICIAN accounts get a Technician ID — ADMIN and GYM_MANAGER
        // keep technicianCode as null. Generated once, here, and never reused
        // or renumbered afterwards (see generateNextTechnicianCode()).
        if (role == Role.TECHNICIAN) {
            user.setTechnicianCode(generateNextTechnicianCode());
        }

        return userRepository.save(user);
    }

    /**
     * All registered technicians, for the Admin's technician-selection
     * dropdown (Module 4 "Assign a Technician" / Module 5 "Assign
     * Technician"). Ordered by technician code so the list is stable.
     */
    public List<User> findAllTechnicians() {
        return userRepository.findAllByRoleOrderByTechnicianCodeAsc(Role.TECHNICIAN);
    }

    /**
     * Looks up a TECHNICIAN account by username for assignment — rejects
     * anyone whose role isn't TECHNICIAN, even if the username exists,
     * so assignment is always restricted to the TECHNICIAN role.
     */
    public User findTechnicianByUsername(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Technician not found."));
        if (user.getRole() != Role.TECHNICIAN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selected user is not a technician.");
        }
        return user;
    }

    /**
     * Generates the next TECH### code by scanning existing technician codes
     * for the highest numeric suffix — mirrors EquipmentService's EQ### code
     * generation. A deleted TECH005 is never reissued while TECH006+ exist,
     * so IDs are never renumbered.
     */
    private synchronized String generateNextTechnicianCode() {
        int max = userRepository.findAllByRoleOrderByTechnicianCodeAsc(Role.TECHNICIAN).stream()
                .map(User::getTechnicianCode)
                .filter(code -> code != null && !code.isBlank())
                .map(TECHNICIAN_CODE_PATTERN::matcher)
                .filter(Matcher::matches)
                .mapToInt(m -> Integer.parseInt(m.group(1)))
                .max()
                .orElse(0);

        int next = max + 1;
        return TECHNICIAN_CODE_PREFIX + String.format("%03d", next);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password."));
    }

    public boolean matchesPassword(String rawPassword, String hash) {
        return passwordEncoder.matches(rawPassword, hash);
    }

    /**
     * Changes the password for the currently authenticated user.
     * {@code username} must come from the verified JWT (the controller passes
     * authentication.getName()), never from the request body, so a user can
     * only ever change their own password.
     */
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = findByUsername(username);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password and confirmation do not match.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password must be different from your current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
