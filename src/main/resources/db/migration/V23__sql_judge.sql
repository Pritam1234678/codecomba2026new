-- SQL Judge — question metadata + submissions.
--
-- Question metadata (sql_problems) lives in the CodeCoder application DB.
-- The actual datasets (q_<id> schemas) live on the six external Neon nodes,
-- provisioned by SqlProblemProvisioningService. expected_result is the
-- normalized output of officialSolutionSql, computed once at publish time.
--
-- provisioning_status is a JSON map nodeId -> {status, error, at} so a
-- partially-failed publish can be retried node-by-node without a distributed
-- transaction.

CREATE TABLE IF NOT EXISTS sql_problems (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    schema_name VARCHAR(64) NOT NULL,
    setup_sql TEXT,
    official_solution_sql TEXT,
    expected_result TEXT,
    comparison_mode VARCHAR(16) NOT NULL DEFAULT 'UNORDERED',
    time_limit_ms INT NOT NULL DEFAULT 2000,
    max_result_rows INT NOT NULL DEFAULT 500,
    provisioning_status TEXT,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sql_problems_enabled ON sql_problems(enabled);

-- Candidate SQL submissions. status values:
--   QUEUED, RUNNING, ACCEPTED, WRONG_ANSWER, TIME_LIMIT_EXCEEDED,
--   RUNTIME_ERROR, SECURITY_VIOLATION, INTERNAL_ERROR
-- result_preview holds a small sanitized result preview for test runs (RUN)
-- so the polling endpoint can render it even after the queue drained. Real
-- result sets are never persisted wholesale.
CREATE TABLE IF NOT EXISTS sql_submissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    problem_id BIGINT NOT NULL REFERENCES sql_problems(id),
    submitted_sql TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'QUEUED',
    execution_time_ms BIGINT,
    selected_node VARCHAR(64),
    error_message TEXT,
    result_preview TEXT,
    test_run BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sql_subs_user ON sql_submissions(user_id);
CREATE INDEX IF NOT EXISTS idx_sql_subs_problem ON sql_submissions(problem_id);
CREATE INDEX IF NOT EXISTS idx_sql_subs_user_problem ON sql_submissions(user_id, problem_id);
