package com.example.smart_booking_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roleId")
    private Integer roleId;

    @Column(name = "roleName", unique = true, nullable = false, length = 64)
    private String roleName;

    @Column(name = "description")
    private String description;

    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructor for easy creation
    public Role(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }
}
