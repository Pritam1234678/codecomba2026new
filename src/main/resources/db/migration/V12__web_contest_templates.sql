-- Web Contest Templates: stores metadata for multi-file web coding challenges
CREATE TABLE web_contest_templates (
    id BIGSERIAL PRIMARY KEY,
    problem_id BIGINT NOT NULL REFERENCES problems(id),
    language VARCHAR(20) NOT NULL,
    template_path VARCHAR(500) NOT NULL,
    manifest_json TEXT NOT NULL,
    test_count INT NOT NULL DEFAULT 6,
    timeout_seconds INT NOT NULL DEFAULT 60,
    memory_mb INT NOT NULL DEFAULT 512,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_web_contest_templates_problem_lang ON web_contest_templates(problem_id, language);
