-- ─────────────────────────────────────────────────────────────────────────────
-- V3: Live Duel Mode
--
-- Adds three tables (duel_matches, duel_submissions, duel_eligible_problems)
-- and the partial-unique-index armor that gates "one active duel per user"
-- and "one winner per match" at the database level.
--
-- No existing tables are altered. Submissions are linked to a duel via
-- duel_submissions (FK on submission_id, FK on match_id). Existing
-- submissions are unaffected.
--
-- Re-runnable by construction: every CREATE TABLE / CREATE INDEX uses
-- IF NOT EXISTS, the trigger function is CREATE OR REPLACE, and the
-- trigger itself is dropped before re-creation.
-- ─────────────────────────────────────────────────────────────────────────────

-- ── duel_matches ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.duel_matches (
    match_id        UUID         NOT NULL,
    user_a_id       BIGINT       NOT NULL,
    user_b_id       BIGINT       NOT NULL,
    problem_id      BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    outcome         VARCHAR(20),
    winner_user_id  BIGINT,
    started_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    ended_at        TIMESTAMP(6) WITHOUT TIME ZONE,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT duel_matches_pkey            PRIMARY KEY (match_id),
    CONSTRAINT duel_matches_status_check    CHECK (status IN ('WAITING','IN_PROGRESS','FINISHED')),
    CONSTRAINT duel_matches_outcome_check   CHECK (outcome IS NULL OR outcome IN ('USER_A_WIN','USER_B_WIN','DRAW','ABANDONED')),
    -- Req 2.3 / Req 9: outcome and winner_user_id agree
    CONSTRAINT duel_matches_winner_outcome_consistent CHECK (
        (outcome IS NULL)
        OR (outcome IN ('USER_A_WIN','USER_B_WIN') AND winner_user_id IS NOT NULL)
        OR (outcome IN ('DRAW','ABANDONED')        AND winner_user_id IS NULL)
    ),
    -- Req 2.6: deterministic seat ordering
    CONSTRAINT duel_matches_distinct_users     CHECK (user_a_id < user_b_id),
    -- Req 14.3: ended_at >= started_at when set
    CONSTRAINT duel_matches_time_monotonic    CHECK (
        ended_at IS NULL OR started_at IS NULL OR ended_at >= started_at
    ),
    -- Req 2.4: FINISHED rows must have outcome and ended_at
    CONSTRAINT duel_matches_finished_complete CHECK (
        status <> 'FINISHED' OR (outcome IS NOT NULL AND ended_at IS NOT NULL)
    ),
    -- Winner must be one of the two participants
    CONSTRAINT duel_matches_winner_is_participant CHECK (
        winner_user_id IS NULL
        OR winner_user_id = user_a_id
        OR winner_user_id = user_b_id
    ),
    CONSTRAINT duel_matches_user_a_fk    FOREIGN KEY (user_a_id)      REFERENCES public.users(id),
    CONSTRAINT duel_matches_user_b_fk    FOREIGN KEY (user_b_id)      REFERENCES public.users(id),
    CONSTRAINT duel_matches_problem_fk   FOREIGN KEY (problem_id)     REFERENCES public.problems(id),
    CONSTRAINT duel_matches_winner_fk    FOREIGN KEY (winner_user_id) REFERENCES public.users(id)
);

CREATE INDEX IF NOT EXISTS idx_duel_matches_user_a    ON public.duel_matches (user_a_id);
CREATE INDEX IF NOT EXISTS idx_duel_matches_user_b    ON public.duel_matches (user_b_id);
CREATE INDEX IF NOT EXISTS idx_duel_matches_status    ON public.duel_matches (status);
CREATE INDEX IF NOT EXISTS idx_duel_matches_started   ON public.duel_matches (started_at);

-- Req 10.1: at most one Active_Match per user — partial unique on each seat.
-- These also serve as the gate that backs Req 9.2 (the conditional UPDATE).
CREATE UNIQUE INDEX IF NOT EXISTS ux_duel_active_user_a
    ON public.duel_matches (user_a_id)
    WHERE status IN ('WAITING','IN_PROGRESS');
CREATE UNIQUE INDEX IF NOT EXISTS ux_duel_active_user_b
    ON public.duel_matches (user_b_id)
    WHERE status IN ('WAITING','IN_PROGRESS');

-- Req 9.2 immutability gate: once winner_user_id is set, no UPDATE may change it.
-- Postgres has no built-in column-immutability, so we enforce it with a trigger.
-- Also (Req 2.4 + Req 9.2) freezes the row entirely once status='FINISHED'.
CREATE OR REPLACE FUNCTION public.duel_matches_winner_immutable()
RETURNS TRIGGER AS $fn$
BEGIN
    IF OLD.winner_user_id IS NOT NULL AND NEW.winner_user_id IS DISTINCT FROM OLD.winner_user_id THEN
        RAISE EXCEPTION 'duel_matches.winner_user_id is immutable once set (match_id=%)', OLD.match_id;
    END IF;
    -- Req 2.4 + Req 9.2: once FINISHED, outcome / ended_at / status are also frozen.
    IF OLD.status = 'FINISHED' THEN
        IF NEW.outcome IS DISTINCT FROM OLD.outcome
           OR NEW.ended_at IS DISTINCT FROM OLD.ended_at
           OR NEW.status  IS DISTINCT FROM OLD.status THEN
            RAISE EXCEPTION 'duel_matches row is frozen after FINISHED (match_id=%)', OLD.match_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$fn$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_duel_matches_winner_immutable ON public.duel_matches;
CREATE TRIGGER trg_duel_matches_winner_immutable
    BEFORE UPDATE ON public.duel_matches
    FOR EACH ROW EXECUTE FUNCTION public.duel_matches_winner_immutable();

-- ── duel_submissions ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.duel_submissions (
    submission_id  BIGINT  NOT NULL,
    match_id       UUID    NOT NULL,
    is_first_ac    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT duel_submissions_pkey       PRIMARY KEY (submission_id),
    CONSTRAINT duel_submissions_sub_fk     FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE,
    CONSTRAINT duel_submissions_match_fk   FOREIGN KEY (match_id)      REFERENCES public.duel_matches(match_id)
);

CREATE INDEX IF NOT EXISTS idx_duel_submissions_match ON public.duel_submissions (match_id);

-- Req 6.1 / Req 12.2: at most one is_first_ac per (match, user). We enforce this
-- at the application layer because is_first_ac is per-(match, user) and the
-- user lives on the submissions row, not on duel_submissions. The application
-- guard is the DuelService.onDuelVerdict path which only flips is_first_ac=TRUE
-- on the winning submission (the one whose UPDATE wins the conditional gate).

-- ── duel_eligible_problems ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.duel_eligible_problems (
    problem_id  BIGINT       NOT NULL,
    added_at    TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    added_by    BIGINT,
    CONSTRAINT duel_eligible_problems_pkey         PRIMARY KEY (problem_id),
    CONSTRAINT duel_eligible_problems_problem_fk   FOREIGN KEY (problem_id) REFERENCES public.problems(id) ON DELETE CASCADE,
    CONSTRAINT duel_eligible_problems_added_by_fk  FOREIGN KEY (added_by)   REFERENCES public.users(id)
);
