-- V3__Create_hotels_table.sql (MySQL Version)

-- Create hotels table
CREATE TABLE `hotels` (
                          `hotelId` VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
                          `ownerId` VARCHAR(36),
                          `name` VARCHAR(255) NOT NULL,
                          `slug` VARCHAR(255) UNIQUE,
                          `description` TEXT,
                          `address` VARCHAR(500),
                          `city` VARCHAR(128),
                          `country` VARCHAR(128) DEFAULT 'Vietnam',
                          `latitude` DECIMAL(9,6),
                          `longitude` DECIMAL(9,6),
                          `status` VARCHAR(32) DEFAULT 'PENDING',
                          `ratingAverage` DECIMAL(2,1) DEFAULT 0.0,
                          `totalReviews` INT DEFAULT 0,
                          `starRating` INT,
                          `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          `updatedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          `approvedBy` VARCHAR(36),
                          `approvedAt` DATETIME,
                          FOREIGN KEY (`ownerId`) REFERENCES `users`(`userId`) ON DELETE SET NULL,
                          FOREIGN KEY (`approvedBy`) REFERENCES `users`(`userId`) ON DELETE SET NULL,
                          INDEX idx_hotels_owner_id (`ownerId`),
                          INDEX idx_hotels_status (`status`),
                          INDEX idx_hotels_city (`city`),
                          INDEX idx_hotels_slug (`slug`),
                          INDEX idx_hotels_rating (`ratingAverage` DESC),
                          CONSTRAINT chk_star_rating CHECK (`starRating` >= 1 AND `starRating` <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create hotel images table
CREATE TABLE `hotelImages` (
                               `imageId` VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
                               `hotelId` VARCHAR(36) NOT NULL,
                               `url` TEXT NOT NULL,
                               `isCover` BOOLEAN DEFAULT FALSE,
                               `displayOrder` INT DEFAULT 0,
                               `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (`hotelId`) REFERENCES `hotels`(`hotelId`) ON DELETE CASCADE,
                               INDEX idx_hotel_images_hotel_id (`hotelId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create hotel policies table
CREATE TABLE `hotelPolicies` (
                                 `policyId` VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
                                 `hotelId` VARCHAR(36) NOT NULL,
                                 `checkInTime` TIME DEFAULT '14:00:00',
                                 `checkOutTime` TIME DEFAULT '12:00:00',
                                 `cancellationPolicy` TEXT,
                                 `childPolicy` TEXT,
                                 `petPolicy` TEXT,
                                 `smokingPolicy` TEXT,
                                 `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 `updatedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 FOREIGN KEY (`hotelId`) REFERENCES `hotels`(`hotelId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create amenities table
CREATE TABLE `amenities` (
                             `amenityId` INT AUTO_INCREMENT PRIMARY KEY,
                             `name` VARCHAR(128) UNIQUE NOT NULL,
                             `icon` VARCHAR(50),
                             `category` VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create hotel_amenities junction table
CREATE TABLE `hotelAmenities` (
                                  `hotelAmenityId` INT AUTO_INCREMENT PRIMARY KEY,
                                  `hotelId` VARCHAR(36) NOT NULL,
                                  `amenityId` INT NOT NULL,
                                  UNIQUE KEY unique_hotel_amenity (`hotelId`, `amenityId`),
                                  FOREIGN KEY (`hotelId`) REFERENCES `hotels`(`hotelId`) ON DELETE CASCADE,
                                  FOREIGN KEY (`amenityId`) REFERENCES `amenities`(`amenityId`) ON DELETE CASCADE,
                                  INDEX idx_hotel_amenities_hotel_id (`hotelId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;