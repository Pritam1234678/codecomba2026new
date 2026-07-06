-- V14: Drop display_name column — replaced by fullName from auth profile
ALTER TABLE users
    DROP COLUMN IF EXISTS display_name;
