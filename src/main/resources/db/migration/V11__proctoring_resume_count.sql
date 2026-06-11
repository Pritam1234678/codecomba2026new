-- V11: Resume support for proctored sessions.
--
-- When a candidate accidentally refreshes / closes the tab mid-contest, the
-- session row stays active (endedAt IS NULL). Instead of hard-blocking re-entry
-- with ALREADY_ACTIVE, we now let the candidate RESUME the same session a
-- limited number of times. resume_count tracks how many times the candidate has
-- rejoined; the API caps it at 2 (third attempt is refused).
ALTER TABLE proctoring_sessions
    ADD COLUMN resume_count INTEGER NOT NULL DEFAULT 0;
