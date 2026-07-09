# V15 Migration Summary: Private Contest Hosting Tables

## Migration Details

**File**: `V15__create_private_contest_tables.sql`  
**Applied**: 2026-07-07 08:19:21  
**Status**: ✅ Successfully Applied

## Tables Created

### 1. contest_hosting_requests
Tracks user requests to become Contest_Hosts, requiring Admin approval.

**Columns**:
- `id` (BIGSERIAL PRIMARY KEY)
- `user_id` (BIGINT NOT NULL) - References users(id) with CASCADE delete
- `reason` (TEXT) - Why the user wants to host contests
- `intended_use_case` (VARCHAR(50) NOT NULL) - EDUCATION, RECRUITMENT, COMMUNITY, INTERNAL_TRAINING, OTHER
- `status` (VARCHAR(20) NOT NULL, DEFAULT 'PENDING') - PENDING, APPROVED, REJECTED, REVOKED
- `submitted_at` (TIMESTAMP NOT NULL, DEFAULT CURRENT_TIMESTAMP)
- `reviewed_by` (BIGINT) - References users(id) with SET NULL
- `reviewed_at` (TIMESTAMP)
- `admin_notes` (TEXT)
- `created_at` (TIMESTAMP NOT NULL, DEFAULT CURRENT_TIMESTAMP)
- `updated_at` (TIMESTAMP NOT NULL, DEFAULT CURRENT_TIMESTAMP)

**Indexes**:
- `idx_hosting_requests_status` on status
- `idx_hosting_requests_user` on user_id
- `idx_hosting_requests_submitted` on submitted_at

**Constraints**:
- CHECK constraint on `intended_use_case` (5 valid values)
- CHECK constraint on `status` (4 valid values)
- Foreign key to users(id) with CASCADE delete
- Foreign key to users(id) for reviewed_by with SET NULL

---

### 2. private_contests
Extension table linking a contests row to its Contest_Host and hosting metadata.

**Columns**:
- `id` (BIGSERIAL PRIMARY KEY)
- `contest_id` (BIGINT NOT NULL UNIQUE) - 1:1 relationship with contests(id)
- `host_user_id` (BIGINT NOT NULL) - References users(id) with CASCADE delete
- `enable_proctoring` (BOOLEAN NOT NULL, DEFAULT FALSE)
- `cancelled` (BOOLEAN NOT NULL, DEFAULT FALSE)
- `cancelled_at` (TIMESTAMP)
- `cancellation_reason` (TEXT)
- `created_at` (TIMESTAMP NOT NULL, DEFAULT CURRENT_TIMESTAMP)

**Indexes**:
- `idx_private_contests_host` on host_user_id
- `idx_private_contests_contest` on contest_id
- `idx_private_contests_created` on created_at

**Constraints**:
- UNIQUE constraint on contest_id (enforces 1:1 relationship)
- Foreign key to contests(id) with CASCADE delete
- Foreign key to users(id) with CASCADE delete

---

### 3. private_contest_invitations
Stores unique, time-limited invite tokens for each private contest.

**Columns**:
- `id` (BIGSERIAL PRIMARY KEY)
- `contest_id` (BIGINT NOT NULL) - References contests(id) with CASCADE delete
- `token` (VARCHAR(64) NOT NULL UNIQUE) - Cryptographically random token
- `created_at` (TIMESTAMP NOT NULL, DEFAULT CURRENT_TIMESTAMP)
- `expires_at` (TIMESTAMP NOT NULL)
- `invalidated` (BOOLEAN NOT NULL, DEFAULT FALSE)

**Indexes**:
- `idx_invitations_token` on token
- `idx_invitations_contest` on contest_id
- `idx_invitations_expires` on expires_at
- `idx_invitations_invalidated` on invalidated

**Constraints**:
- UNIQUE constraint on token
- Foreign key to contests(id) with CASCADE delete

---

### 4. private_contest_participants
Tracks which users have accepted invitations and joined a private contest.

**Columns**:
- `id` (BIGSERIAL PRIMARY KEY)
- `contest_id` (BIGINT NOT NULL) - References contests(id) with CASCADE delete
- `user_id` (BIGINT NOT NULL) - References users(id) with CASCADE delete
- `joined_at` (TIMESTAMP NOT NULL, DEFAULT CURRENT_TIMESTAMP)

**Indexes**:
- `idx_participants_contest` on contest_id
- `idx_participants_user` on user_id
- `idx_participants_joined` on joined_at

**Constraints**:
- UNIQUE constraint `uq_participant_per_contest` on (contest_id, user_id)
- Foreign key to contests(id) with CASCADE delete
- Foreign key to users(id) with CASCADE delete

---

## Verification Results

### ✅ All Tables Created Successfully
```sql
SELECT COUNT(*) FROM information_schema.tables 
WHERE table_name IN (
  'contest_hosting_requests',
  'private_contests',
  'private_contest_invitations',
  'private_contest_participants'
);
-- Result: 4
```

### ✅ All Indexes Created Successfully
```sql
SELECT COUNT(*) FROM pg_indexes 
WHERE tablename IN (
  'contest_hosting_requests',
  'private_contests',
  'private_contest_invitations',
  'private_contest_participants'
);
-- Result: 20 indexes (including primary keys and unique constraints)
```

### ✅ Foreign Key Constraints Working
- Verified CASCADE delete behavior
- Verified SET NULL behavior for reviewed_by
- All foreign key relationships correctly established

### ✅ Check Constraints Working
- `intended_use_case` CHECK constraint validated
- `status` CHECK constraint validated
- Invalid values correctly rejected

### ✅ Unique Constraints Working
- Token uniqueness verified
- Contest_id uniqueness verified (1:1 relationship)
- Participant (contest_id, user_id) uniqueness verified

### ✅ Rollback Tested
- Created rollback script: `U15__rollback_private_contest_tables.sql`
- Successfully tested rollback (drops all 4 tables)
- Successfully re-applied migration

---

## Migration Integration

### Flyway Schema History
```sql
SELECT version, description, installed_on 
FROM flyway_schema_history 
WHERE version = '15';
```

**Result**:
- Version: 15
- Description: create private contest tables
- Installed: 2026-07-07 08:19:21

### Dependencies
The migration depends on existing tables:
- `users` table (for user_id, host_user_id, reviewed_by foreign keys)
- `contests` table (for contest_id foreign key)

### Impact
- No changes to existing tables
- No data migration required
- Backward compatible (existing features unaffected)

---

## Performance Considerations

### Index Strategy
All indexes are optimized for expected query patterns:

1. **Hosting Requests**:
   - Status filtering for admin dashboard (`idx_hosting_requests_status`)
   - User lookup for checking approval status (`idx_hosting_requests_user`)
   - Time-based queries (`idx_hosting_requests_submitted`)

2. **Private Contests**:
   - Host-based queries for "my contests" list (`idx_private_contests_host`)
   - Contest detail lookups (`idx_private_contests_contest`)
   - Time-range queries for monthly quota (`idx_private_contests_created`)

3. **Invitations**:
   - Token validation (O(1) via unique index `idx_invitations_token`)
   - Expiry cleanup job (`idx_invitations_expires`)
   - Invalidation filtering (`idx_invitations_invalidated`)

4. **Participants**:
   - Participant list per contest (`idx_participants_contest`)
   - User's joined contests (`idx_participants_user`)
   - Join time analytics (`idx_participants_joined`)

### Query Performance Expectations
- Token lookup: O(1) via unique B-tree index
- Host contest list: O(log n) with index scan
- Participant count: O(log n) with index scan
- Monthly quota check: Index range scan on created_at

---

## Business Rules Enforced

### Database Level
1. ✅ Unique token per invitation (unique constraint)
2. ✅ One private_contests row per contest (unique constraint on contest_id)
3. ✅ One participation per user per contest (unique constraint)
4. ✅ Valid intended_use_case values (check constraint)
5. ✅ Valid status values (check constraint)
6. ✅ Cascade delete on contest removal
7. ✅ Cascade delete on user removal

### Application Level (Not in Migration)
- Monthly quota: 2 contests per host (enforced in service layer)
- Participant limit: 100 per contest (enforced in service layer)
- Duration limit: 5 hours max (enforced in service layer)
- Overlap check: No time conflicts (enforced in service layer)

---

## Next Steps

### Implementation Tasks
1. ✅ **Task 1 Complete**: Database schema and Flyway migration
2. **Task 2**: Create JPA entity classes
3. **Task 3**: Create repository interfaces
4. **Task 4**: Implement service layer
5. **Task 5**: Create REST API endpoints
6. **Task 6**: Add validation and security
7. **Task 7**: Implement frontend UI

### Related Requirements
This migration satisfies the database schema requirements for:
- Requirement 20.1: Contest hosting request submission
- Requirement 20.2: Admin approval workflow
- Requirement 20.3: Private contest creation
- Requirement 20.4: Invitation management
- Requirement 20.5: Participant tracking
- Requirement 20.6: Access control foundation

---

## Files Created

1. **Migration**: `src/main/resources/db/migration/V15__create_private_contest_tables.sql`
2. **Rollback**: `src/main/resources/db/migration/U15__rollback_private_contest_tables.sql`
3. **Test Suite**: `test_migration_v15.sql`
4. **Documentation**: `V15_MIGRATION_SUMMARY.md` (this file)

---

## Testing Checklist

- [x] Migration applies successfully
- [x] All tables created
- [x] All indexes created
- [x] Primary keys work
- [x] Foreign keys work
- [x] Unique constraints work
- [x] Check constraints work
- [x] Default values work
- [x] CASCADE delete works
- [x] Rollback works
- [x] Re-apply works
- [x] Flyway history updated

---

## Notes

1. **Token Generation**: The application layer should use `SecureRandom` with 32 bytes, base64url-encoded to generate tokens.

2. **Expiry Cleanup**: A scheduled job should periodically clean up expired tokens:
   ```sql
   DELETE FROM private_contest_invitations 
   WHERE expires_at < CURRENT_TIMESTAMP;
   ```

3. **Audit Trail**: The `contest_hosting_requests` table preserves the audit trail even if the reviewing admin is deleted (SET NULL on reviewed_by).

4. **1:1 Relationship**: The unique constraint on `private_contests.contest_id` enforces the 1:1 relationship with the contests table, ensuring a contest cannot be both public and private.

5. **Naming Convention**: All table and column names use snake_case as per PostgreSQL conventions, matching the existing schema style.
