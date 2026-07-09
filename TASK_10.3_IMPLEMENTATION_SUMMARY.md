# Task 10.3: Private Contest Reminder Scheduler Implementation Summary

## Overview
Implemented a Spring-based scheduler that sends automated email reminders to participants of upcoming private contests.

## Requirements Addressed
- **Requirement 31.1**: Send 24-hour reminder before contest start
- **Requirement 31.2**: Send 1-hour reminder before contest start
- **Requirement 31.3**: Notifications when contest starts (handled by ContestStatusScheduler)
- **Requirement 31.4**: Notifications when contest ends (handled by ContestStatusScheduler)
- **Requirement 31.5**: User opt-out preferences (TODO - placeholder in code)

## Implementation Details

### 1. PrivateContestReminderScheduler.java
**Location**: `src/main/java/com/example/codecombat2026/scheduler/PrivateContestReminderScheduler.java`

**Key Features**:
- **@Scheduled Annotation**: Runs hourly using configurable cron expression
- **Configurable Intervals**: Cron and enabled flag can be overridden via properties
- **Dual Reminder Windows**:
  - 24-hour reminder: 23-25 hour window (to handle hourly checks)
  - 1-hour reminder: 30-90 minute window
- **Redis-Based Duplicate Prevention**: Uses Redis keys to track sent reminders
  - Key format: `private:contest:reminder:24h:{contestId}` or `private:contest:reminder:1h:{contestId}`
  - TTL: Expires after contest start + 1 day for automatic cleanup
- **Contest Filtering**:
  - Only processes UPCOMING contests
  - Skips public contests (no PrivateContest record)
  - Skips cancelled contests
  - Handles contests with no start time
- **Participant Loading**:
  - Fetches all participants for each contest
  - Handles lazy-loaded User proxies by re-fetching from repository
  - Filters out participants without valid email addresses
- **Error Handling**: 
  - Non-fatal errors logged but don't block scheduler execution
  - Repository errors caught and logged

**Dependencies**:
- ContestRepository - fetch UPCOMING contests
- PrivateContestRepository - verify contest is private
- PrivateContestParticipantRepository - load participants
- UserRepository - fetch user details
- PrivateContestEmailService - send reminder emails
- StringRedisTemplate - duplicate prevention

### 2. Configuration Properties
**Location**: `src/main/resources/application.properties`

Added the following configurable properties:

```properties
# Participant reminder scheduler - runs every hour at minute 0
# Cron format: second minute hour day month weekday
# Override via PRIVATE_CONTEST_REMINDER_CRON environment variable
private.contest.reminder.cron=${PRIVATE_CONTEST_REMINDER_CRON:0 0 * * * *}

# Enable/disable reminder emails (default: true)
private.contest.reminder.enabled=${PRIVATE_CONTEST_REMINDER_ENABLED:true}
```

### 3. Email Service Integration
**Email Method Used**: `PrivateContestEmailService.sendContestReminderEmail()`

This method (implemented in Task 15.1) sends reminder emails to a list of participants with:
- Contest ID
- Hours until start (24 or 1)
- Link to contest page
- Contest preparation checklist

### 4. Comprehensive Test Suite
**Location**: `src/test/java/com/example/codecombat2026/scheduler/PrivateContestReminderSchedulerTest.java`

**Test Coverage** (11 tests):
1. ✅ Should send 24-hour reminder when contest starts in 24 hours
2. ✅ Should send 1-hour reminder when contest starts in 1 hour
3. ✅ Should not send reminder if already sent (Redis key exists)
4. ✅ Should skip cancelled contests
5. ✅ Should skip public contests (no PrivateContest record)
6. ✅ Should handle contest with no participants gracefully
7. ✅ Should skip participants without valid email
8. ✅ Should handle repository errors gracefully
9. ✅ Should not run when reminders are disabled
10. ✅ Should skip contests outside reminder windows
11. ✅ Should handle lazy-loaded user proxies

**Mocking Strategy**:
- All repositories and services mocked using Mockito
- Redis template operations mocked
- Time manipulation using test data setup

## Architecture Integration

### Scheduler Execution Flow
```
Hourly Cron Job (every hour)
  ↓
Check if enabled
  ↓
Fetch UPCOMING contests
  ↓
For each contest:
  ├─ Verify it's a private contest
  ├─ Check if cancelled
  ├─ Calculate hours until start
  ├─ Check if within reminder window (24h or 1h)
  ├─ Check Redis for duplicate prevention
  ├─ Load participants
  ├─ Send emails via PrivateContestEmailService
  └─ Mark reminder as sent in Redis
```

### Redis Key Strategy
- **Key Pattern**: `private:contest:reminder:{hours}h:{contestId}`
- **Value**: "sent" (simple flag)
- **TTL**: Contest start time + 1 day
- **Purpose**: Prevents duplicate emails if scheduler runs multiple times within the window

### Integration with Existing Components

1. **ContestStatusScheduler** (Task 10.1):
   - Handles contest start/end notifications to HOST
   - Handles contest lifecycle (UPCOMING → LIVE → ENDED)
   - PrivateContestReminderScheduler sends participant reminders

2. **PrivateContestEmailService** (Task 15.1):
   - Provides `sendContestReminderEmail()` method
   - Async email sending via @Async annotation
   - Uses existing MailConfig and noreply sender

3. **InviteTokenCleanupScheduler** (Task 10.2):
   - Separate scheduler for token cleanup
   - Runs daily at 2 AM UTC
   - PrivateContestReminderScheduler runs hourly

## Configuration & Deployment

### Environment Variables
- **PRIVATE_CONTEST_REMINDER_CRON**: Override cron expression (default: `0 0 * * * *`)
- **PRIVATE_CONTEST_REMINDER_ENABLED**: Enable/disable reminders (default: `true`)

### Cron Expression Format
Default: `0 0 * * * *` (every hour at minute 0)
- Format: `second minute hour day month weekday`
- Example: `0 30 * * * *` (every hour at minute 30)

### Production Considerations

1. **Scalability**:
   - Hourly execution is sufficient for reminder windows
   - Redis prevents duplicates in multi-instance deployments
   - Non-blocking email sending via @Async

2. **Error Handling**:
   - Repository errors logged but don't stop scheduler
   - Email send failures logged but don't block processing
   - Scheduler continues running even if one contest fails

3. **Performance**:
   - Only queries UPCOMING contests
   - Redis lookup before expensive participant queries
   - Minimal database queries per execution

4. **Monitoring**:
   - Log entries for each reminder sent
   - Log entries for skipped contests (cancelled, no participants)
   - Error logs for failures

## Testing & Validation

### Unit Tests
- **11 comprehensive tests** covering all scenarios
- **Mockito** for dependency injection
- **ArgumentCaptor** for verifying method calls
- **ReflectionTestUtils** for configuration injection

### Integration Testing
The scheduler is automatically registered as a Spring `@Component` and will be instantiated when the application starts with `@EnableScheduling`.

### Manual Testing Steps
1. Create a private contest starting in 24 hours
2. Add participants to the contest
3. Wait for hourly scheduler to run (or manually trigger via application restart)
4. Verify Redis key is set: `private:contest:reminder:24h:{contestId}`
5. Check participant email inboxes for reminder
6. Verify logs show reminder sent: `Sent 24 hour reminder for contestId=...`
7. Repeat for 1-hour reminder window

### Debugging
Enable debug logging in application.properties:
```properties
logging.level.com.example.codecombat2026.scheduler=DEBUG
```

## Future Enhancements (TODO)

1. **User Preferences** (Requirement 31.5):
   - Add `email_preferences` table with opt-out flags
   - Check user preferences before sending reminders
   - Always send start/end notifications (non-optional)

2. **Additional Reminder Windows**:
   - 7-day reminder for long-running contests
   - 10-minute reminder for urgent preparation

3. **SMS Notifications**:
   - Integrate with SMS provider (Twilio, SNS)
   - Send SMS reminders for critical events

4. **Custom Reminder Times**:
   - Allow Contest_Host to configure reminder intervals
   - Per-contest reminder preferences

5. **Metrics**:
   - Prometheus counter: `private_contest_reminders_sent_total`
   - Histogram: `private_contest_reminder_latency_seconds`
   - Gauge: `private_contest_reminders_pending`

## References

- **Design Document**: `.kiro/specs/private-contest-hosting/design.md` (Section 10: Scheduler Integration)
- **Requirements**: `.kiro/specs/private-contest-hosting/requirements.md` (Requirement 31)
- **Task List**: `.kiro/specs/private-contest-hosting/tasks.md` (Task 10.3)
- **Email Service**: `PrivateContestEmailService.java` (Task 15.1)
- **Contest Status Scheduler**: `ContestStatusScheduler.java` (Task 10.1)

## Completion Checklist
- [x] Create PrivateContestReminderScheduler with @Scheduled annotation
- [x] Implement 24-hour reminder logic
- [x] Implement 1-hour reminder logic
- [x] Use Redis for duplicate prevention
- [x] Reference PrivateContestEmailService for sending emails
- [x] Add configurable cron interval in application.properties
- [x] Handle cancelled contests
- [x] Handle contests with no participants
- [x] Handle lazy-loaded user proxies
- [x] Write comprehensive unit tests (11 tests)
- [x] Document implementation

## Status
✅ **COMPLETED** - Scheduler implemented, tested, and documented.

The PrivateContestReminderScheduler is fully functional and ready for integration testing.
