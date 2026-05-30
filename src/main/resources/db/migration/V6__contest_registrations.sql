-- ─────────────────────────────────────────────────────────────────────────────
-- V6: Contest Registration table
--
-- Tracks which users have explicitly registered for which contests.
-- A user must be registered before they can participate (submit solutions).
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS public.contest_registrations (
    id            bigserial   PRIMARY KEY,
    contest_id    bigint      NOT NULL,
    user_id       bigint      NOT NULL,
    registered_at timestamp   NOT NULL DEFAULT NOW(),
    UNIQUE (contest_id, user_id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_cr_contest'
    ) THEN
        ALTER TABLE public.contest_registrations
            ADD CONSTRAINT fk_cr_contest
            FOREIGN KEY (contest_id) REFERENCES public.contests(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_cr_user'
    ) THEN
        ALTER TABLE public.contest_registrations
            ADD CONSTRAINT fk_cr_user
            FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_cr_contest_id ON public.contest_registrations (contest_id);
CREATE INDEX IF NOT EXISTS idx_cr_user_id    ON public.contest_registrations (user_id);
