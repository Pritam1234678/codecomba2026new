CREATE TABLE IF NOT EXISTS practice_submissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    code TEXT,
    language VARCHAR(20),
    status VARCHAR(20),
    submitted_at TIMESTAMP,
    time_consumed DOUBLE PRECISION,
    test_cases_passed INTEGER,
    total_test_cases INTEGER,
    score INTEGER,
    error_message TEXT,
    test_case_details TEXT,
    user_name VARCHAR(100),
    problem_name VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_ps_user_id ON practice_submissions(user_id);
CREATE INDEX IF NOT EXISTS idx_ps_problem_id ON practice_submissions(problem_id);
CREATE INDEX IF NOT EXISTS idx_ps_user_problem ON practice_submissions(user_id, problem_id);
