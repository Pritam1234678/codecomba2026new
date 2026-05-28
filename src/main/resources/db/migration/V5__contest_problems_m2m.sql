-- ─────────────────────────────────────────────────────────────────────────────
-- V5: Problem ↔ Contest many-to-many junction
--
-- Introduces the `contest_problems` junction so a single problem can belong
-- to multiple contests. The pre-existing `problems.contest_id` column is
-- preserved (and its FK kept in place) for the dual-write transition window;
-- it is made nullable here so standalone problems can exist. A future V6
-- will drop it once the application has stabilised.
--
-- The migration is fully idempotent: CREATE … IF NOT EXISTS for the table /
-- index, DO-blocks guarded by pg_constraint for the FKs, and
-- ON CONFLICT DO NOTHING for the backfill. Safe to re-run against a
-- partially-applied schema.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS public.contest_problems (
    contest_id    bigint    NOT NULL,
    problem_id    bigint    NOT NULL,
    display_order integer   NOT NULL DEFAULT 0,
    added_at      timestamp NOT NULL DEFAULT NOW(),
    PRIMARY KEY (contest_id, problem_id)
);

-- Foreign keys created separately so a partial prior run (table without FKs)
-- is recoverable without dropping the table.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_cp_contest'
    ) THEN
        ALTER TABLE public.contest_problems
            ADD CONSTRAINT fk_cp_contest
            FOREIGN KEY (contest_id) REFERENCES public.contests(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_cp_problem'
    ) THEN
        ALTER TABLE public.contest_problems
            ADD CONSTRAINT fk_cp_problem
            FOREIGN KEY (problem_id) REFERENCES public.problems(id) ON DELETE CASCADE;
    END IF;
END$$;

-- Reverse-lookup index. (contest_id is already the leading column of the PK,
-- so a separate index on contest_id would be redundant.)
CREATE INDEX IF NOT EXISTS idx_contest_problems_problem_id
    ON public.contest_problems (problem_id);

-- Backfill every existing membership. ON CONFLICT keeps the migration
-- replay-safe — re-running against a DB that already has the rows is a
-- no-op rather than an error.
INSERT INTO public.contest_problems (contest_id, problem_id, display_order, added_at)
SELECT contest_id, id, 0, NOW()
  FROM public.problems
 WHERE contest_id IS NOT NULL
ON CONFLICT (contest_id, problem_id) DO NOTHING;

-- Allow standalone problems. The FK fkpa9waom3ntotn7gpm2spnsow5 from
-- problems.contest_id → contests(id) is intentionally left in place so
-- legacy code paths still observe a consistent value during the transition.
ALTER TABLE public.problems
    ALTER COLUMN contest_id DROP NOT NULL;
