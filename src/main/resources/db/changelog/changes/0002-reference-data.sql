--liquibase formatted sql

-- Reference data the application needs in every environment (replaces config/DataInitializer).
-- Each changeset is skipped (MARK_RAN) on databases that already contain the rows.

--changeset travelmate:0002-roles
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM roles
--comment: Roles used by Spring Security (stored without the ROLE_ prefix)
INSERT INTO roles (roleName, createdAt) VALUES
  ('CUSTOMER', NOW(6)),
  ('ADMIN', NOW(6)),
  ('OWNER', NOW(6));
--rollback DELETE FROM roles WHERE roleName IN ('CUSTOMER', 'ADMIN', 'OWNER');

--changeset travelmate:0002-amenities
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM Amenity
--comment: Amenity catalogue; names are codes the frontend maps to labels/icons
INSERT INTO Amenity (amenityName, amenityType, isActive) VALUES
  ('pool', 'PROPERTY', b'1'),
  ('parking', 'PROPERTY', b'1'),
  ('sauna', 'PROPERTY', b'1'),
  ('spa', 'PROPERTY', b'1'),
  ('non_smoking', 'PROPERTY', b'1'),
  ('wifi', 'PROPERTY', b'1'),
  ('airport_transfer', 'PROPERTY', b'1'),
  ('pets', 'PROPERTY', b'1'),
  ('gym', 'PROPERTY', b'1'),
  ('smoking_area', 'PROPERTY', b'1'),
  ('reception_24h', 'PROPERTY', b'1'),
  ('ac', 'PROPERTY', b'1'),
  ('tv', 'ROOM', b'1'),
  ('ac', 'ROOM', b'1'),
  ('minibar', 'ROOM', b'1'),
  ('tea_coffee', 'ROOM', b'1'),
  ('wifi', 'ROOM', b'1'),
  ('bathtub', 'ROOM', b'1'),
  ('balcony', 'ROOM', b'1'),
  ('non_smoking', 'ROOM', b'1');
--rollback DELETE FROM Amenity;
