-- timeLimit is canonically SECONDS (judge, AI generator, and UI all assume this).
-- A bug in the admin forms stored milliseconds for some problems (e.g. 5000, 100000),
-- which made the judge wait minutes-to-hours before declaring TLE — the verdict never
-- surfaced and the UI sat on "Still judging". Any stored value > 100 is therefore a
-- millisecond artifact: convert to seconds and clamp to a sane [1, 15]s window.
UPDATE problems
   SET time_limit = LEAST(15, GREATEST(1, ROUND(time_limit / 1000.0)))
 WHERE time_limit > 100;
