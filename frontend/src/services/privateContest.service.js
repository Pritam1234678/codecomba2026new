/**
 * Private Contest Service
 * 
 * Handles all API calls related to private contest hosting and management:
 * - Hosting request submission and status
 * - Contest creation and management
 * - Invitation handling
 * - Participant management
 * - Problem management (browse, attach, remove, AI generation)
 * - Submission handling
 * - Leaderboard access
 * - Analytics and reporting
 * - Proctoring data access (for hosts)
 */

import api from './api';

const PrivateContestService = {
    // ────────────────────────────────────────────────────────────────────────────
    // HOSTING REQUEST APIs
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Submit a hosting request
     * @param {string} reason - Why the user wants hosting privileges
     * @param {string} intendedUseCase - EDUCATION | RECRUITMENT | COMPETITION | TRAINING | OTHER
     * @returns {Promise<Object>} Created hosting request
     */
    submitHostingRequest(reason, intendedUseCase) {
        return api.post('/hosting-requests/submit', { reason, intendedUseCase });
    },

    /**
     * Get current user's hosting request status
     * @returns {Promise<Object>} { hasRequest, canCreateContests, status, request }
     */
    getMyHostingStatus() {
        return api.get('/hosting-requests/my-status');
    },

    // ────────────────────────────────────────────────────────────────────────────
    // CONTEST MANAGEMENT APIs
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Create a new private contest
     * @param {Object} contestData - { name, description, startTime, endTime, enableProctoring }
     * @returns {Promise<Object>} Created contest with invite link
     */
    createPrivateContest(contestData) {
        return api.post('/contests/hosting/create', contestData);
    },

    /**
     * Get list of contests where user is the host
     * @returns {Promise<Array>} List of hosted contests
     */
    getMyContests() {
        return api.get('/contests/hosting/my-contests');
    },

    /**
     * Get list of contests where user is a participant
     * @returns {Promise<Array>} List of joined contests
     */
    getJoinedContests() {
        return api.get('/contests/private/joined');
    },

    /**
     * Get contest details (must be host or participant)
     * @param {number} contestId - Contest ID
     * @returns {Promise<Object>} Contest details
     */
    getContestDetails(contestId) {
        return api.get(`/contests/private/${contestId}`);
    },

    /**
     * Update contest details
     * @param {number} contestId - Contest ID
     * @param {Object} updates - { name, description, startTime, endTime }
     * @returns {Promise<Object>} Updated contest
     */
    updateContest(contestId, updates) {
        return api.put(`/contests/hosting/${contestId}/update`, updates);
    },

    /**
     * Cancel a contest
     * @param {number} contestId - Contest ID
     * @returns {Promise<Object>} Success message
     */
    cancelContest(contestId) {
        return api.post(`/contests/hosting/${contestId}/cancel`);
    },

    // ────────────────────────────────────────────────────────────────────────────
    // INVITATION APIs
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Preview contest from invite token (public, no auth required)
     * @param {string} token - Invite token
     * @returns {Promise<Object>} Contest preview details
     */
    previewInvite(token) {
        return api.get(`/contests/private/join?token=${token}`);
    },

    /**
     * Accept invitation and join contest
     * @param {string} token - Invite token
     * @returns {Promise<Object>} { contestId, userId, joinedAt, redirectUrl }
     */
    acceptInvitation(token) {
        return api.post('/contests/private/join', { token });
    },

    /**
     * Regenerate invite token (host only)
     * @param {number} contestId - Contest ID
     * @returns {Promise<Object>} { inviteLink, token, expiresAt }
     */
    regenerateInviteToken(contestId) {
        return api.post(`/contests/private/${contestId}/invite/regenerate`);
    },

    /**
     * Update invite token expiry (host only)
     * @param {number} contestId - Contest ID
     * @param {string} expiresAt - ISO 8601 datetime
     * @returns {Promise<Object>} { token, expiresAt, inviteLink }
     */
    updateInviteExpiry(contestId, expiresAt) {
        return api.put(`/contests/private/${contestId}/invite/expiry`, { expiresAt });
    },

    // ────────────────────────────────────────────────────────────────────────────
    // PARTICIPANT MANAGEMENT APIs
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * List all participants in a contest (host only)
     * @param {number} contestId - Contest ID
     * @returns {Promise<Object>} { contestId, participantCount, participants }
     */
    getParticipants(contestId) {
        return api.get(`/contests/private/${contestId}/participants`);
    },

    /**
     * Remove a participant from contest (host only, before contest starts)
     * @param {number} contestId - Contest ID
     * @param {number} userId - User ID to remove
     * @returns {Promise<void>}
     */
    removeParticipant(contestId, userId) {
        return api.delete(`/contests/private/${contestId}/participants/${userId}`);
    },

    // ────────────────────────────────────────────────────────────────────────────
    // PROBLEM MANAGEMENT APIs
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Browse available problems to attach (host only)
     * @param {number} contestId - Contest ID
     * @param {Object} filters - { difficulty, search }
     * @returns {Promise<Array>} List of available problems
     */
    browseAvailableProblems(contestId, filters = {}) {
        const params = new URLSearchParams();
        if (filters.difficulty) params.append('difficulty', filters.difficulty);
        if (filters.search) params.append('search', filters.search);

        return api.get(`/contests/private/${contestId}/problems/available?${params.toString()}`);
    },

    /**
     * Attach problems to contest (host only)
     * @param {number} contestId - Contest ID
     * @param {Array<number>} problemIds - Problem IDs to attach
     * @returns {Promise<Object>} { attachedCount, message }
     */
    attachProblems(contestId, problemIds) {
        return api.post(`/contests/private/${contestId}/problems`, { problemIds });
    },

    /**
     * Remove a problem from contest (host only, before contest starts)
     * @param {number} contestId - Contest ID
     * @param {number} problemId - Problem ID to remove
     * @returns {Promise<Object>} Success message
     */
    removeProblem(contestId, problemId) {
        return api.delete(`/contests/private/${contestId}/problems/${problemId}`);
    },

    /**
     * Generate a problem using AI (host only)
     * @param {number} contestId - Contest ID
     * @param {Object} params - { prompt, difficulty, topic }
     * @returns {Promise<Object>} Generated problem
     */
    generateAIProblem(contestId, params) {
        return api.post(`/contests/private/${contestId}/problems/generate`, params);
    },

    /**
     * Edit a problem (owner or admin only)
     * @param {number} contestId - Contest ID
     * @param {number} problemId - Problem ID
     * @param {Object} updates - Problem fields to update
     * @returns {Promise<Object>} Updated problem
     */
    editProblem(contestId, problemId, updates) {
        return api.put(`/contests/private/${contestId}/problems/${problemId}/edit`, updates);
    },

    // ────────────────────────────────────────────────────────────────────────────
    // SUBMISSION APIs
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Submit code for a problem in private contest
     * @param {number} contestId - Contest ID
     * @param {Object} submission - { problemId, code, language }
     * @returns {Promise<Object>} Submission object with ID
     */
    submitCode(contestId, submission) {
        return api.post(`/contests/private/${contestId}/submit`, submission);
    },

    /**
     * Get all submissions for a contest (host only)
     * @param {number} contestId - Contest ID
     * @param {Object} filters - { userId, problemId, status, page, size }
     * @returns {Promise<Object>} Paginated submissions
     */
    getContestSubmissions(contestId, filters = {}) {
        const params = new URLSearchParams();
        if (filters.userId) params.append('userId', filters.userId);
        if (filters.problemId) params.append('problemId', filters.problemId);
        if (filters.status) params.append('status', filters.status);
        params.append('page', filters.page || 0);
        params.append('size', filters.size || 20);

        return api.get(`/contests/private/${contestId}/submissions?${params.toString()}`);
    },

    // ────────────────────────────────────────────────────────────────────────────
    // LEADERBOARD APIs
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Get leaderboard for private contest (host or participant only)
     * @param {number} contestId - Contest ID
     * @returns {Promise<Array>} Leaderboard entries
     */
    getLeaderboard(contestId) {
        return api.get(`/contests/private/${contestId}/leaderboard`);
    },

    // ────────────────────────────────────────────────────────────────────────────
    // ANALYTICS APIs
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Get analytics for a contest (host only)
     * @param {number} contestId - Contest ID
     * @returns {Promise<Object>} Analytics data
     */
    getAnalytics(contestId) {
        return api.get(`/contests/private/${contestId}/analytics`);
    },

    /**
     * Export analytics as CSV (host only)
     * @param {number} contestId - Contest ID
     * @returns {Promise<Blob>} CSV file data
     */
    exportAnalyticsCSV(contestId) {
        return api.get(`/contests/private/${contestId}/analytics/export`, {
            responseType: 'blob'
        });
    },

    // ────────────────────────────────────────────────────────────────────────────
    // PROCTORING APIs (Host Access)
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Get proctoring sessions for contest (host only)
     * @param {number} contestId - Contest ID
     * @param {boolean} flagged - Optional filter for flagged sessions only
     * @returns {Promise<Array>} List of proctoring sessions
     */
    getProctoringSession(contestId, flagged = null) {
        const params = new URLSearchParams();
        if (flagged !== null) params.append('flagged', flagged);

        return api.get(`/contests/private/${contestId}/proctoring/sessions?${params.toString()}`);
    },

    /**
     * Get detailed proctoring data for a session (host only)
     * @param {number} contestId - Contest ID
     * @param {number} sessionId - Session ID
     * @returns {Promise<Object>} { session, events, screenshots, submissions }
     */
    getProctoringSessionDetail(contestId, sessionId) {
        return api.get(`/contests/private/${contestId}/proctoring/sessions/${sessionId}`);
    },

    // ────────────────────────────────────────────────────────────────────────────
    // ADMIN APIs (for completeness)
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * List all private contests (admin only)
     * @param {Object} filters - { status, hostUserId, cancelled, createdAfter, createdBefore, page, size }
     * @returns {Promise<Object>} Paginated contests
     */
    adminListAllContests(filters = {}) {
        const params = new URLSearchParams();
        if (filters.status) params.append('status', filters.status);
        if (filters.hostUserId) params.append('hostUserId', filters.hostUserId);
        if (filters.cancelled !== undefined) params.append('cancelled', filters.cancelled);
        if (filters.createdAfter) params.append('createdAfter', filters.createdAfter);
        if (filters.createdBefore) params.append('createdBefore', filters.createdBefore);
        params.append('page', filters.page || 0);
        params.append('size', filters.size || 20);
        params.append('sort', filters.sort || 'createdAt,desc');

        return api.get(`/admin/private-contests?${params.toString()}`);
    },

    /**
     * Get contest details (admin only, bypasses access checks)
     * @param {number} contestId - Contest ID
     * @returns {Promise<Object>} Full contest details
     */
    adminGetContestDetails(contestId) {
        return api.get(`/admin/private-contests/${contestId}`);
    },

    /**
     * Delete a private contest (admin only)
     * @param {number} contestId - Contest ID
     * @returns {Promise<Object>} Success response
     */
    adminDeleteContest(contestId) {
        return api.delete(`/admin/private-contests/${contestId}`);
    },

    /**
     * Get judge queue statistics (admin only)
     * @returns {Promise<Object>} Queue stats
     */
    adminGetJudgeStats() {
        return api.get('/admin/private-contests/judge-stats');
    },

    /**
     * Query audit logs (admin only)
     * @param {Object} filters - { userId, action, resourceType, startDate, endDate, page, size }
     * @returns {Promise<Object>} Paginated audit logs
     */
    adminQueryAuditLogs(filters = {}) {
        const params = new URLSearchParams();
        if (filters.userId) params.append('userId', filters.userId);
        if (filters.action) params.append('action', filters.action);
        if (filters.resourceType) params.append('resourceType', filters.resourceType);
        if (filters.startDate) params.append('startDate', filters.startDate);
        if (filters.endDate) params.append('endDate', filters.endDate);
        params.append('page', filters.page || 0);
        params.append('size', filters.size || 50);
        params.append('sort', filters.sort || 'timestamp,desc');

        return api.get(`/admin/private-contests/audit-logs?${params.toString()}`);
    }
};

export default PrivateContestService;
