ALTER TABLE problems ADD COLUMN IF NOT EXISTS topics VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_problems_topics ON problems(topics);
