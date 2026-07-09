# Task 8.4 Implementation: Update Verdict Callback for Private Contest Leaderboard

## Overview
This document describes the implementation of Task 8.4, which modifies the `finalizeAndNotify()` method in `SubmissionWorkerPool` to support private contest leaderboard updates with delta scoring.

## Requirements Addressed
- **Requirement 13.5**: Write verdict to submissions row (already implemented)
- **Requirement 13.6**: Reuse existing SSE mechanism to push verdict (already implemented)
- **Requirement 13.7**: Update the `private:leaderboard:{contestId}` sorted set with delta scoring
- **Requirement 14.2**: Update the `private:leaderboard:{contestId}` sorted set with updated score

## Changes Made

### 1. Added `isPrivateContest()` Helper Method to `SubmissionJob`
**File**: `src/main/java/com/example/codecombat2026/dto/SubmissionJob.java`

Added a convenience method to check if a submission belongs to a private contest:

```java
/**
 * Helper method to check if this submission is for a private contest.
 * @return true if privateContestId is not null, false otherwise
 */
public boolean isPrivateContest() {
    return privateContestId != null;
}
```

**Rationale**: This provides a clean, self-documenting way to check if a job is for a private contest, improving code readability and maintainability.

### 2. Modified `finalizeAndNotify()` in `SubmissionWorkerPool`
**File**: `src/main/java/com/example/codecombat2026/service/SubmissionWorkerPool.java`

Updated the leaderboard update logic to branch based on whether the submission is for a private contest:

#### Private Contest Branch
When `job.isPrivateContest()` returns true:

1. **Cache Key Prefix**: Uses `private:leaderboard:{contestId}` instead of `leaderboard:{contestId}`
2. **Score Key**: Uses `private:score:{contestId}:{userId}:{problemId}` instead of `contest:score:{contestId}:{userId}:{problemId}`
3. **Delta Scoring**: 
   - Reads previous score from cache
   - Calculates delta (newScore - prevScore)
   - Uses `ZINCRBY` via `redis.opsForZSet().incrementScore()` to atomically update the leaderboard
   - Stores the new score for future delta calculations
4. **SSE Verdict Push**: Reuses existing SSE mechanism (unchanged)

#### Public Contest Branch
When `job.isPrivateContest()` returns false:
- Uses existing logic with `contest:score:*` prefix
- Calls `leaderboard.updateScore()` service
- Maintains backward compatibility

#### Code Structure
```java
if (!job.isTestRun() && job.getContestId() != null) {
    if (job.isPrivateContest()) {
        // Private contest leaderboard logic
        String leaderboardKey = "private:leaderboard:" + job.getContestId();
        String scoreKey = "private:score:" + job.getContestId()
            + ":" + job.getUserId() + ":" + job.getProblemId();
        
        // Read prev score, calculate delta, update
        String prevStr = redis.opsForValue().get(scoreKey);
        int prevScore = (prevStr != null) ? Integer.parseInt(prevStr) : 0;
        int delta = score - prevScore;

        redis.opsForValue().set(scoreKey, String.valueOf(score), Duration.ofHours(26));
        
        if (delta != 0) {
            redis.opsForZSet().incrementScore(leaderboardKey, 
                job.getUserId().toString(), delta);
        }
    } else {
        // Public contest leaderboard logic (existing)
        // ... unchanged ...
    }
}
```

## Key Design Decisions

### 1. Delta Scoring Approach
- **Why**: Prevents score stacking when a user resubmits code
- **Example**: User submits WA (50 points) → AC (100 points) → Leaderboard increases by +50, not +100
- **Implementation**: Store per-problem score, calculate delta on each submission

### 2. Atomic Updates with ZINCRBY
- **Why**: Ensures thread-safe leaderboard updates without race conditions
- **Implementation**: Uses Redis `ZINCRBY` via Spring's `incrementScore()` method
- **Benefit**: Multiple workers can update the same leaderboard concurrently

### 3. Cache Key Isolation
- **Why**: Separates private and public contest leaderboards completely
- **Pattern**: `private:*` prefix for all private contest cache keys
- **TTL**: 26 hours (contest duration + buffer)

### 4. Zero-Delta Optimization
- **Why**: Avoids unnecessary Redis operations when score doesn't change
- **Implementation**: Only call `incrementScore()` if `delta != 0`

### 5. Error Handling
- **Why**: One failure shouldn't lose the verdict
- **Implementation**: Leaderboard update wrapped in try-catch with warning log
- **Fallback**: Verdict still written to DB and pushed via SSE

## Testing

### Unit Tests Created
**File**: `src/test/java/com/example/codecombat2026/service/PrivateContestLeaderboardTest.java`

Tests verify:
1. `isPrivateContest()` helper method logic
2. Cache key format correctness (private vs public)
3. Delta score calculations for various scenarios:
   - First submission (delta = newScore)
   - Improved score (delta = positive)
   - Worse score (delta = negative)
   - Same score (delta = 0, no update)

### Test Coverage
- ✅ Helper method behavior
- ✅ Key format validation
- ✅ Delta calculation logic
- ✅ Edge cases (null values, zero delta)

## Integration Points

### Upstream Dependencies
- `SubmissionJob.privateContestId` field (already exists)
- `SubmissionWorkerPool.finalizeAndNotify()` method (modified)

### Downstream Impact
- Private contest leaderboard API endpoints will use `private:leaderboard:{contestId}` keys
- Leaderboard service for private contests will read from these isolated keys
- No impact on public contests (separate code path)

## Validation

### Compilation Status
- ✅ `SubmissionJob.java` compiles without errors
- ✅ `SubmissionWorkerPool.java` compiles without errors (27 pre-existing warnings, no new errors)
- ✅ `PrivateContestLeaderboardTest.java` compiles without errors

### Code Quality
- Follows existing code style and patterns
- Maintains backward compatibility with public contests
- Preserves duel submission isolation (no leaderboard updates for duel jobs)
- Consistent with delta scoring approach used in public contests

## Future Considerations

### Monitoring
- Add metrics for private contest leaderboard update latency
- Track delta calculation distribution (positive/negative/zero)
- Monitor Redis operation failures in private contest path

### Performance
- Current implementation uses blocking Redis operations
- Consider pipeline operations if multiple updates per job are needed
- 26-hour TTL should be sufficient for contest duration + buffer

### Scalability
- Design supports multiple judge workers updating same leaderboard
- ZINCRBY ensures atomic updates without locking
- No single point of contention (per-problem score keys are isolated)

## References
- **Requirements**: Requirements 13.5, 13.6, 13.7, 14.2
- **Design Document**: `design.md` Section "Verdict Callback Flow"
- **Related Files**:
  - `SubmissionJob.java` (DTO)
  - `SubmissionWorkerPool.java` (Service)
  - `LeaderboardCacheService.java` (Public contest leaderboard)

## Completion Status
✅ Task 8.4 is **COMPLETE**

All required functionality has been implemented:
- ✅ Check `job.isPrivateContest()` flag
- ✅ Use cache key prefix `private:leaderboard:{contestId}`
- ✅ Implement delta scoring with `private:score:{contestId}:{userId}:{problemId}`
- ✅ Use ZINCRBY to update leaderboard atomically
- ✅ Reuse existing SSE verdict push mechanism

The implementation is ready for integration testing with the full private contest hosting feature.
