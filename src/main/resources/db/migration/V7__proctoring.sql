-- ─────────────────────────────────────────────────────────────────────────────
-- V7: Proctored Contest Mode
--
-- Adds six tables behind the proctoring layer:
--   - proctored_contests        (1:1 extension on contests)
--   - proctoring_sessions       (one per candidate attempt)
--   - proctoring_events         (per-event log, retained 30 days)
--   - proctoring_screenshots    (per-event JPEG metadata, retained 30 days)
--   - proctoring_consent_acks   (per-(user,contest,version) consent log)
--   - proctoring_admin_audit    (force-end / warning audit)
--
-- No existing tables are altered. Idempotent under IF NOT EXISTS / DO blocks.
-- Style mirrors V5 (contest_problems) and V6 (contest_registrations).
-- ─────────────────────────────────────────────────────────────────────────────

-- ── proctored_contests ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.proctored_contests (
    id              bigserial PRIMARY KEY,
    contest_id      bigint    NOT NULL,
    created_at      timestamp NOT NULL DEFAULT NOW(),
    consent_version integer   NOT NULL DEFAULT 1,
    CONSTRAINT uq_proctored_contests_contest_id UNIQUE (contest_id)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pc_contest') THEN
        ALTER TABLE public.proctored_contests
            ADD CONSTRAINT fk_pc_contest
            FOREIGN KEY (contest_id) REFERENCES public.contests(id) ON DELETE CASCADE;
    END IF;
END$$;

-- ── proctoring_sessions ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.proctoring_sessions (
    id               bigserial   PRIMARY KEY,
    contest_id       bigint      NOT NULL,
    user_id          bigint      NOT NULL,
    started_at       timestamp   NOT NULL,
    ended_at         timestamp   NULL,
    end_reason       varchar(32) NULL,
    risk_score       integer     NOT NULL DEFAULT 0,
    risk_band        varchar(8)  NOT NULL DEFAULT 'LOW',
    flagged          boolean     NOT NULL DEFAULT FALSE,
    client_ip        varchar(45) NULL,
    consent_version  integer     NOT NULL,
    CONSTRAINT uq_proctoring_sessions_contest_user UNIQUE (contest_id, user_id),
    CONSTRAINT ck_proctoring_sessions_end_reason CHECK (
        end_reason IS NULL OR end_reason IN
        ('CONTEST_ENDED','SELF_FINISHED','SELF_QUIT','ADMIN_FORCED','HEARTBEAT_TIMEOUT')
    ),
    CONSTRAINT ck_proctoring_sessions_risk_band CHECK (
        risk_band IN ('LOW','MEDIUM','HIGH')
    ),
    CONSTRAINT ck_proctoring_sessions_ended_when_reason CHECK (
        (ended_at IS NULL AND end_reason IS NULL)
        OR (ended_at IS NOT NULL AND end_reason IS NOT NULL)
    )
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ps_contest') THEN
        ALTER TABLE public.proctoring_sessions
            ADD CONSTRAINT fk_ps_contest
            FOREIGN KEY (contest_id) REFERENCES public.contests(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ps_user') THEN
        ALTER TABLE public.proctoring_sessions
            ADD CONSTRAINT fk_ps_user
            FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_ps_contest_flagged ON public.proctoring_sessions (contest_id, flagged);
CREATE INDEX IF NOT EXISTS idx_ps_user_ended      ON public.proctoring_sessions (user_id, ended_at);

-- ── proctoring_events ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.proctoring_events (
    id                bigserial   PRIMARY KEY,
    session_id        bigint      NOT NULL,
    event_type        varchar(32) NOT NULL,
    client_timestamp  timestamp   NOT NULL,
    server_timestamp  timestamp   NOT NULL DEFAULT NOW(),
    payload_json      jsonb       NULL,
    replayed          boolean     NOT NULL DEFAULT FALSE,
    score_delta       integer     NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pe_session') THEN
        ALTER TABLE public.proctoring_events
            ADD CONSTRAINT fk_pe_session
            FOREIGN KEY (session_id) REFERENCES public.proctoring_sessions(id) ON DELETE CASCADE;
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_pe_session_server_ts ON public.proctoring_events (session_id, server_timestamp);
CREATE INDEX IF NOT EXISTS idx_pe_type_server_ts    ON public.proctoring_events (event_type, server_timestamp);

-- ── proctoring_screenshots ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.proctoring_screenshots (
    id            bigserial    PRIMARY KEY,
    session_id    bigint       NOT NULL,
    event_id      bigint       NOT NULL,
    captured_at   timestamp    NOT NULL,
    mime_type     varchar(32)  NOT NULL,
    byte_size     integer      NOT NULL,
    storage_ref   varchar(255) NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pshot_session') THEN
        ALTER TABLE public.proctoring_screenshots
            ADD CONSTRAINT fk_pshot_session
            FOREIGN KEY (session_id) REFERENCES public.proctoring_sessions(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pshot_event') THEN
        ALTER TABLE public.proctoring_screenshots
            ADD CONSTRAINT fk_pshot_event
            FOREIGN KEY (event_id) REFERENCES public.proctoring_events(id) ON DELETE CASCADE;
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_pshot_session     ON public.proctoring_screenshots (session_id);
CREATE INDEX IF NOT EXISTS idx_pshot_captured_at ON public.proctoring_screenshots (captured_at);

-- ── proctoring_consent_acks ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.proctoring_consent_acks (
    id              bigserial    PRIMARY KEY,
    user_id         bigint       NOT NULL,
    contest_id      bigint       NOT NULL,
    consent_version integer      NOT NULL,
    accepted_at     timestamp    NOT NULL DEFAULT NOW(),
    client_ip       varchar(45)  NULL,
    user_agent      text         NULL,
    CONSTRAINT uq_pca_user_contest_version UNIQUE (user_id, contest_id, consent_version)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pca_user') THEN
        ALTER TABLE public.proctoring_consent_acks
            ADD CONSTRAINT fk_pca_user
            FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pca_contest') THEN
        ALTER TABLE public.proctoring_consent_acks
            ADD CONSTRAINT fk_pca_contest
            FOREIGN KEY (contest_id) REFERENCES public.contests(id) ON DELETE CASCADE;
    END IF;
END$$;

-- ── proctoring_admin_audit ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.proctoring_admin_audit (
    id          bigserial   PRIMARY KEY,
    admin_id    bigint      NOT NULL,
    session_id  bigint      NOT NULL,
    action      varchar(16) NOT NULL,
    acted_at    timestamp   NOT NULL DEFAULT NOW(),
    reason      text        NULL,
    CONSTRAINT ck_paa_action CHECK (action IN ('FORCE_END','WARNING'))
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_paa_admin') THEN
        ALTER TABLE public.proctoring_admin_audit
            ADD CONSTRAINT fk_paa_admin
            FOREIGN KEY (admin_id) REFERENCES public.users(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_paa_session') THEN
        ALTER TABLE public.proctoring_admin_audit
            ADD CONSTRAINT fk_paa_session
            FOREIGN KEY (session_id) REFERENCES public.proctoring_sessions(id) ON DELETE CASCADE;
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_paa_session ON public.proctoring_admin_audit (session_id);
