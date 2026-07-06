-- V13: Social profile fields for users
-- All columns are nullable — the profile is optional and editable after creation.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS bio           TEXT,
    ADD COLUMN IF NOT EXISTS title         VARCHAR(120),
    ADD COLUMN IF NOT EXISTS location      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS company       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS github_url    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS linkedin_url  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS instagram_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS twitter_url   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS website_url   VARCHAR(255);
