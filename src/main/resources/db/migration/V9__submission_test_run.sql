-- "Run" (test run) previously saved a submissions row indistinguishable from a
-- real submission. Those rows leaked into the dashboard, the "latest submission"
-- lookup, and — critically — the contest leaderboard query. Add an explicit flag
-- so test runs can be persisted (the polling fallback still reads the row by id)
-- yet excluded from every user/contest/problem-facing read.
ALTER TABLE submissions ADD COLUMN is_test_run BOOLEAN NOT NULL DEFAULT FALSE;

-- Hot path: "give me this user's real submissions" and the leaderboard scan both
-- filter on is_test_run, so index it alongside the existing user/problem indexes.
CREATE INDEX idx_submission_user_testrun ON submissions (user_id, is_test_run);
