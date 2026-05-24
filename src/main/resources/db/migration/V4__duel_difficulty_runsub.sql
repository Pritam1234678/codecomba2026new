-- ─────────────────────────────────────────────────────────────────────────────
-- V4: Duel difficulty buckets + per-match time limit
--
-- Adds two columns to duel_matches so the matchmaker can route by difficulty
-- and the draw-timer can be picked from the row instead of a global @Value.
--
-- Old (v3) rows backfill to 'MEDIUM' / 600s via DEFAULT — no app-side change
-- required to read pre-V4 history rows.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE public.duel_matches
    ADD COLUMN IF NOT EXISTS difficulty VARCHAR(10) NOT NULL DEFAULT 'MEDIUM';

ALTER TABLE public.duel_matches
    ADD COLUMN IF NOT EXISTS time_limit_sec INT NOT NULL DEFAULT 600;

-- Defensive backfill — IF NOT EXISTS + DEFAULT means new rows already get
-- 'MEDIUM' / 600 but we explicitly set anything that somehow slipped through
-- (e.g. legacy rows inserted via raw SQL bypassing the column defaults).
UPDATE public.duel_matches
SET difficulty = COALESCE(difficulty, 'MEDIUM'),
    time_limit_sec = COALESCE(time_limit_sec, 600);

-- Difficulty bucket constraint added separately so it doesn't fail on the
-- ALTER above when the column already exists from a manual run.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'duel_matches_difficulty_check'
    ) THEN
        ALTER TABLE public.duel_matches
            ADD CONSTRAINT duel_matches_difficulty_check
            CHECK (difficulty IN ('EASY','MEDIUM','HARD'));
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_duel_matches_difficulty
    ON public.duel_matches (difficulty);
