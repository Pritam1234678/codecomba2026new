ALTER TABLE problem_solutions ADD COLUMN codes TEXT;
UPDATE problem_solutions SET codes = ('{"' || language || '":"' || REPLACE(code, '"', '\"') || '"}') WHERE language IS NOT NULL;
ALTER TABLE problem_solutions ALTER COLUMN codes SET NOT NULL;
ALTER TABLE problem_solutions DROP COLUMN language;
ALTER TABLE problem_solutions DROP COLUMN code;
