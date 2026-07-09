-- ─────────────────────────────────────────────────────────────────────────────
-- V17: Audit Logs Table
--
-- Creates the audit_logs table used by the Private Contest Hosting feature to
-- record critical lifecycle actions (hosting requests, contest create/cancel/delete,
-- participant join/remove, invite regeneration, admin actions) for compliance
-- and debugging.
--
-- The AuditLog entity (com.example.codecombat2026.entity.AuditLog) was introduced
-- with the private contest feature but its CREATE TABLE was never captured in a
-- migration — environments created via the old ddl-auto=update path already have
-- the table, while fresh databases (validate mode) fail schema validation without
-- it. This migration closes that gap.
--
-- Uses IF NOT EXISTS so it is safe on databases where the table already exists.
--
-- Business Rules:
-- - Append-only (no updates/deletes via application layer)
-- - Minimum retention: 90 days (enforced by scheduled cleanup job)
-- - user_id is nullable to allow system-triggered actions
--
-- Requirements: 2.6, 29.1, 29.2, 29.3
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,

    -- Actor (nullable for system-triggered actions such as scheduled cleanup)
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,

    -- Action performed, e.g. "CONTEST_CREATED", "PARTICIPANT_JOINED"
    action VARCHAR(100) NOT NULL,

    -- Resource affected, e.g. "PRIVATE_CONTEST", "HOSTING_REQUEST"
    resource_type VARCHAR(50) NOT NULL,
    resource_id BIGINT,

    -- When the action happened
    timestamp TIMESTAMP NOT NULL,

    -- Request context
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),

    -- Arbitrary JSON metadata for the action
    details_json TEXT
);

-- Indexes mirror the @Index definitions on the AuditLog entity
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
