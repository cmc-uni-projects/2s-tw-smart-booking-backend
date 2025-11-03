-- V1__Create_users_table.sql (MySQL Version)

-- Create users table
CREATE TABLE `users` (
                         `userId` VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
                         `fullName` VARCHAR(255),
                         `email` VARCHAR(255) UNIQUE NOT NULL,
                         `passwordHash` VARCHAR(255) NOT NULL,
                         `phoneNumber` VARCHAR(32),
                         `isEmailVerified` BOOLEAN DEFAULT FALSE,
                         `twoFactorEnabled` BOOLEAN DEFAULT FALSE,
                         `twoFactorSecret` VARCHAR(255),
                         `status` VARCHAR(32) DEFAULT 'ACTIVE',
                         `verificationToken` VARCHAR(500),
                         `verificationTokenExpiry` DATETIME,
                         `resetPasswordToken` VARCHAR(500),
                         `resetPasswordTokenExpiry` DATETIME,
                         `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         `updatedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         INDEX idx_users_email (`email`),
                         INDEX idx_users_status (`status`),
                         INDEX idx_users_verification_token (`verificationToken`(255)),
                         INDEX idx_users_reset_token (`resetPasswordToken`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;