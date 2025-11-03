-- V2__Create_roles_table.sql (MySQL Version)

-- Create roles table
CREATE TABLE `roles` (
                         `roleId` INT AUTO_INCREMENT PRIMARY KEY,
                         `roleName` VARCHAR(64) UNIQUE NOT NULL,
                         `description` VARCHAR(255),
                         `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create user_roles junction table
CREATE TABLE `userRoles` (
                             `userRoleId` INT AUTO_INCREMENT PRIMARY KEY,
                             `userId` VARCHAR(36) NOT NULL,
                             `roleId` INT NOT NULL,
                             `assignedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             `assignedBy` VARCHAR(36),
                             UNIQUE KEY unique_user_role (`userId`, `roleId`),
                             FOREIGN KEY (`userId`) REFERENCES `users`(`userId`) ON DELETE CASCADE,
                             FOREIGN KEY (`roleId`) REFERENCES `roles`(`roleId`) ON DELETE CASCADE,
                             FOREIGN KEY (`assignedBy`) REFERENCES `users`(`userId`) ON DELETE SET NULL,
                             INDEX idx_user_roles_user_id (`userId`),
                             INDEX idx_user_roles_role_id (`roleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default roles
INSERT INTO `roles` (`roleName`, `description`) VALUES
                                                    ('ADMIN', 'System administrator with full access'),
                                                    ('OWNER', 'Hotel owner who can manage their properties'),
                                                    ('CUSTOMER', 'Regular customer who can book hotels');

-- Create owner applications table
CREATE TABLE `ownerApplications` (
                                     `applicationId` VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
                                     `userId` VARCHAR(36) NOT NULL,
                                     `businessName` VARCHAR(255) NOT NULL,
                                     `businessAddress` VARCHAR(500),
                                     `businessLicense` VARCHAR(255),
                                     `businessLicenseUrl` TEXT,
                                     `identityCardUrl` TEXT,
                                     `phoneNumber` VARCHAR(32),
                                     `taxCode` VARCHAR(50),
                                     `description` TEXT,
                                     `status` VARCHAR(32) DEFAULT 'PENDING',
                                     `rejectionReason` TEXT,
                                     `reviewedBy` VARCHAR(36),
                                     `reviewedAt` DATETIME,
                                     `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     `updatedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     FOREIGN KEY (`userId`) REFERENCES `users`(`userId`) ON DELETE CASCADE,
                                     FOREIGN KEY (`reviewedBy`) REFERENCES `users`(`userId`) ON DELETE SET NULL,
                                     INDEX idx_owner_apps_user_id (`userId`),
                                     INDEX idx_owner_apps_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;