package com.gymams.repository;

import com.gymams.model.Role;
import com.gymams.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);

    /** Used for the technician dropdown (Module 4/5 technician assignment) and TECH### code generation. */
    List<User> findAllByRoleOrderByTechnicianCodeAsc(Role role);

    /** Creation order (oldest first) — used only by the one-time technician-code backfill in UserService. */
    List<User> findAllByRoleOrderByIdAsc(Role role);
}
