--liquibase formatted sql

-- Baseline: the schema Hibernate generated from the JPA entities (ddl-auto=update) before Liquibase was introduced.
-- Databases that already have these tables (e.g. the deployed TiDB) mark this changeset as ran without executing it.
-- Tables are ordered so every foreign key points to a table created earlier.

--changeset travelmate:0001-baseline-schema
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users'
--comment: Initial schema (25 tables)

CREATE TABLE `users` (
  `userId` char(36) NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `fullName` varchar(255) DEFAULT NULL,
  `isEmailVerified` bit(1) DEFAULT NULL,
  `membershipRank` enum('BRONZE','DIAMOND','GOLD','SILVER') DEFAULT NULL,
  `notificationEmail` varchar(255) DEFAULT NULL,
  `passwordHash` varchar(255) NOT NULL,
  `phoneNumber` varchar(32) DEFAULT NULL,
  `points` int NOT NULL,
  `provider` enum('facebook','google','local') DEFAULT NULL,
  `providerId` varchar(255) DEFAULT NULL,
  `resetPasswordToken` varchar(500) DEFAULT NULL,
  `resetPasswordTokenExpiry` datetime(6) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `twoFactorEnabled` bit(1) DEFAULT NULL,
  `twoFactorSecret` varchar(255) DEFAULT NULL,
  `updatedAt` datetime(6) DEFAULT NULL,
  `verificationToken` varchar(500) DEFAULT NULL,
  `verificationTokenExpiry` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`userId`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `roles` (
  `roleId` int NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `roleName` varchar(64) NOT NULL,
  PRIMARY KEY (`roleId`),
  UNIQUE KEY `UKovpqau8r3lgfnmak7xyel2bt0` (`roleName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `userRoles` (
  `userId` char(36) NOT NULL,
  `roleId` int NOT NULL,
  PRIMARY KEY (`userId`,`roleId`),
  KEY `FKne7e7xkwg8jw19u3f79bxuguv` (`roleId`),
  CONSTRAINT `FKgqpfwr3766gtqga2i0kgmlwlu` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`),
  CONSTRAINT `FKne7e7xkwg8jw19u3f79bxuguv` FOREIGN KEY (`roleId`) REFERENCES `roles` (`roleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `userdetail` (
  `userdetailId` int NOT NULL AUTO_INCREMENT,
  `address` text,
  `city` varchar(100) DEFAULT NULL,
  `country` varchar(100) DEFAULT NULL,
  `createdAt` datetime(6) NOT NULL,
  `dateOfBirth` date DEFAULT NULL,
  `gender` varchar(50) DEFAULT NULL,
  `isActive` bit(1) NOT NULL,
  `profilePhotoUrl` varchar(512) DEFAULT NULL,
  `updatedAt` datetime(6) DEFAULT NULL,
  `userId` char(36) NOT NULL,
  PRIMARY KEY (`userdetailId`),
  UNIQUE KEY `UKsiqx74bfrii77j3tvdt1d2f3u` (`userId`),
  CONSTRAINT `FKjae8196dj3loludyw2g8wqmgb` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `social_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `provider` enum('facebook','google','local') NOT NULL,
  `providerId` varchar(255) NOT NULL,
  `user_id` char(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6rmxxiton5yuvu7ph2hcq2xn7` (`user_id`),
  CONSTRAINT `FK6rmxxiton5yuvu7ph2hcq2xn7` FOREIGN KEY (`user_id`) REFERENCES `users` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `BlackListedTokens` (
  `token` varchar(512) NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `expiresAt` datetime(6) NOT NULL,
  PRIMARY KEY (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `registerOwner` (
  `registerOwnerid` bigint NOT NULL AUTO_INCREMENT,
  `adminReason` text,
  `businessLicenseImage` varchar(512) NOT NULL,
  `businessLicenseNumber` varchar(255) NOT NULL,
  `cardBackImage` varchar(512) NOT NULL,
  `cardFrontImage` varchar(512) NOT NULL,
  `createAt` datetime(6) NOT NULL,
  `hometown_address` text NOT NULL,
  `permanent_address` text NOT NULL,
  `personalDob` date DEFAULT NULL,
  `personalIdCard` varchar(255) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') NOT NULL,
  `reviewed_by_admin_id` char(36) DEFAULT NULL,
  `userId` char(36) NOT NULL,
  PRIMARY KEY (`registerOwnerid`),
  KEY `FKs1ii2ri6xg29d5xuhbgroinpu` (`reviewed_by_admin_id`),
  KEY `FKml0ggkq5w4o2jeotjy1tfyird` (`userId`),
  CONSTRAINT `FKml0ggkq5w4o2jeotjy1tfyird` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`),
  CONSTRAINT `FKs1ii2ri6xg29d5xuhbgroinpu` FOREIGN KEY (`reviewed_by_admin_id`) REFERENCES `users` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `Amenity` (
  `amenityId` int NOT NULL AUTO_INCREMENT,
  `amenityName` varchar(255) DEFAULT NULL,
  `amenityType` enum('PROPERTY','ROOM') DEFAULT NULL,
  `isActive` bit(1) NOT NULL,
  PRIMARY KEY (`amenityId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `properties` (
  `propertyId` int NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `createdAt` date DEFAULT NULL,
  `description` text,
  `district` varchar(255) DEFAULT NULL,
  `districtCode` varchar(255) DEFAULT NULL,
  `emailContact` varchar(255) DEFAULT NULL,
  `isActive` bit(1) NOT NULL,
  `latitude` decimal(9,6) DEFAULT NULL,
  `longitude` decimal(9,6) DEFAULT NULL,
  `phoneContact` varchar(255) DEFAULT NULL,
  `postalCode` varchar(255) DEFAULT NULL,
  `propertyName` varchar(255) DEFAULT NULL,
  `propertyStatus` enum('APPROVE','PENDING','REJECTED','SUSPENDED') DEFAULT NULL,
  `propertyType` enum('HOMESTAY','HOTEL','RESORT','VILLA') DEFAULT NULL,
  `province` varchar(255) DEFAULT NULL,
  `provinceCode` varchar(255) DEFAULT NULL,
  `rating` decimal(2,1) DEFAULT NULL,
  `reviewCount` int NOT NULL,
  `updatedAt` date DEFAULT NULL,
  `ward` varchar(255) DEFAULT NULL,
  `userId` char(36) DEFAULT NULL,
  PRIMARY KEY (`propertyId`),
  KEY `FKpnors2ixe2tiim43ylxey1gxe` (`userId`),
  CONSTRAINT `FKpnors2ixe2tiim43ylxey1gxe` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `property_details` (
  `propertyDetailId` int NOT NULL AUTO_INCREMENT,
  `area` decimal(10,2) DEFAULT NULL,
  `propertyId` int NOT NULL,
  PRIMARY KEY (`propertyDetailId`),
  UNIQUE KEY `UKsx8sxb8r2vfmwf8xujn4i4xsx` (`propertyId`),
  CONSTRAINT `FK8xsygy1ey7s09a2irx4jrt34x` FOREIGN KEY (`propertyId`) REFERENCES `properties` (`propertyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `property_policies` (
  `policyId` int NOT NULL AUTO_INCREMENT,
  `allowFreeCancellation` bit(1) NOT NULL,
  `cancellationPolicyDescription` text,
  `checkInTime` time(6) DEFAULT NULL,
  `checkOutTime` time(6) DEFAULT NULL,
  `childrenAllowed` bit(1) NOT NULL,
  `childrenPolicyDescription` text,
  `freeCancellationDays` int DEFAULT NULL,
  `minimumAge` int DEFAULT NULL,
  `petPolicyDescription` text,
  `petsAllowed` bit(1) NOT NULL,
  `prepaymentPolicy` text,
  `quietHours` varchar(255) DEFAULT NULL,
  `requiresPrepayment` bit(1) NOT NULL,
  `securityDepositAmount` decimal(38,2) DEFAULT NULL,
  `securityDepositDescription` text,
  `securityDepositRequired` bit(1) NOT NULL,
  `smokingAllowed` bit(1) NOT NULL,
  `smokingPolicyDescription` text,
  `propertyId` int DEFAULT NULL,
  PRIMARY KEY (`policyId`),
  UNIQUE KEY `UK5u6bsgqebseafc44y49nyh8xj` (`propertyId`),
  CONSTRAINT `FK2oq99n28i8d5nbw8oykpmayde` FOREIGN KEY (`propertyId`) REFERENCES `properties` (`propertyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `propertyimage` (
  `propertyImageId` int NOT NULL AUTO_INCREMENT,
  `imageUrl` varchar(255) DEFAULT NULL,
  `isActive` bit(1) NOT NULL,
  `isCover` bit(1) NOT NULL,
  `propertyId` int DEFAULT NULL,
  PRIMARY KEY (`propertyImageId`),
  KEY `FK5e52swvmjikyfhogpwt6gg5sp` (`propertyId`),
  CONSTRAINT `FK5e52swvmjikyfhogpwt6gg5sp` FOREIGN KEY (`propertyId`) REFERENCES `properties` (`propertyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `PropertyAmenity` (
  `propertyAmenityId` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `amenityId` int DEFAULT NULL,
  `propertyId` int DEFAULT NULL,
  PRIMARY KEY (`propertyAmenityId`),
  KEY `FKb548pa7sabcottbqfkdsgpaqh` (`amenityId`),
  KEY `FKs656kbqreeks3xy054m9ibjnx` (`propertyId`),
  CONSTRAINT `FKb548pa7sabcottbqfkdsgpaqh` FOREIGN KEY (`amenityId`) REFERENCES `Amenity` (`amenityId`),
  CONSTRAINT `FKs656kbqreeks3xy054m9ibjnx` FOREIGN KEY (`propertyId`) REFERENCES `properties` (`propertyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `rooms` (
  `roomId` int NOT NULL AUTO_INCREMENT,
  `isActive` bit(1) DEFAULT NULL,
  `area` decimal(38,2) DEFAULT NULL,
  `capacity` int DEFAULT NULL,
  `createdDate` date DEFAULT NULL,
  `description` text,
  `pricePerNight` decimal(38,2) DEFAULT NULL,
  `roomAmount` int DEFAULT NULL,
  `roomCategory` enum('DELUXE','STANDARD','SUITE','WHOLE') DEFAULT NULL,
  `roomName` varchar(255) DEFAULT NULL,
  `roomStatus` enum('AVAILABLE','BOOKED','MAINTENANCE','SUSPENDED') DEFAULT NULL,
  `weekendPrice` decimal(38,2) DEFAULT NULL,
  `property_id` int DEFAULT NULL,
  PRIMARY KEY (`roomId`),
  KEY `FK35r032kwh410ggyqcbqrnhcut` (`property_id`),
  CONSTRAINT `FK35r032kwh410ggyqcbqrnhcut` FOREIGN KEY (`property_id`) REFERENCES `properties` (`propertyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `roomimage` (
  `roomImageId` int NOT NULL AUTO_INCREMENT,
  `imageUrl` varchar(255) DEFAULT NULL,
  `isActive` bit(1) NOT NULL,
  `isCover` bit(1) NOT NULL,
  `roomId` int DEFAULT NULL,
  PRIMARY KEY (`roomImageId`),
  KEY `FK7k8qsrjfy54tyvdkq4dhfi10m` (`roomId`),
  CONSTRAINT `FK7k8qsrjfy54tyvdkq4dhfi10m` FOREIGN KEY (`roomId`) REFERENCES `rooms` (`roomId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `RoomAmenity` (
  `roomAmenityId` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `amenityId` int DEFAULT NULL,
  `roomId` int DEFAULT NULL,
  PRIMARY KEY (`roomAmenityId`),
  KEY `FKcab4k5f42feq3gpqrpqx49lj2` (`amenityId`),
  KEY `FKh35ea63iv9dnixnkk41f8vyn8` (`roomId`),
  CONSTRAINT `FKcab4k5f42feq3gpqrpqx49lj2` FOREIGN KEY (`amenityId`) REFERENCES `Amenity` (`amenityId`),
  CONSTRAINT `FKh35ea63iv9dnixnkk41f8vyn8` FOREIGN KEY (`roomId`) REFERENCES `rooms` (`roomId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `promotions` (
  `promotionId` int NOT NULL AUTO_INCREMENT,
  `bannerUrl` varchar(512) DEFAULT NULL,
  `code` varchar(50) NOT NULL,
  `createdAt` datetime(6) DEFAULT NULL,
  `description` text,
  `discountType` enum('FIXED_AMOUNT','PERCENTAGE') NOT NULL,
  `discountValue` decimal(38,2) NOT NULL,
  `endDate` datetime(6) NOT NULL,
  `maxDiscountAmount` decimal(38,2) DEFAULT NULL,
  `minBookingAmount` decimal(38,2) DEFAULT NULL,
  `minMembershipRank` enum('BRONZE','DIAMOND','GOLD','SILVER') DEFAULT NULL,
  `startDate` datetime(6) NOT NULL,
  `status` enum('ACTIVE','DELETED','EXPIRED','PAUSED') NOT NULL,
  `updatedAt` datetime(6) DEFAULT NULL,
  `usageCount` int DEFAULT NULL,
  `usageLimit` int DEFAULT NULL,
  `propertyId` int DEFAULT NULL,
  PRIMARY KEY (`promotionId`),
  UNIQUE KEY `UK6e42y712tlp3hyn066qm46cpr` (`code`,`propertyId`),
  KEY `FKibp0xakc6mcy7ig85nqirv4qd` (`propertyId`),
  CONSTRAINT `FKibp0xakc6mcy7ig85nqirv4qd` FOREIGN KEY (`propertyId`) REFERENCES `properties` (`propertyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `bookings` (
  `bookingId` int NOT NULL AUTO_INCREMENT,
  `adminPromotionCode` varchar(50) DEFAULT NULL,
  `checkInDate` date DEFAULT NULL,
  `checkOutDate` date DEFAULT NULL,
  `createdAt` datetime(6) DEFAULT NULL,
  `customerEmail` varchar(255) DEFAULT NULL,
  `customerName` varchar(255) DEFAULT NULL,
  `customerPhone` varchar(255) DEFAULT NULL,
  `discountAmount` decimal(15,2) DEFAULT NULL,
  `guestCount` int DEFAULT NULL,
  `penaltyAmount` decimal(15,2) DEFAULT NULL,
  `promotionCode` varchar(50) DEFAULT NULL,
  `refundAmount` decimal(15,2) DEFAULT NULL,
  `specialRequest` varchar(255) DEFAULT NULL,
  `status` enum('CANCELLED','CHECKED_IN','COMPLETED','CONFIRMED','PENDING_PAYMENT') DEFAULT NULL,
  `totalPrice` decimal(15,2) DEFAULT NULL,
  `updatedAt` datetime(6) DEFAULT NULL,
  `propertyId` int DEFAULT NULL,
  `roomId` int DEFAULT NULL,
  `userId` char(36) DEFAULT NULL,
  PRIMARY KEY (`bookingId`),
  KEY `FKbye0insyb31h5ymfydc3908gk` (`propertyId`),
  KEY `FKi7ofsq9l3l9jgdwg8ricxprou` (`roomId`),
  KEY `FK2rgb3wqoda92euxecb2agubec` (`userId`),
  CONSTRAINT `FK2rgb3wqoda92euxecb2agubec` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`),
  CONSTRAINT `FKbye0insyb31h5ymfydc3908gk` FOREIGN KEY (`propertyId`) REFERENCES `properties` (`propertyId`),
  CONSTRAINT `FKi7ofsq9l3l9jgdwg8ricxprou` FOREIGN KEY (`roomId`) REFERENCES `rooms` (`roomId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `payment` (
  `paymentId` int NOT NULL AUTO_INCREMENT,
  `confirmedDate` datetime(6) DEFAULT NULL,
  `createdAt` datetime(6) NOT NULL,
  `note` text,
  `paymentDate` datetime(6) DEFAULT NULL,
  `paymentEvidenceUrl` varchar(512) DEFAULT NULL,
  `paymentMethod` varchar(50) DEFAULT NULL,
  `paymentStatus` enum('APPROVED','PENDING','REFUNDED','REFUND_REQUESTED','REJECTED') NOT NULL,
  `refundedAmount` decimal(15,2) DEFAULT NULL,
  `totalAmount` decimal(15,2) NOT NULL,
  `transactionReference` varchar(100) DEFAULT NULL,
  `updatedAt` datetime(6) DEFAULT NULL,
  `bookingId` int DEFAULT NULL,
  PRIMARY KEY (`paymentId`),
  UNIQUE KEY `UKmpbd1mlbjoa7fp7s3va01sub2` (`bookingId`),
  CONSTRAINT `FK3k350sfmsswd6hwpi5a0s24qy` FOREIGN KEY (`bookingId`) REFERENCES `bookings` (`bookingId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refund_requests` (
  `id` int NOT NULL AUTO_INCREMENT,
  `accountHolder` varchar(255) DEFAULT NULL,
  `accountNumber` varchar(255) DEFAULT NULL,
  `adminNote` text,
  `amount` decimal(15,2) DEFAULT NULL,
  `bankName` varchar(255) DEFAULT NULL,
  `reason` text,
  `refundImage` varchar(255) DEFAULT NULL,
  `requestDate` datetime(6) DEFAULT NULL,
  `resolveDate` datetime(6) DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') DEFAULT NULL,
  `bookingId` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6eacooi6gj9uubf11hikqwf9u` (`bookingId`),
  CONSTRAINT `FKf790y9kpykdm24h3ytufmikbe` FOREIGN KEY (`bookingId`) REFERENCES `bookings` (`bookingId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `rating` (
  `ratingId` int NOT NULL AUTO_INCREMENT,
  `comment` varchar(255) DEFAULT NULL,
  `createdAt` datetime(6) DEFAULT NULL,
  `isHidden` bit(1) DEFAULT NULL,
  `is_pinned` bit(1) DEFAULT NULL,
  `rating` int DEFAULT NULL,
  `ratingType` tinyint DEFAULT NULL,
  `bookings` int DEFAULT NULL,
  `users` char(36) DEFAULT NULL,
  PRIMARY KEY (`ratingId`),
  KEY `FKl04ysejr7kfdcnctuesbs4d73` (`bookings`),
  KEY `FK574idvmk4glg22w1e3anriinm` (`users`),
  CONSTRAINT `FK574idvmk4glg22w1e3anriinm` FOREIGN KEY (`users`) REFERENCES `users` (`userId`),
  CONSTRAINT `FKl04ysejr7kfdcnctuesbs4d73` FOREIGN KEY (`bookings`) REFERENCES `bookings` (`bookingId`),
  CONSTRAINT `rating_chk_1` CHECK ((`ratingType` between 0 and 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `ratingImages` (
  `imageId` int NOT NULL AUTO_INCREMENT,
  `imageUrl` varchar(255) DEFAULT NULL,
  `ratingId` int DEFAULT NULL,
  PRIMARY KEY (`imageId`),
  KEY `FKt3v7fqk99157mmc2dema66b4h` (`ratingId`),
  CONSTRAINT `FKt3v7fqk99157mmc2dema66b4h` FOREIGN KEY (`ratingId`) REFERENCES `rating` (`ratingId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) DEFAULT NULL,
  `is_read` bit(1) NOT NULL,
  `message` text,
  `relatedEntityId` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` enum('ACCOUNT_UPDATE','ADMIN_NEW_OWNER_REGISTRATION','ADMIN_NEW_PROPERTY_SUBMISSION','ADMIN_NEW_REFUND_REQUEST','ADMIN_SYSTEM_ALERT','APPROVAL','BOOKING_CANCELLED','BOOKING_CANCELLED_BY_GUEST','BOOKING_FAILED','BOOKING_RECEIVED','BOOKING_SUCCESS','GENERAL','PAYMENT_SUCCESS','PAYOUT_SUCCESS','PROMOTION','PROPERTY_SUSPENDED','REFUND_PROCESSED','REJECTION','REMINDER_CHECKIN','REVENUE_REPORT','ROOM_SUSPENDED','SECURITY_ALERT','SYSTEM') DEFAULT NULL,
  `user_id` char(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9y21adhxn0ayjhfocscqox7bh` (`user_id`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `search_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `searchTerm` varchar(255) NOT NULL,
  `userId` char(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgi9o11nwxv5mk77fhuykn6ae1` (`userId`),
  CONSTRAINT `FKgi9o11nwxv5mk77fhuykn6ae1` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `system_logs` (
  `logId` bigint NOT NULL AUTO_INCREMENT,
  `action` enum('CREATE','DELETE','UPDATE') NOT NULL,
  `createdAt` datetime(6) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `entityId` varchar(255) NOT NULL,
  `entityType` enum('PROPERTY','ROOM') NOT NULL,
  `newValue` text,
  `oldValue` text,
  `userId` char(36) NOT NULL,
  PRIMARY KEY (`logId`),
  KEY `FK9bbpisrw797vgawls5c0exa3v` (`userId`),
  CONSTRAINT `FK9bbpisrw797vgawls5c0exa3v` FOREIGN KEY (`userId`) REFERENCES `users` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--rollback SET FOREIGN_KEY_CHECKS = 0;
--rollback DROP TABLE `system_logs`, `search_history`, `notifications`, `ratingImages`, `rating`, `refund_requests`, `payment`, `bookings`, `promotions`, `RoomAmenity`, `roomimage`, `rooms`, `PropertyAmenity`, `propertyimage`, `property_policies`, `property_details`, `properties`, `Amenity`, `registerOwner`, `BlackListedTokens`, `social_accounts`, `userdetail`, `userRoles`, `roles`, `users`;
--rollback SET FOREIGN_KEY_CHECKS = 1;
