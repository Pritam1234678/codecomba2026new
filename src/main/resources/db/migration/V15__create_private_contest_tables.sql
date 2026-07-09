-- ─────────────────────────────────────────────────────────────────────────────
-- V15: Private Contest Hosting Tables
--
-- Creates database schema for private contest hosting feature:
-- - contest_hosting_requests: Admin-approved hosting requests
-- - private_contests: Extension table linking contests to hosts
-- - private_contest_invitations: Unique invite tokens per contest
-- - private_contest_participants: Users who have joined via invitation
--
-- Business Rules Enforced:
-- - Monthly quota: 2 private contests per host (enforced at application layer)
-- - Participant limit: 100 per contest (enforced at application layer)
-- - Duration limit: 5 hours max (enforced at application layer)
-- - Unique constraint: One user per contest participation
-- ─────────────────────────────────────────────────────────────────────────────

-- ── Table 1: contest_hosting_requests ────────────────────────────────────────
-- Tracks user requests to become Contest_Hosts, requiring Admin approval.
CREATE TABLE contest_hosting_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Request details
    reason TEXT,
    intended_use_case VARCHAR(50) NOT NULL 
        CHECK (intended_use_case IN ('EDUCATION', 'RECRUITMENT', 'COMMUNITY', 'INTERNAL_TRAINING', 'OTHER')),
    
    -- Status tracking
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'REVOKED')),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Admin review
    reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP,
    admin_notes TEXT,
    
    -- Audit timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for contest_hosting_requests
CREATE INDEX idx_hosting_requests_status ON contest_hosting_requests(status);
CREATE INDEX idx_hosting_requests_user ON contest_hosting_requests(user_id);
CREATE INDEX idx_hosting_requests_submitted ON contest_hosting_requests(submitted_at);

-- ── Table 2: private_contests ────────────────────────────────────────────────
-- Extension table linking a contests row to its Contest_Host and hosting metadata.
-- 1:1 relationship with contests table via contest_id unique constraint.
CREATE TABLE private_contests (
    id BIGSERIAL PRIMARY KEY,
    contest_id BIGINT NOT NULL UNIQUE REFERENCES contests(id) ON DELETE CASCADE,
    host_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Business rules
    enable_proctoring BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    
    -- Tracking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for private_contests
CREATE INDEX idx_private_contests_host ON private_contests(host_user_id);
CREATE INDEX idx_private_contests_contest ON private_contests(contest_id);
CREATE INDEX idx_private_contests_created ON private_contests(created_at);

-- ── Table 3: private_contest_invitations ─────────────────────────────────────
-- Stores unique, time-limited invite tokens for each private contest.
-- Token: Cryptographically random (32 bytes, base64url-encoded) = 64 chars
CREATE TABLE private_contest_invitations (
    id BIGSERIAL PRIMARY KEY,
    contest_id BIGINT NOT NULL REFERENCES contests(id) ON DELETE CASCADE,
    
    -- Token (must be unique across all invitations)
    token VARCHAR(64) NOT NULL UNIQUE,
    
    -- Lifecycle
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    invalidated BOOLEAN NOT NULL DEFAULT FALSE
);

-- Indexes for private_contest_invitations
CREATE INDEX idx_invitations_token ON private_contest_invitations(token);
CREATE INDEX idx_invitations_contest ON private_contest_invitations(contest_id);
CREATE INDEX idx_invitations_expires ON private_contest_invitations(expires_at);
CREATE INDEX idx_invitations_invalidated ON private_contest_invitations(invalidated);

-- ── Table 4: private_contest_participants ────────────────────────────────────
-- Tracks which users have accepted invitations and joined a private contest.
-- Unique constraint prevents duplicate joins.
CREATE TABLE private_contest_participants (
    id BIGSERIAL PRIMARY KEY,
    contest_id BIGINT NOT NULL REFERENCES contests(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Prevent duplicate joins (one user per contest)
    CONSTRAINT uq_participant_per_contest UNIQUE (contest_id, user_id)
);

-- Indexes for private_contest_participants
CREATE INDEX idx_participants_contest ON private_contest_participants(contest_id);
CREATE INDEX idx_participants_user ON private_contest_participants(user_id);
CREATE INDEX idx_participants_joined ON private_contest_participants(joined_at);

-- ── Performance Notes ────────────────────────────────────────────────────────
-- Token lookups: O(1) via idx_invitations_token unique index
-- Host contest queries: O(log n) via idx_private_contests_host
-- Participant list: O(log n) via idx_participants_contest
-- Expiry cleanup: Scheduled job uses idx_invitations_expires
-- Status filtering: idx_hosting_requests_status for admin dashboard
