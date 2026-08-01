CREATE TABLE IF NOT EXISTS problem_complaints (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    problem_id BIGINT NOT NULL REFERENCES problems(id),
    contest_id BIGINT,
    complaint_type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_complaints_user ON problem_complaints(user_id);
CREATE INDEX idx_complaints_status ON problem_complaints(status);
CREATE INDEX idx_complaints_problem ON problem_complaints(problem_id);
