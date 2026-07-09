# Task 16.1 Implementation Summary: RateLimitService using Valkey

## Overview
Implemented `PrivateContestRateLimitService` to enforce rate limits for private contest operations using Valkey (Redis-compatible) with INCR + TTL sliding window pattern.

## Implementation Details

### 1. Service: `PrivateContestRateLimitService.java`
**Location:** `src/main/java/com/example/codecombat2026/service/PrivateContestRateLimitService.java`

#### Methods Implemented (per Requirement 24):

1. **`checkContestCreationLimit(userId)`**
   - Limit: 5 requests per hour (sliding window)
   - Key pattern: `ratelimit:contest:create:user:{userId}`
   - Requirement: 24.1

2. **`checkAIProblemGenLimit(userId)`**
   - Limit: 5 requests per day (sliding window)
   - Key pattern: `ratelimit:ai:problem:gen:user:{userId}`
   - Requirement: 24.2

3. **`checkInviteRegenLimit(contestId)`**
   - Limit: 10 requests per hour (sliding window)
   - Key pattern: `ratelimit:invite:regen:contest:{contestId}`
   - Requirement: 24.3

4. **`checkInviteAcceptLimit(contestId)`**
   - Limit: 100 requests per hour (sliding window)
   - Key pattern: `ratelimit:invite:accept:contest:{contestId}`
   - Requirement: 24.4

#### Technical Approach:
- **Valkey INCR with TTL**: Uses atomic increment operation with TTL for sliding window
- **Exception Handling**: Throws `TooManyRequestsException` with retry-after information (Requirement 24.5)
- **Fail-Open**: If Valkey is unavailable, logs error but allows request (prevents complete service outage)
- **Helper Methods**: Provides `get{Operation}RetryAfter()` methods for each rate limit type

### 2. Exception Enhancement: `TooManyRequestsException.java`
**Changes:**
- Added optional `retryAfterSeconds` field
- New constructor: `TooManyRequestsException(String message, Long retryAfterSeconds)`
- Getter method for retry-after value

### 3. Global Exception Handler Update: `GlobalExceptionHandler.java`
**Changes:**
- Updated `handleTooManyRequestsException` to include `Retry-After` HTTP header
- Reads `retryAfterSeconds` from exception and adds to response headers

### 4. Unit Tests: `PrivateContestRateLimitServiceTest.java`
**Location:** `src/test/java/com/example/codecombat2026/service/PrivateContestRateLimitServiceTest.java`

**Test Coverage:**
- Contest creation rate limit (5 per hour)
- AI problem generation rate limit (5 per day)
- Invite regeneration rate limit (10 per hour)
- Invite acceptance rate limit (100 per hour)
- Retry-after helper methods
- Error handling (Valkey failures)
- Edge cases (null counts, negative TTL)

**Total Tests:** 26 unit tests

### 5. Integration Tests: `PrivateContestRateLimitServiceIntegrationTest.java`
**Location:** `src/test/java/com/example/codecombat2026/service/PrivateContestRateLimitServiceIntegrationTest.java`

**Test Coverage:**
- End-to-end rate limiting with real Valkey
- Verifies actual INCR operations and TTL behavior
- Tests all 4 rate limit types

### 6. Bug Fixes
Fixed pre-existing compilation errors in other files:
- `ContestStatusScheduler.java`: Fixed lossy conversion from long to int
- Properly handled `countByContestId()` return type conversions

## Valkey Key Design

| Operation | Key Pattern | TTL | Limit |
|-----------|-------------|-----|-------|
| Contest Creation | `ratelimit:contest:create:user:{userId}` | 3600s (1 hour) | 5 |
| AI Problem Gen | `ratelimit:ai:problem:gen:user:{userId}` | 86400s (1 day) | 5 |
| Invite Regen | `ratelimit:invite:regen:contest:{contestId}` | 3600s (1 hour) | 10 |
| Invite Accept | `ratelimit:invite:accept:contest:{contestId}` | 3600s (1 hour) | 100 |

## Algorithm: Sliding Window Rate Limiting

```java
1. INCR key
2. IF count == 1:
     EXPIRE key windowSeconds
3. IF count > limit:
     GET TTL key
     THROW TooManyRequestsException(message, ttl)
4. ELSE:
     Allow request
```

## Usage Examples

### In Service Layer:
```java
@Autowired
private PrivateContestRateLimitService rateLimitService;

// Before creating a contest
rateLimitService.checkContestCreationLimit(userId);

// Before generating AI problem
rateLimitService.checkAIProblemGenLimit(userId);

// Before regenerating invite link
rateLimitService.checkInviteRegenLimit(contestId);

// Before accepting invite
rateLimitService.checkInviteAcceptLimit(contestId);
```

### HTTP Response on Rate Limit:
```http
HTTP/1.1 429 Too Many Requests
Retry-After: 1800
Content-Type: application/json

{
  "message": "You have reached your hourly limit of 5 contest creation requests. Please try again later."
}
```

## Requirements Satisfied

✅ **Requirement 24.1**: Contest creation - 5 per hour per user  
✅ **Requirement 24.2**: AI problem generation - 5 per day per user  
✅ **Requirement 24.3**: Invite regeneration - 10 per hour per contest  
✅ **Requirement 24.4**: Invite acceptance - 100 per hour per contest  
✅ **Requirement 24.5**: Return HTTP 429 with Retry-After header  

## Files Created/Modified

### Created:
1. `src/main/java/com/example/codecombat2026/service/PrivateContestRateLimitService.java` (244 lines)
2. `src/test/java/com/example/codecombat2026/service/PrivateContestRateLimitServiceTest.java` (344 lines)
3. `src/test/java/com/example/codecombat2026/service/PrivateContestRateLimitServiceIntegrationTest.java` (117 lines)

### Modified:
1. `src/main/java/com/example/codecombat2026/exception/TooManyRequestsException.java`
   - Added retry-after support
2. `src/main/java/com/example/codecombat2026/exception/GlobalExceptionHandler.java`
   - Added Retry-After header to 429 responses
3. `src/main/java/com/example/codecombat2026/scheduler/ContestStatusScheduler.java`
   - Fixed compilation errors (lossy conversion)

## Next Steps for Integration

To use this service in the private contest feature:

1. **Private Contest Creation** (`PrivateContestService.java`):
   ```java
   rateLimitService.checkContestCreationLimit(userId);
   // ... create contest logic
   ```

2. **AI Problem Generation** (`AIProblemGeneratorService.java`):
   ```java
   rateLimitService.checkAIProblemGenLimit(userId);
   // ... AI generation logic
   ```

3. **Invite Link Regeneration** (`InviteTokenService.java`):
   ```java
   rateLimitService.checkInviteRegenLimit(contestId);
   // ... regenerate token logic
   ```

4. **Invite Acceptance** (`PrivateInviteService.java`):
   ```java
   rateLimitService.checkInviteAcceptLimit(contestId);
   // ... accept invite logic
   ```

## Testing

### Unit Tests:
```bash
./mvnw test -Dtest=PrivateContestRateLimitServiceTest
```

### Integration Tests:
```bash
./mvnw test -Dtest=PrivateContestRateLimitServiceIntegrationTest
```

### All Tests:
```bash
./mvnw test
```

## Notes

- **Fail-Open Strategy**: If Valkey is unavailable, the service logs an error but allows the request to proceed. This prevents complete service outage but weakens rate limiting temporarily.
- **Atomic Operations**: Uses Valkey's atomic INCR to ensure thread-safe counting across multiple application instances.
- **TTL Management**: TTL is set only on the first increment (count == 1) to implement true sliding window behavior.
- **Human-Readable Messages**: Error messages include formatted duration (e.g., "45 minutes", "2 hours") for better UX.
