package com.gymams.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "username", nullable = false, unique = true, length = 20)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /**
     * Auto-generated business identifier (e.g. TECH001) for TECHNICIAN
     * accounts only — null/empty for ADMIN and GYM_MANAGER. Generated once
     * at registration time by UserService and never renumbered, even if
     * other technicians are later removed. Nullable + unique (MySQL allows
     * multiple NULLs in a unique column, so ADMIN/GYM_MANAGER rows are
     * unaffected).
     */
    @Column(name = "technician_code", unique = true, length = 10)
    private String technicianCode;

    public User() {}

    public User(String fullName, String username, String passwordHash, Role role) {
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getTechnicianCode() { return technicianCode; }
    public void setTechnicianCode(String technicianCode) { this.technicianCode = technicianCode; }
}
