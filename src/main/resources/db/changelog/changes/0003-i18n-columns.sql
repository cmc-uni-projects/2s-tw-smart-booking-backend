--liquibase formatted sql

--changeset travelmate:0003-users-preferred-language
--comment: UI/email language of each user (vi | en); emails, schedulers and notifications use it
ALTER TABLE users ADD COLUMN preferredLanguage varchar(10) NOT NULL DEFAULT 'vi';
--rollback ALTER TABLE users DROP COLUMN preferredLanguage;

--changeset travelmate:0003-notifications-message-keys
--comment: Notifications store an i18n key + JSON params and are rendered in the reader's language. title/message keep a pre-rendered copy (recipient's language) and stay the only text for older rows.
ALTER TABLE notifications
  ADD COLUMN titleKey varchar(150) NULL,
  ADD COLUMN messageKey varchar(150) NULL,
  ADD COLUMN params text NULL;
--rollback ALTER TABLE notifications DROP COLUMN titleKey, DROP COLUMN messageKey, DROP COLUMN params;
