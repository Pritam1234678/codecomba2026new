# Implementation Plan: Private Contest Hosting

## Overview

This implementation plan breaks down the Private Contest Hosting feature into discrete, manageable coding tasks. The feature integrates seamlessly with the existing CodeCoder Spring Boot architecture, reusing the contest engine, judge system, proctoring infrastructure, leaderboard, and problem management systems.

**Technology Stack**: Java 21, Spring Boot 3, PostgreSQL 18, Valkey 7, React 19  
**Integration**: Extends existing `Contest`, `User`, `Problem`, `Submission` entities  
**New Infrastructure**: 4 new database tables, cache key prefixes, separate judge queue

## Tasks

- [x] 1. Database schema and Flyway migration
  - Create Flyway migration `V8__create_private_contest_tables.sql`
  - Define `contest_hosting_requests` table with status enum and admin review fields
  - Define `private_contests` table with 1:1 relationship to `contests` table
  - Define `private_contest_invitations` table with token, expiry, and invalidation tracking
  - Define `private_contest_participants` table with unique constraint on (contest_id, user_id)
  - Add indexes for performance: status lookups, host queries, token lookups
  - Add foreign key constraints with CASCADE deletes where appropriate
  - Test migration on development database and verify rollback
  - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5, 20.6_

- [x] 2. Core entity classes and repositories
  - [x] 2.1 Create ContestHostingRequest entity
    - Map to `contest_hosting_requests` table with Lombok @Data
    - Define enum for IntendedUseCase (EDUCATION, RECRUITMENT, COMMUNITY, INTERNAL_TRAINING, OTHER)
    - Define enum for HostingRequestStatus (PENDING, APPROVED, REJECTED, REVOKED)
    - Add relationships: @ManyToOne for user, @ManyToOne for reviewedBy admin
    - _Requirements: 1.1, 1.2, 2.1, 2.2_
  
  - [x] 2.2 Create PrivateContest entity
    - Map to `private_contests` table with Lombok @Data
    - Add @OneToOne relationship to Contest entity via contestId
    - Add @ManyToOne relationship to User entity via hostUserId
    - Add fields: enableProctoring, cancelled, cancelledAt, cancellationReason
    - _Requirements: 4.6, 4.7, 18.2_
  
  - [x] 2.3 Create PrivateContestInvitation entity
    - Map to `private_contest_invitations` table with Lombok @Data
    - Add token field (VARCHAR 64), createdAt, expiresAt, invalidated
    - Add @ManyToOne relationship to Contest entity via contestId
    - _Requirements: 4.8, 5.1, 5.2, 5.3_

  
  - [x] 2.4 Create PrivateContestParticipant entity
    - Map to `private_contest_participants` table with Lombok @Data
    - Add @ManyToOne relationships to Contest and User entities
    - Add unique constraint on (contestId, userId)
    - Add joinedAt timestamp
    - _Requirements: 6.6, 6.7_
  
  - [x] 2.5 Create Spring Data JPA repositories
    - ContestHostingRequestRepository with queries: findByUserId, findByStatus, countByUserIdAndStatus
    - PrivateContestRepository with queries: findByContestId, findByHostUserId, countByHostUserIdAndCreatedAtBetween
    - PrivateContestInvitationRepository with queries: findByToken, findByContestId, deleteByExpiresAtBefore
    - PrivateContestParticipantRepository with queries: findByContestId, findByUserId, countByContestId, existsByContestIdAndUserId
    - _Requirements: 3.1, 4.4, 6.5, 7.1_

- [x] 3. Business validation services
  - [x] 3.1 Create PrivateContestAccessValidator service
    - Implement isHost(contestId, userId) method
    - Implement isParticipant(contestId, userId) method
    - Implement canAccess(contestId, userId) method combining both checks
    - Add @Service annotation and inject required repositories
    - _Requirements: 11.4, 23.2, 23.3_
  
  - [x] 3.2 Create PrivateContestBusinessRules service
    - Implement validateMonthlyQuota(hostUserId) checking 2 contests per calendar month
    - Implement validateDuration(startTime, endTime) checking max 5 hours (300 minutes)
    - Implement validateNoOverlap(hostUserId, startTime, endTime) for time conflict detection
    - Implement validateParticipantLimit(contestId) checking max 100 participants
    - Throw appropriate exceptions: TooManyRequestsException, BadRequestException, ConflictException
    - _Requirements: 3.1, 3.2, 4.2, 4.3, 4.4, 4.5, 6.5_
  
  - [x] 3.3 Create InviteTokenService for token operations
    - Implement generateToken() using SecureRandom and Base64 URL encoding (32 bytes)
    - Implement validateToken(token) checking existence, expiry, invalidation
    - Implement isTokenValid(token) returning boolean for frontend preview
    - Add cache-aside pattern for token validation (optional optimization)
    - _Requirements: 4.8, 5.1, 6.3, 6.4, 26.3_

- [x] 4. Contest hosting request workflow
  - [x] 4.1 Create ContestHostingService
    - Implement submitHostingRequest(userId, reason, intendedUseCase)
    - Check for existing PENDING or APPROVED request (throw 409 if exists)
    - Create ContestHostingRequest entity with status PENDING
    - Send email notification to all admins with approval link
    - Return HostingRequestDTO
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  
  - [x] 4.2 Implement admin approval/rejection methods in ContestHostingService
    - Implement approveRequest(requestId, adminId, adminNotes)
    - Implement rejectRequest(requestId, adminId, adminNotes)
    - Implement revokeApproval(requestId, adminId, reason)
    - Validate request is in PENDING status before approve/reject
    - Send email to user on approval with link to "Create Private Contest" page
    - Send email to user on rejection with reason
    - Record action in audit log
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6, 19.4, 19.5_
  
  - [x] 4.3 Create ContestHostingController REST endpoints
    - POST /api/hosting/request - submit hosting request (ROLE_USER)
    - GET /api/hosting/request/status - get current user's hosting status
    - GET /api/admin/hosting/requests - list all requests with filters (ROLE_ADMIN)
    - POST /api/admin/hosting/requests/{id}/approve (ROLE_ADMIN)
    - POST /api/admin/hosting/requests/{id}/reject (ROLE_ADMIN)
    - POST /api/admin/hosting/requests/{id}/revoke (ROLE_ADMIN)
    - Add @PreAuthorize annotations for role-based access
    - _Requirements: 1.1, 2.1, 2.2, 2.3, 19.4_

- [x] 5. Private contest creation and management
  - [x] 5.1 Create PrivateContestService core methods
    - Implement createPrivateContest(dto, hostUserId)
    - Call businessRules.validateMonthlyQuota(hostUserId)
    - Call businessRules.validateDuration(startTime, endTime)
    - Call businessRules.validateNoOverlap(hostUserId, startTime, endTime)
    - Create Contest entity with status UPCOMING
    - Create PrivateContest entity with hostUserId and enableProctoring flag
    - Create ProctoredContest entity if enableProctoring is true
    - Generate invite token and create PrivateContestInvitation entity
    - Return PrivateContestDTO with invite link
    - Send email to host with contest details and invite link
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 17.1_
  
  - [x] 5.2 Implement contest cancellation in PrivateContestService
    - Implement cancelContest(contestId, hostUserId, reason)
    - Validate contest status is UPCOMING (throw 409 if LIVE or ENDED)
    - Validate user is the host
    - Update PrivateContest with cancelled=true, cancelledAt, cancellationReason
    - Invalidate all invite tokens for the contest
    - Send email to all participants with cancellation reason
    - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5_
  
  - [x] 5.3 Implement contest cloning in PrivateContestService
    - Implement cloneContest(contestId, hostUserId)
    - Validate user is the host of the source contest
    - Validate source contest status is ENDED
    - Create new Contest and PrivateContest with same name + " (Copy)"
    - Copy problem attachments via contest_problems junction table
    - Generate new invite token
    - Do NOT copy participants, submissions, or leaderboard
    - Return PrivateContestDTO with new contestId
    - _Requirements: 33.1, 33.2, 33.3, 33.4, 33.5, 33.6_

  
  - [x] 5.4 Create PrivateContestController REST endpoints
    - POST /api/contests/private - create private contest (Contest_Host only)
    - GET /api/contests/private/my-contests - list host's contests
    - GET /api/contests/private/joined - list contests user joined as participant
    - GET /api/contests/private/{id} - get contest details (host or participant only)
    - PUT /api/contests/private/{id}/cancel - cancel contest (host only)
    - POST /api/contests/private/{id}/clone - clone ended contest (host only)
    - Add access validation using PrivateContestAccessValidator
    - _Requirements: 4.1, 11.2, 11.3, 11.4, 18.1, 33.1_

- [x] 6. Invite link and participant management
  - [x] 6.1 Create PrivateInviteService for invitation operations
    - Implement regenerateInviteToken(contestId, hostUserId)
    - Mark old token as invalidated
    - Generate new token with fresh expiresAt (30 days default)
    - Implement updateTokenExpiry(contestId, hostUserId, expiresAt)
    - Validate expiresAt is between now and contest end time
    - _Requirements: 5.2, 5.3, 5.4_
  
  - [x] 6.2 Implement participant join flow in PrivateInviteService
    - Implement acceptInvite(userId, token)
    - Validate token using InviteTokenService.validateToken(token)
    - Check participant limit using businessRules.validateParticipantLimit(contestId)
    - Create PrivateContestParticipant entity
    - Handle unique constraint violation (already joined)
    - Send email to host on first participant join
    - _Requirements: 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 17.2_
  
  - [x] 6.3 Implement participant removal in PrivateInviteService
    - Implement removeParticipant(contestId, userId, hostUserId)
    - Validate user is the host
    - Validate contest status is not LIVE (throw 409 if started)
    - Delete PrivateContestParticipant row
    - _Requirements: 7.4, 7.5_
  
  - [x] 6.4 Create PrivateContestInviteController REST endpoints
    - GET /api/contests/private/join?token= - preview contest before joining (public)
    - POST /api/contests/private/join - accept invitation (authenticated user)
    - POST /api/contests/private/{id}/invite/regenerate - regenerate token (host only)
    - PUT /api/contests/private/{id}/invite/expiry - update token expiry (host only)
    - GET /api/contests/private/{id}/participants - list participants (host only)
    - DELETE /api/contests/private/{id}/participants/{userId} - remove participant (host only)
    - _Requirements: 5.2, 5.5, 5.6, 6.1, 6.2, 7.1, 7.3, 7.4_

- [ ] 7. Problem management for private contests
  - [x] 7.1 Add visibility enum to Problem entity
    - Extend Problem entity with visibility field (PUBLIC, PRIVATE_AVAILABLE, PRIVATE_OWNED, ADMIN_ONLY)
    - Add createdBy field (nullable Long) to track Contest_Host ownership
    - Update existing problem data with visibility=PUBLIC as default
    - _Requirements: 30.1, 30.4_

  
  - [x] 7.2 Create PrivateContestProblemService
    - Implement browseProblem s(difficulty, search, page, size) filtering by visibility
    - Return only problems with visibility PUBLIC or PRIVATE_AVAILABLE
    - Implement attachProblems(contestId, problemIds, hostUserId)
    - Validate host ownership and contest status is UPCOMING
    - Create ContestProblem rows with displayOrder
    - Invalidate contest cache key
    - Implement removeProblems(contestId, problemIds, hostUserId)
    - _Requirements: 8.1, 8.2, 8.4, 8.5, 8.6, 8.7, 30.2, 30.3_
  
  - [x] 7.3 Create AIProblemGeneratorService
    - Implement generateProblem(prompt, difficulty, topic, hostUserId)
    - Check rate limit using Valkey key ai:problem:gen:user:{userId} (5/day)
    - Call AI service (OpenAI/Claude API) with prompt
    - Parse response and create Problem entity with visibility=PRIVATE_OWNED, createdBy=hostUserId
    - Store test cases (sample and hidden)
    - Return ProblemDTO for preview
    - _Requirements: 9.1, 9.2, 9.4, 9.6, 9.7, 30.4_
  
  - [ ] 7.4 Implement problem editing in AIProblemGeneratorService
    - Implement editProblem(problemId, dto, userId)
    - Validate problem.createdBy == userId OR user is admin
    - Validate problem is not attached to a LIVE or ENDED contest
    - Update Problem entity fields
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 30.5_
  
  - [ ] 7.5 Create problem management REST endpoints
    - GET /api/contests/private/{id}/problems/available - browse problems (host only)
    - POST /api/contests/private/{id}/problems - attach problems (host only)
    - DELETE /api/contests/private/{id}/problems/{problemId} - remove problem (host only)
    - POST /api/contests/private/{id}/problems/generate - AI generate (host only)
    - PUT /api/problems/{id} - edit problem (owner or admin only)
    - Add rate limiting interceptor for AI generation endpoint
    - _Requirements: 8.1, 8.4, 9.1, 10.1_

- [ ] 8. Submission and judge queue integration
  - [x] 8.1 Extend SubmissionJob DTO with privateContest flag
    - Add boolean privateContest field to SubmissionJob DTO
    - Update SubmissionJob constructor/builder to include this field
    - This flag routes to correct cache key prefixes (private:leaderboard vs leaderboard)
    - _Requirements: 13.2, 21.2_
  
  - [ ] 8.2 Create PrivateContestSubmissionService
    - Implement submitCode(contestId, problemId, userId, code, language)
    - Validate user is a participant using accessValidator.isParticipant()
    - Validate contest status is LIVE
    - Create Submission entity with status PENDING
    - Create SubmissionJob with privateContest=true
    - Push to private:submission:queue in Valkey
    - Return submissionId
    - _Requirements: 11.5, 13.1, 13.2_

  
  - [x] 8.3 Extend UnifiedSubmissionWorkerPool for dual queue draining
    - Modify workerLoop() to alternate between submission:queue and private:submission:queue
    - Implement fair round-robin strategy (1 public, 1 private, repeat)
    - Log queue name for each job processed for observability
    - Update janitor to reclaim stuck jobs from both queues
    - _Requirements: 13.3, 22.1, 22.2_
  
  - [x] 8.4 Update verdict callback for private contest leaderboard
    - Modify finalizeAndNotify() to check job.isPrivateContest() flag
    - If true, use cache key prefix private:leaderboard:{contestId}
    - Implement delta scoring: read prev score from private:score:{contestId}:{userId}:{problemId}
    - Use ZINCRBY to update leaderboard atomically
    - Reuse existing SSE verdict push mechanism
    - _Requirements: 13.5, 13.6, 13.7, 14.2_
  
  - [x] 8.5 Create submission REST endpoints for private contests
    - POST /api/contests/private/{id}/submit - submit code (participant only)
    - GET /api/contests/private/{id}/submissions - list all submissions (host only)
    - Add query filters: userId, problemId, status
    - Reuse existing SSE endpoint for real-time verdicts
    - _Requirements: 11.5, 13.1, 13.6_

- [x] 9. Leaderboard service for private contests
  - [x] 9.1 Create PrivateContestLeaderboardService
    - Implement initializeLeaderboard(contestId) creating empty ZSET
    - Implement getLeaderboard(contestId) reading from private:leaderboard:{contestId}
    - Return sorted list with rank, userId, username, score, penalty
    - Implement persistLeaderboard(contestId) freezing final rankings to database
    - _Requirements: 12.4, 12.5, 14.1, 14.3_
  
  - [x] 9.2 Create leaderboard REST endpoint
    - GET /api/contests/private/{id}/leaderboard - get real-time leaderboard
    - Validate user is participant or host
    - Add cache control headers for 10-second refresh rate
    - _Requirements: 14.3, 14.4_

- [x] 10. Contest lifecycle scheduler integration
  - [x] 10.1 Extend ContestStatusScheduler for private contests
    - Modify updateContestStatuses() to handle both public and private contests
    - On UPCOMING → LIVE transition, initialize private:leaderboard:{contestId}
    - On LIVE → ENDED transition, call leaderboardService.persistLeaderboard()
    - Send email notifications to host on status changes
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 17.3, 17.4_
  
  - [x] 10.2 Create invite token cleanup scheduler
    - Implement cleanupExpiredInvitations() scheduled at 02:00 UTC daily
    - Delete rows from private_contest_invitations where expiresAt < now AND invalidated=true
    - Log count of deleted tokens for monitoring
    - _Requirements: 26.1, 26.2_

  
  - [x] 10.3 Create participant reminder scheduler
    - Implement sendContestReminders() scheduled hourly
    - Find contests starting in 24 hours, send reminder emails to participants
    - Find contests starting in 1 hour, send reminder emails to participants
    - Check user preferences for opt-out (except mandatory start/end notifications)
    - _Requirements: 31.1, 31.2, 31.3, 31.4, 31.5_

- [ ] 11. Analytics and reporting
  - [x] 11.1 Create PrivateContestAnalyticsService
    - Implement getAnalytics(contestId, hostUserId)
    - Validate user is the host
    - Calculate: total participants, active participants, total submissions
    - Calculate per-problem stats: submission count, acceptance rate, avg solve time
    - Calculate engagement timeline (submissions per 15-min interval)
    - Cache results for ENDED contests with 24-hour TTL
    - _Requirements: 16.1, 16.4_
  
  - [x] 11.2 Implement CSV export for analytics
    - Implement exportAnalyticsCSV(contestId, hostUserId)
    - Generate CSV with rows: problemId, title, submissions, accepted, acceptance_rate, avg_time
    - Return as downloadable file with Content-Disposition header
    - _Requirements: 16.3_
  
  - [x] 11.3 Create analytics REST endpoints
    - GET /api/contests/private/{id}/analytics - get analytics JSON (host only)
    - GET /api/contests/private/{id}/analytics/export - export CSV (host only)
    - _Requirements: 16.1, 16.3_
  
  - [ ] 11.4 Create real-time dashboard WebSocket endpoint
    - Implement /ws/contests/private/{id}/dashboard WebSocket endpoint
    - Stream updates every 5 seconds: participant count, submission count, top 10 leaderboard, recent submissions
    - Validate only host or admin can connect
    - _Requirements: 32.1, 32.2, 32.3, 32.4_

- [ ] 12. Proctoring integration
  - [x] 12.1 Update PrivateContestService.createPrivateContest() for proctoring
    - If enableProctoring is true, create ProctoredContest entity
    - Set consentVersion and other proctoring config fields
    - _Requirements: 15.1, 15.2_
  
  - [x] 12.2 Create proctoring data access endpoint for hosts
    - GET /api/contests/private/{id}/proctoring/sessions - list sessions (host or admin only)
    - Validate user is host or admin
    - Return proctoring sessions with risk scores and screenshots
    - _Requirements: 15.3, 15.4, 15.5_
  
  - [ ] 12.3 Update contest detail endpoint to indicate proctoring
    - Modify GET /api/contests/private/{id} response to include isProctored flag
    - Trigger existing proctoring consent flow on frontend
    - _Requirements: 15.2_

- [x] 13. Admin oversight and moderation
  - [x] 13.1 Create PrivateContestAdminService
    - Implement listAllPrivateContests(filters, page, size) - admin only
    - Implement getPrivateContestDetails(contestId) - admin only (bypasses ownership check)
    - Implement deletePrivateContest(contestId, adminId) with cascade deletes
    - _Requirements: 19.1, 19.2, 19.3_

  
  - [x] 13.2 Create PrivateContestAdminController REST endpoints
    - GET /api/admin/private-contests - list all private contests (ROLE_ADMIN)
    - GET /api/admin/private-contests/{id} - get full details (ROLE_ADMIN)
    - DELETE /api/admin/private-contests/{id} - delete contest (ROLE_ADMIN)
    - GET /api/admin/judge-stats - monitor queue stats (ROLE_ADMIN)
    - _Requirements: 19.1, 19.2, 19.3, 22.3_

- [ ] 14. Audit logging
  - [x] 14.1 Create AuditLog entity and repository
    - Define entity with fields: id, userId, action, resourceType, resourceId, timestamp, ipAddress, userAgent, detailsJson
    - Create AuditLogRepository extending JpaRepository
    - Add index on userId, action, timestamp
    - _Requirements: 29.1, 29.2_
  
  - [x] 14.2 Create AuditService for event logging
    - Implement logEvent(userId, action, resourceType, resourceId, details)
    - Extract IP address and user agent from HTTP request
    - Store detailsJson as JSON string
    - Call asynchronously to avoid blocking API responses
    - _Requirements: 29.1_
  
  - [x] 14.3 Integrate audit logging into all critical operations
    - Log: hosting request submitted, approved, rejected, revoked
    - Log: private contest created, cancelled, deleted
    - Log: participant joined, removed
    - Log: problem added, removed
    - Log: invite link regenerated
    - _Requirements: 29.1_
  
  - [ ] 14.4 Create admin audit log query endpoint
    - GET /api/admin/audit-logs - query with filters (ROLE_ADMIN)
    - Support filters: userId, action, resourceType, dateRange
    - Return paginated results
    - _Requirements: 29.3_

- [ ] 15. Email notification system
  - [ ] 15.1 Create PrivateContestEmailService
    - Implement sendHostingRequestSubmittedEmail(admins, requestId)
    - Implement sendHostingApprovedEmail(user, requestId)
    - Implement sendHostingRejectedEmail(user, reason)
    - Implement sendContestCreatedEmail(host, contestId, inviteLink)
    - Implement sendFirstParticipantJoinedEmail(host, contestId, participantUsername)
    - Implement sendContestStartedEmail(host, contestId, dashboardLink)
    - Implement sendContestEndedEmail(host, contestId, analyticsLink)
    - Implement sendContestCancelledEmail(participants, contestId, reason)
    - Implement sendContestReminderEmail(participants, contestId, hoursUntilStart)
    - Implement sendParticipantContestStartedEmail(participants, contestId)
    - Implement sendParticipantContestEndedEmail(participant, contestId, rank, score)
    - Use existing MailConfig and noreplyMailSender
    - Use @Async annotation for non-blocking sends
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 18.3, 27.1, 27.2, 27.3, 27.4, 31.1, 31.2, 31.3, 31.4_

  
  - [x] 15.2 Create HTML email templates
    - Create Thymeleaf templates for all email types
    - Include CodeCoder logo, branding, and footer
    - Add unsubscribe link functionality
    - Test rendering with sample data
    - _Requirements: 27.2, 27.3_

- [x] 16. Rate limiting and abuse prevention
  - [x] 16.1 Create RateLimitService using Valkey
    - Implement checkContestCreationLimit(userId) - 5 per hour sliding window
    - Implement checkAIProblemGenLimit(userId) - 5 per day
    - Implement checkInviteRegenLimit(contestId) - 10 per hour
    - Implement checkInviteAcceptLimit(contestId) - 100 per hour
    - Use Valkey INCR with TTL for counters
    - Throw TooManyRequestsException with Retry-After header
    - _Requirements: 24.1, 24.2, 24.3, 24.4, 24.5_
  
  - [x] 16.2 Add rate limit interceptor to controllers
    - Create @RateLimited annotation
    - Implement HandlerInterceptor checking limits before controller execution
    - Apply to: POST /api/contests/private, POST /api/contests/private/*/problems/generate, POST /api/contests/private/*/invite/regenerate
    - Log rate limit violations for monitoring
    - _Requirements: 24.5, 24.6_

- [ ] 17. Cache strategy implementation
  - [x] 17.1 Create PrivateContestCacheService
    - Implement cacheContestMetadata(contestId, dto) with 6-hour TTL
    - Implement getCachedContest(contestId) returning Optional<PrivateContestDTO>
    - Implement cacheParticipantSet(contestId, userIds) with 6-hour TTL
    - Implement isCachedParticipant(contestId, userId) for fast membership check
    - Implement invalidateContestCache(contestId)
    - _Requirements: 21.1, 25.3_
  
  - [x] 17.2 Integrate cache into read-heavy operations
    - Use cache in GET /api/contests/private/{id} before hitting database
    - Use cache in participant access checks
    - Invalidate on: participant join, contest edit, problem attach, status change
    - _Requirements: 25.3_

- [ ] 18. Monitoring and observability
  - [ ] 18.1 Add Prometheus metrics for private contests
    - Counter: private_contests_created_total
    - Counter: private_invite_accepted_total
    - Gauge: private_submission_queue_length
    - Histogram: private_submission_judge_latency_seconds
    - Gauge: active_private_contests_count
    - Gauge: active_private_participants_total
    - _Requirements: 39.1, 39.2_
  
  - [ ] 18.2 Create monitoring endpoints
    - GET /api/admin/judge-stats - return queue lengths, avg latency, worker utilization
    - Expose /metrics endpoint for Prometheus scraping
    - _Requirements: 22.3, 39.1, 39.2_

  
  - [ ] 18.3 Configure alerting rules
    - Document alert for private_submission_queue_length > 100 for 5+ minutes
    - Document alert for private_submission_judge_latency > 10s for 5+ minutes
    - Document alert for contest_creation_failure_rate > 10% over 1 hour
    - _Requirements: 39.3_

- [ ] 19. Integration with existing user points system
  - [ ] 19.1 Update submission verdict handler for points
    - When submission status is ACCEPTED in private contest, increment user.totalPoints
    - Use same point calculation as public contests (based on problem difficulty)
    - Record in user_points_history or audit log
    - _Requirements: 35.1, 35.2, 35.3_
  
  - [ ] 19.2 Update user profile to show points from all contest types
    - Ensure existing user profile API includes points from private contests
    - Display on leaderboard with no distinction between public/private sources
    - _Requirements: 35.4_

- [ ] 20. Checkpoint - Backend core functionality complete
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 21. Frontend - Contest hosting request flow
  - [ ] 21.1 Create Request Hosting page
    - Add "Request Hosting" button to user profile page (visible to verified users without hosting request)
    - Create form with reason textarea (500 chars) and intendedUseCase dropdown
    - Submit POST /api/hosting/request
    - Display success message with "Application pending review"
    - _Requirements: 1.1, 28.2_
  
  - [ ] 21.2 Create Admin Hosting Requests dashboard
    - Add menu item "Hosting Requests" to admin panel
    - Display table of pending requests with: username, email, submitted date, reason, use case
    - Add "Approve" and "Reject" buttons per row
    - Show modal for admin notes on approve/reject
    - Call POST /api/admin/hosting/requests/{id}/approve or reject
    - Refresh list after action
    - _Requirements: 2.1, 2.2, 2.3_

- [ ] 22. Frontend - Private contest creation wizard
  - [ ] 22.1 Add "My Private Contests" navigation menu item
    - Show only to users with approved hosting request
    - Fetch and display from GET /api/hosting/request/status
    - _Requirements: 28.1_
  
  - [ ] 22.2 Create "My Private Contests" list page
    - Display list of contests from GET /api/contests/private/my-contests
    - Show: name, status, start time, participant count
    - Add "Create New Contest" button showing quota (e.g., "1/2 used this month")
    - Add filter by status (UPCOMING, LIVE, ENDED)
    - _Requirements: 28.3_

  
  - [ ] 22.3 Create Private Contest creation wizard - Step 1: Basic Info
    - Form fields: name (text, 200 max), description (textarea), startTime (datetime), endTime (datetime)
    - Validate duration <= 5 hours client-side
    - Add enableProctoring checkbox
    - Next button proceeds to Step 2
    - _Requirements: 4.1, 4.2, 28.4_
  
  - [ ] 22.4 Create Private Contest creation wizard - Step 2: Problem Selection
    - Display problem browser with filters: difficulty, search by title
    - Call GET /api/contests/private/problems/available with filters
    - Add "Select" button per problem, maintain local selected list
    - Show "Generate with AI" button opening AI generation modal
    - Next button proceeds to Step 3
    - _Requirements: 8.1, 8.2, 28.4_
  
  - [ ] 22.5 Create AI Problem Generation modal
    - Form fields: prompt (1000 chars), difficulty dropdown, topic (100 chars)
    - Submit POST /api/contests/private/problems/generate
    - Display generated problem in preview pane
    - Add "Accept" button (adds to selected problems) and "Edit" button (opens problem editor)
    - Show rate limit message if 429 returned (5/day)
    - _Requirements: 9.1, 9.2, 9.3, 9.5, 9.7_
  
  - [ ] 22.6 Create Private Contest creation wizard - Step 3: Review & Create
    - Display summary: name, times, problem count, proctoring status
    - Show invite link preview (generated after creation)
    - Submit POST /api/contests/private with full payload
    - Call POST /api/contests/private/{id}/problems to attach selected problems
    - On success, display invite link with "Copy to Clipboard" button
    - Redirect to contest management page
    - _Requirements: 4.9, 5.5, 28.4_

- [ ] 23. Frontend - Contest management dashboard
  - [ ] 23.1 Create Private Contest management page - Overview tab
    - Display: contest name, description, status badge, start/end times, participant count
    - Show countdown timer (time until start or time remaining)
    - Display invite link prominently with "Copy" button and QR code
    - Show "Regenerate Link" button and expiry timestamp
    - Show "Cancel Contest" button if status is UPCOMING
    - _Requirements: 5.5, 5.6, 18.1, 28.5, 28.6, 28.7_
  
  - [ ] 23.2 Create Private Contest management page - Participants tab
    - Display table: username, email, joined date, "Remove" button
    - Call GET /api/contests/private/{id}/participants
    - Remove button calls DELETE /api/contests/private/{id}/participants/{userId}
    - Disable remove button if contest status is LIVE or ENDED
    - _Requirements: 7.3, 7.4, 7.5, 28.5_
  
  - [ ] 23.3 Create Private Contest management page - Problems tab
    - Display list of attached problems with order, title, difficulty
    - Show "Add Problems" button opening problem browser modal
    - Show "Remove" button per problem
    - Allow drag-and-drop reordering (update displayOrder)
    - Disable edits if contest status is LIVE or ENDED
    - _Requirements: 8.4, 8.6, 8.7, 28.5_

  
  - [ ] 23.4 Create Private Contest management page - Leaderboard tab
    - Display real-time leaderboard from GET /api/contests/private/{id}/leaderboard
    - Columns: rank, username, score, penalty, last submission time
    - Auto-refresh every 10 seconds while contest is LIVE
    - _Requirements: 14.3, 14.4, 28.5_
  
  - [ ] 23.5 Create Private Contest management page - Analytics tab
    - Display analytics from GET /api/contests/private/{id}/analytics
    - Show: total participants, active participants, total submissions
    - Display per-problem table: title, submissions, acceptance rate, avg solve time
    - Show engagement timeline chart (Chart.js or similar)
    - Add "Export CSV" button
    - Cache for ENDED contests on frontend
    - _Requirements: 16.1, 16.2, 16.3, 28.5_
  
  - [ ] 23.6 Create Private Contest management page - Proctoring tab (if enabled)
    - Display proctoring sessions from GET /api/contests/private/{id}/proctoring/sessions
    - Show: participant username, risk score, session status
    - Add "View Screenshots" button opening screenshot gallery modal
    - Only show tab if enableProctoring is true
    - _Requirements: 15.3, 15.4_

- [ ] 24. Frontend - Participant invite and join flow
  - [ ] 24.1 Create Invite Link landing page
    - Route: /contest/private/join?token=...
    - Call GET /api/contests/private/join?token=... to preview contest
    - Display: contest name, description, host username, start/end times, participant count
    - Show "Join Contest" button if authenticated, else redirect to login with return URL
    - _Requirements: 6.1, 6.2_
  
  - [ ] 24.2 Implement Join Contest action
    - On "Join Contest" click, call POST /api/contests/private/join with token
    - Handle errors: 404 (expired/invalid token), 429 (full capacity), 409 (already joined)
    - On success, redirect to /contest/{id}
    - _Requirements: 6.3, 6.4, 6.5, 6.8_
  
  - [ ] 24.3 Add "Joined Contests" section to user dashboard
    - Display contests from GET /api/contests/private/joined
    - Show: name, status, start time, "Enter Contest" button
    - _Requirements: 11.3_

- [ ] 25. Frontend - Private contest participant experience
  - [ ] 25.1 Create Private Contest detail page for participants
    - Reuse existing public contest UI components
    - Display: name, description, timer, problem list
    - Show proctoring consent modal if isProctored is true
    - Call GET /api/contests/private/{id} to load contest
    - _Requirements: 11.4, 15.2_
  
  - [ ] 25.2 Integrate code submission for private contests
    - Reuse existing code editor (Monaco) and submission UI
    - Call POST /api/contests/private/{id}/submit
    - Listen to SSE for real-time verdict (reuse existing SSE connection)
    - _Requirements: 13.1, 13.6_

  
  - [ ] 25.3 Display leaderboard for participants
    - Show leaderboard on contest page from GET /api/contests/private/{id}/leaderboard
    - Auto-refresh every 10 seconds during LIVE status
    - Highlight current user's row
    - _Requirements: 14.3, 14.4_

- [ ] 26. Frontend - Admin oversight UI
  - [ ] 26.1 Create Admin Private Contests dashboard
    - Add menu item "Private Contests" to admin panel
    - Display table from GET /api/admin/private-contests
    - Columns: contest ID, host username, name, status, participant count, created date
    - Add filters: status, host user
    - _Requirements: 19.1_
  
  - [ ] 26.2 Add admin actions to private contest management
    - Add "View as Admin" button allowing admins to bypass ownership checks
    - Add "Delete Contest" button calling DELETE /api/admin/private-contests/{id}
    - Show confirmation modal with cascade delete warning
    - _Requirements: 19.2, 19.3_
  
  - [ ] 26.3 Create Judge Stats monitoring page
    - Display from GET /api/admin/judge-stats
    - Show: public queue length, private queue length, avg latency per queue, worker utilization
    - Auto-refresh every 30 seconds
    - _Requirements: 22.3_

- [ ] 27. Checkpoint - Frontend core functionality complete
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 28. Testing and bug fixes
  - [ ]* 28.1 Write integration tests for hosting request workflow
    - Test: submit request → admin approve → user can create contest
    - Test: submit request → admin reject → user cannot create contest
    - Test: duplicate pending request returns 409
    - Test: already approved user cannot submit new request
    - _Requirements: 1.1, 1.3, 1.4, 2.2, 2.3_
  
  - [ ]* 28.2 Write integration tests for contest creation business rules
    - Test: monthly quota enforcement (3rd contest returns 429)
    - Test: duration validation (6-hour contest returns 400)
    - Test: overlap detection (overlapping times return 409)
    - Test: proctoring integration (enableProctoring creates proctored_contests row)
    - _Requirements: 3.2, 4.2, 4.3, 4.4, 4.5, 4.7_
  
  - [ ]* 28.3 Write integration tests for invite link flow
    - Test: valid token allows join
    - Test: expired token returns 404
    - Test: invalidated token returns 404
    - Test: participant limit (101st user returns 429)
    - Test: duplicate join returns 409
    - _Requirements: 6.3, 6.4, 6.5, 6.7_
  
  - [ ]* 28.4 Write integration tests for submission and leaderboard
    - Test: submission creates row and pushes to private:submission:queue
    - Test: verdict callback updates private:leaderboard with delta scoring
    - Test: leaderboard returns sorted rankings
    - _Requirements: 13.1, 13.2, 13.7, 14.2, 14.3_

  
  - [ ]* 28.5 Write unit tests for business validation services
    - Test: PrivateContestBusinessRules.validateMonthlyQuota
    - Test: PrivateContestBusinessRules.validateDuration
    - Test: PrivateContestBusinessRules.validateNoOverlap
    - Test: InviteTokenService.generateToken (verify length, uniqueness)
    - Test: InviteTokenService.validateToken (various expiry/invalidation scenarios)
    - _Requirements: 3.1, 4.2, 4.4, 5.1, 6.3_
  
  - [ ]* 28.6 Write unit tests for rate limiting
    - Test: RateLimitService.checkContestCreationLimit (6th request in hour fails)
    - Test: RateLimitService.checkAIProblemGenLimit (6th request in day fails)
    - Mock Valkey operations
    - _Requirements: 24.1, 24.2_
  
  - [ ]* 28.7 Perform end-to-end testing
    - Manual test: full contest lifecycle (create → invite → join → submit → verdict → leaderboard → end)
    - Manual test: proctoring flow (enable → consent → face detection → screenshots)
    - Manual test: analytics export as CSV
    - Manual test: contest cancellation email notifications
    - _Requirements: All major workflows_

- [ ] 29. Documentation and help resources
  - [ ] 29.1 Write API documentation
    - Generate Swagger/OpenAPI spec for all private contest endpoints
    - Document request/response schemas, error codes, authentication requirements
    - _Requirements: 40.2_
  
  - [ ] 29.2 Create user guide for Contest Hosts
    - Write tutorial: "How to Request Hosting Privileges"
    - Write tutorial: "How to Create a Private Contest"
    - Write tutorial: "How to Generate and Share Invite Links"
    - Write tutorial: "How to Select Problems and Use AI Generator"
    - Write tutorial: "How to Enable Proctoring"
    - Write tutorial: "How to Interpret Analytics Dashboard"
    - Publish in frontend "Help" section
    - _Requirements: 40.1_
  
  - [ ] 29.3 Create operator documentation
    - Document monitoring metrics and alerting rules
    - Document backup and disaster recovery procedures for new tables
    - Document cache key patterns and TTL values
    - Document judge queue separation and worker configuration
    - _Requirements: 38.3, 39.1, 39.3_

- [ ] 30. Final checkpoint and deployment preparation
  - Ensure all tests pass, ask the user if questions arise.


## Notes

- **Technology**: Java 21, Spring Boot 3, PostgreSQL 18, Valkey 7, React 19
- **Naming Conventions**: snake_case for database, camelCase for Java
- **Annotations**: Use Lombok `@Data` for entities, `@Autowired` field injection for services
- **Testing**: Tasks marked with `*` are optional test-related sub-tasks and can be skipped for faster MVP
- **Checkpoints**: Pause at checkpoints 20, 27, and 30 to verify all tests pass and ask user for guidance
- **Requirements Traceability**: Each task references specific requirements from the requirements document
- **Integration**: All features integrate with existing CodeCoder subsystems - no separate microservice
- **Judge Workers**: Configured on VM2 (Oracle Cloud 6GB RAM) with JUDGE_WORKERS=6, draining both public and private queues
- **Cache Prefixes**: `private:contest:*`, `private:leaderboard:*`, `private:submission:queue`, `private:score:*`
- **Email Service**: Reuse existing `MailConfig` with `noreplyMailSender` for all notifications

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3", "2.4"] },
    { "id": 2, "tasks": ["2.5", "3.1", "7.1"] },
    { "id": 3, "tasks": ["3.2", "3.3", "4.1", "8.1", "14.1"] },
    { "id": 4, "tasks": ["4.2", "4.3", "5.1", "14.2"] },
    { "id": 5, "tasks": ["5.2", "5.3", "5.4", "6.1", "7.2", "14.3"] },
    { "id": 6, "tasks": ["6.2", "6.3", "6.4", "7.3", "17.1"] },
    { "id": 7, "tasks": ["7.4", "7.5", "8.2", "15.1", "17.2"] },
    { "id": 8, "tasks": ["8.3", "8.4", "8.5", "9.1", "15.2"] },
    { "id": 9, "tasks": ["9.2", "10.1", "11.1", "12.1", "13.1", "16.1"] },
    { "id": 10, "tasks": ["10.2", "10.3", "11.2", "11.3", "12.2", "13.2", "16.2"] },
    { "id": 11, "tasks": ["11.4", "12.3", "14.4", "18.1", "19.1"] },
    { "id": 12, "tasks": ["18.2", "18.3", "19.2"] },
    { "id": 13, "tasks": ["20"] },
    { "id": 14, "tasks": ["21.1", "21.2", "22.1"] },
    { "id": 15, "tasks": ["22.2", "22.3", "24.1"] },
    { "id": 16, "tasks": ["22.4", "22.5", "24.2"] },
    { "id": 17, "tasks": ["22.6", "23.1", "24.3"] },
    { "id": 18, "tasks": ["23.2", "23.3", "25.1"] },
    { "id": 19, "tasks": ["23.4", "23.5", "25.2"] },
    { "id": 20, "tasks": ["23.6", "25.3", "26.1"] },
    { "id": 21, "tasks": ["26.2", "26.3"] },
    { "id": 22, "tasks": ["27"] },
    { "id": 23, "tasks": ["28.1", "28.2", "28.3", "28.4", "28.5", "28.6"] },
    { "id": 24, "tasks": ["28.7", "29.1", "29.2", "29.3"] },
    { "id": 25, "tasks": ["30"] }
  ]
}
```
