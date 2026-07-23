CREATE TABLE IF NOT EXISTS problem_solutions (
    id          BIGSERIAL PRIMARY KEY,
    problem_id  BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    user_name   VARCHAR(255),
    language    VARCHAR(20) NOT NULL,
    code        TEXT NOT NULL,
    explanation TEXT,
    image_url   VARCHAR(1024),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ps_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE
);

CREATE INDEX idx_ps_problem ON problem_solutions(problem_id);
