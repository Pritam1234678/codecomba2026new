CREATE TABLE IF NOT EXISTS practice_sheets (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    company     VARCHAR(100),
    description TEXT,
    tags        VARCHAR(512),
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sheet_problems (
    id          BIGSERIAL PRIMARY KEY,
    sheet_id    BIGINT NOT NULL REFERENCES practice_sheets(id) ON DELETE CASCADE,
    problem_id  BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    sort_order  INT DEFAULT 0,
    added_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(sheet_id, problem_id)
);

CREATE INDEX idx_sp_sheet ON sheet_problems(sheet_id);
CREATE INDEX idx_sp_problem ON sheet_problems(problem_id);
