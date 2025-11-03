package com.example.smart_booking_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "userId", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "fullName")
    private String fullName;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "passwordHash", nullable = false)
    private String passwordHash;

    @Column(name = "phoneNumber", length = 32)
    private String phoneNumber;

    @Column(name = "isEmailVerified")
    private Boolean isEmailVerified = false;

    @Column(name = "twoFactorEnabled")
    private Boolean twoFactorEnabled = false;

    @Column(name = "twoFactorSecret")
    private String twoFactorSecret;

    @Column(name = "status", length = 32)
    private String status = "INACTIVE"; // INACTIVE, ACTIVE, SUSPENDED, BANNED

    @Column(name = "verificationToken", length = 500)
    private String verificationToken;

    @Column(name = "verificationTokenExpiry")
    private LocalDateTime verificationTokenExpiry;

    @Column(name = "resetPasswordToken", length = 500)
    private String resetPasswordToken;

    @Column(name = "resetPasswordTokenExpiry")
    private LocalDateTime resetPasswordTokenExpiry;

    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @Getter
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "userRoles",
            joinColumns = @JoinColumn(name = "userId"),
            inverseJoinColumns = @JoinColumn(name = "roleId")
    )
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "INACTIVE";
        }
        if (isEmailVerified == null) {
            isEmailVerified = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getRoleName().equals(roleName));
    }

}
