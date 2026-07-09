-- ─────────────────────────────────────────────────────────────────────────────
-- U15: Rollback Private Contest Hosting Tables
--
-- This is a rollback script for V15__create_private_contest_tables.sql
-- It drops all tables and related objects created in V15 in reverse dependency order.
--
-- Note: Flyway's undo migrations require Flyway Teams Edition. This file is
-- provided for documentation purposes and manual rollback scenarios only.
-- ─────────────────────────────────────────────────────────────────────────────

-- Drop tables in reverse dependency order to avoid foreign key constraint errors

-- Drop participants table first (references contests and users)
DROP TABLE IF EXISTS private_contest_participants CASCADE;

-- Drop invitations table (references contests)
DROP TABLE IF EXISTS private_contest_invitations CASCADE;

-- Drop private_contests table (references contests and users)
DROP TABLE IF EXISTS private_contests CASCADE;

-- Drop hosting requests table (references users)
DROP TABLE IF EXISTS contest_hosting_requests CASCADE;

-- Note: This script does not drop the sequences as they will be automatically
-- removed when the tables are dropped (due to OWNED BY relationship from SERIAL).
