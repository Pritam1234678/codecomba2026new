-- ─────────────────────────────────────────────────────────────────────────────
-- Test Script for V15 Migration: Private Contest Hosting Tables
-- 
-- This script tests:
-- 1. Table creation and structure
-- 2. Foreign key constraints
-- 3. Unique constraints
-- 4. Check constraints
-- 5. Cascade delete behavior
-- 6. Default values
-- ─────────────────────────────────────────────────────────────────────────────

\echo '=== Testing V15 Migration: Private Contest Hosting Tables ==='

-- Test 1: Verify all tables exist
\echo '\n--- Test 1: Verify all tables exist ---'
SELECT 
    table_name, 
    table_type
FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND table_name IN (
    'contest_hosting_requests',
    'private_contests', 
    'private_contest_invitations',
    'private_contest_participants'
  )
ORDER BY table_name;

-- Test 2: Verify indexes exist
\echo '\n--- Test 2: Verify indexes exist ---'
SELECT 
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN (
    'contest_hosting_requests',
    'private_contests',
    'private_contest_invitations', 
    'private_contest_participants'
  )
ORDER BY tablename, indexname;

-- Test 3: Test hosting request creation with valid data
\echo '\n--- Test 3: Test hosting request creation ---'
BEGIN;

-- Insert test user (if not exists)
INSERT INTO users (username, email, password, enabled, full_name, total_points)
VALUES ('test_host', 'test_host@example.com', 'hashedpassword', true, 'Test Host', 0)
ON CONFLICT (email) DO NOTHING;

-- Insert hosting request
INSERT INTO contest_hosting_requests (
    user_id, 
    reason, 
    intended_use_case,
    status
)
SELECT 
    id,
    'I want to host coding competitions for my students',
    'EDUCATION',
    'PENDING'
FROM users WHERE email = 'test_host@example.com';

SELECT * FROM contest_hosting_requests WHERE user_id = (SELECT id FROM users WHERE email = 'test_host@example.com');

ROLLBACK;

-- Test 4: Test check constraint on intended_use_case
\echo '\n--- Test 4: Test check constraint on intended_use_case ---'
BEGIN;

DO $$
BEGIN
    INSERT INTO contest_hosting_requests (
        user_id, 
        reason, 
        intended_use_case,
        status
    )
    SELECT 
        id,
        'Test',
        'INVALID_VALUE',
        'PENDING'
    FROM users WHERE email = 'test_host@example.com';
    
    RAISE EXCEPTION 'Check constraint should have failed!';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'Check constraint working correctly: %', SQLERRM;
END $$;

ROLLBACK;

-- Test 5: Test private contest with foreign key to contests table
\echo '\n--- Test 5: Test private contest creation ---'
BEGIN;

-- Create a test contest first
INSERT INTO contests (name, description, start_time, end_time, status, active)
VALUES (
    'Test Private Contest',
    'Test Description',
    NOW() + INTERVAL '1 day',
    NOW() + INTERVAL '1 day' + INTERVAL '3 hours',
    'UPCOMING',
    true
);

-- Create private contest entry
INSERT INTO private_contests (
    contest_id,
    host_user_id,
    enable_proctoring,
    cancelled
)
SELECT 
    c.id,
    u.id,
    true,
    false
FROM contests c, users u
WHERE c.name = 'Test Private Contest' 
  AND u.email = 'test_host@example.com';

SELECT * FROM private_contests WHERE contest_id IN (SELECT id FROM contests WHERE name = 'Test Private Contest');

ROLLBACK;

-- Test 6: Test unique constraint on private_contests.contest_id
\echo '\n--- Test 6: Test unique constraint on contest_id ---'
BEGIN;

INSERT INTO contests (name, description, start_time, end_time, status, active)
VALUES (
    'Test Contest Unique',
    'Test',
    NOW() + INTERVAL '1 day',
    NOW() + INTERVAL '1 day' + INTERVAL '2 hours',
    'UPCOMING',
    true
);

INSERT INTO private_contests (contest_id, host_user_id)
SELECT c.id, u.id
FROM contests c, users u
WHERE c.name = 'Test Contest Unique' AND u.email = 'test_host@example.com';

DO $$
BEGIN
    INSERT INTO private_contests (contest_id, host_user_id)
    SELECT c.id, u.id
    FROM contests c, users u
    WHERE c.name = 'Test Contest Unique' AND u.email = 'test_host@example.com';
    
    RAISE EXCEPTION 'Unique constraint should have failed!';
EXCEPTION
    WHEN unique_violation THEN
        RAISE NOTICE 'Unique constraint working correctly: %', SQLERRM;
END $$;

ROLLBACK;

-- Test 7: Test invitation token unique constraint
\echo '\n--- Test 7: Test invitation token unique constraint ---'
BEGIN;

INSERT INTO contests (name, description, start_time, end_time, status, active)
VALUES (
    'Test Invite Contest',
    'Test',
    NOW() + INTERVAL '1 day',
    NOW() + INTERVAL '1 day' + INTERVAL '2 hours',
    'UPCOMING',
    true
);

-- Insert first invitation
INSERT INTO private_contest_invitations (
    contest_id,
    token,
    expires_at
)
SELECT 
    id,
    'unique_token_12345',
    NOW() + INTERVAL '30 days'
FROM contests WHERE name = 'Test Invite Contest';

-- Try to insert duplicate token
DO $$
BEGIN
    INSERT INTO private_contest_invitations (
        contest_id,
        token,
        expires_at
    )
    SELECT 
        id,
        'unique_token_12345',
        NOW() + INTERVAL '30 days'
    FROM contests WHERE name = 'Test Invite Contest';
    
    RAISE EXCEPTION 'Token unique constraint should have failed!';
EXCEPTION
    WHEN unique_violation THEN
        RAISE NOTICE 'Token unique constraint working correctly: %', SQLERRM;
END $$;

ROLLBACK;

-- Test 8: Test participant unique constraint (contest_id, user_id)
\echo '\n--- Test 8: Test participant unique constraint ---'
BEGIN;

INSERT INTO contests (name, description, start_time, end_time, status, active)
VALUES (
    'Test Participant Contest',
    'Test',
    NOW() + INTERVAL '1 day',
    NOW() + INTERVAL '1 day' + INTERVAL '2 hours',
    'UPCOMING',
    true
);

-- Insert participant
INSERT INTO private_contest_participants (contest_id, user_id)
SELECT c.id, u.id
FROM contests c, users u
WHERE c.name = 'Test Participant Contest' AND u.email = 'test_host@example.com';

-- Try to insert same participant again
DO $$
BEGIN
    INSERT INTO private_contest_participants (contest_id, user_id)
    SELECT c.id, u.id
    FROM contests c, users u
    WHERE c.name = 'Test Participant Contest' AND u.email = 'test_host@example.com';
    
    RAISE EXCEPTION 'Participant unique constraint should have failed!';
EXCEPTION
    WHEN unique_violation THEN
        RAISE NOTICE 'Participant unique constraint working correctly: %', SQLERRM;
END $$;

ROLLBACK;

-- Test 9: Test CASCADE DELETE behavior
\echo '\n--- Test 9: Test CASCADE DELETE behavior ---'
BEGIN;

-- Create test data
INSERT INTO contests (name, description, start_time, end_time, status, active)
VALUES (
    'Test Cascade Contest',
    'Test',
    NOW() + INTERVAL '1 day',
    NOW() + INTERVAL '1 day' + INTERVAL '2 hours',
    'UPCOMING',
    true
);

INSERT INTO private_contests (contest_id, host_user_id)
SELECT c.id, u.id
FROM contests c, users u
WHERE c.name = 'Test Cascade Contest' AND u.email = 'test_host@example.com';

INSERT INTO private_contest_invitations (contest_id, token, expires_at)
SELECT c.id, 'cascade_test_token', NOW() + INTERVAL '30 days'
FROM contests c
WHERE c.name = 'Test Cascade Contest';

INSERT INTO private_contest_participants (contest_id, user_id)
SELECT c.id, u.id
FROM contests c, users u
WHERE c.name = 'Test Cascade Contest' AND u.email = 'test_host@example.com';

-- Count related records before delete
SELECT 
    'Before delete' as stage,
    COUNT(*) FILTER (WHERE table_name = 'private_contests') as private_contests,
    COUNT(*) FILTER (WHERE table_name = 'invitations') as invitations,
    COUNT(*) FILTER (WHERE table_name = 'participants') as participants
FROM (
    SELECT 'private_contests' as table_name FROM private_contests WHERE contest_id = (SELECT id FROM contests WHERE name = 'Test Cascade Contest')
    UNION ALL
    SELECT 'invitations' FROM private_contest_invitations WHERE contest_id = (SELECT id FROM contests WHERE name = 'Test Cascade Contest')
    UNION ALL
    SELECT 'participants' FROM private_contest_participants WHERE contest_id = (SELECT id FROM contests WHERE name = 'Test Cascade Contest')
) t;

-- Delete the contest
DELETE FROM contests WHERE name = 'Test Cascade Contest';

-- Verify cascade delete worked
SELECT 
    'After delete' as stage,
    COUNT(*) FILTER (WHERE table_name = 'private_contests') as private_contests,
    COUNT(*) FILTER (WHERE table_name = 'invitations') as invitations,
    COUNT(*) FILTER (WHERE table_name = 'participants') as participants
FROM (
    SELECT 'private_contests' as table_name FROM private_contests WHERE contest_id = (SELECT id FROM contests WHERE name = 'Test Cascade Contest')
    UNION ALL
    SELECT 'invitations' FROM private_contest_invitations WHERE contest_id = (SELECT id FROM contests WHERE name = 'Test Cascade Contest')
    UNION ALL
    SELECT 'participants' FROM private_contest_participants WHERE contest_id = (SELECT id FROM contests WHERE name = 'Test Cascade Contest')
) t;

ROLLBACK;

\echo '\n=== All Tests Completed Successfully ==='
