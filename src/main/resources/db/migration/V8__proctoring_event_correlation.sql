-- ─────────────────────────────────────────────────────────────────────────────
-- V8: Server-side replay dedup by client_correlation_id
--
-- Adds a nullable client_correlation_id column to proctoring_events and a
-- partial unique index on (session_id, client_correlation_id) so the server
-- can detect duplicate replayed frames with hard idempotency (task 9.3 in
-- proctored-contest-mode/tasks.md → Req 11.2, 11.3).
--
-- The browser generates a UUID per emitted Suspicious_Event at original
-- capture time; the WS handler propagates it into ProctoringEventService,
-- which on a replayed frame looks up the existing row and skips the
-- INSERT + score delta if a row already exists for the same
-- (session_id, client_correlation_id) pair.
--
-- The column is nullable for backward compatibility with frames that
-- pre-date the correlation id (e.g. the synthetic HEARTBEAT_TIMEOUT event
-- emitted server-side, or any in-flight frames during the rollout). The
-- partial unique index excludes NULLs so legacy rows do not collide.
--
-- Idempotent: ADD COLUMN IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE public.proctoring_events
    ADD COLUMN IF NOT EXISTS client_correlation_id varchar(64) NULL;

-- Partial unique index: enforces dedup only when a correlation id is present.
-- A NULL correlation id is allowed multiple times (rows with no correlation
-- id are not part of the replay dedup contract).
CREATE UNIQUE INDEX IF NOT EXISTS uq_pe_session_correlation
    ON public.proctoring_events (session_id, client_correlation_id)
    WHERE client_correlation_id IS NOT NULL;
