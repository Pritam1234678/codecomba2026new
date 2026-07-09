/**
 * Private Contest Service
 *
 * All API calls for the Private Contest Hosting feature:
 * hosting requests, contest CRUD, invitations, participants, problems,
 * submissions, leaderboard, analytics, proctoring, and admin oversight.
 *
 * Paths verified against the actual Spring controllers:
 *   ContestHostingController          /api/hosting-requests/*
 *   PrivateContestController          /api/contests/private, /api/contests/private/{id, my-contests, joined, id/cancel, id/clone}
 *   PrivateContestInviteController    /api/contests/private/{id}/invite/*, /join, /participants
 *   PrivateContestProblemController   /api/contests/private/{id}/problems/*
 *   PrivateContestSubmissionController /api/contests/private/{id}/submit, /submissions
 *   PrivateContestLeaderboardController /api/contests/private/{id}/leaderboard
 *   PrivateContestAnalyticsController /api/contests/private/{id}/analytics*
 *   PrivateContestProctoringController /api/contests/private/{id}/proctoring/*
 *   PrivateContestAdminController     /api/admin/private-contests/*
 */

import api from './api';

const PrivateContestService = {
    // ── Hosting requests ──────────────────────────────────────────────────────
    submitHostingRequest(reason, intendedUseCase) {
        return api.post('/hosting-requests/submit', { reason, intendedUseCase });
    },
    getMyHostingStatus() {
        return api.get('/hosting-requests/my-status');
    },

    // ── Contest lifecycle ─────────────────────────────────────────────────────
    createPrivateContest(contestData) {
        return api.post('/contests/private', contestData);
    },
    updateContest(contestId, updates) {
        return api.put(`/contests/private/${contestId}`, updates);
    },
    cancelContest(contestId, reason) {
        return api.put(`/contests/private/${contestId}/cancel`, reason ? { reason } : {});
    },
    cloneContest(contestId) {
        return api.post(`/contests/private/${contestId}/clone`);
    },

    // ── Contest listing / details ─────────────────────────────────────────────
    getMyContests() {
        return api.get('/contests/private/my-contests');
    },
    getJoinedContests() {
        return api.get('/contests/private/joined');
    },
    getContestDetails(contestId) {
        return api.get(`/contests/private/${contestId}`);
    },

    // ── Invitations ────────────────────────────────────────────────────────────
    previewInvite(token) {
        return api.get(`/contests/private/join?token=${encodeURIComponent(token)}`);
    },
    acceptInvitation(token) {
        return api.post('/contests/private/join', { token });
    },
    regenerateInviteToken(contestId) {
        return api.post(`/contests/private/${contestId}/invite/regenerate`);
    },
    updateInviteExpiry(contestId, expiresAt) {
        return api.put(`/contests/private/${contestId}/invite/expiry`, { expiresAt });
    },

    // ── Participants ───────────────────────────────────────────────────────────
    getParticipants(contestId) {
        return api.get(`/contests/private/${contestId}/participants`);
    },
    removeParticipant(contestId, userId) {
        return api.delete(`/contests/private/${contestId}/participants/${userId}`);
    },

    // ── Problems ────────────────────────────────────────────────────────────────
    browseAvailableProblems(contestId, filters = {}) {
        const params = new URLSearchParams();
        if (filters.difficulty) params.append('difficulty', filters.difficulty);
        if (filters.search) params.append('search', filters.search);
        return api.get(`/contests/private/${contestId}/problems/available?${params.toString()}`);
    },
    attachProblems(contestId, problemIds) {
        return api.post(`/contests/private/${contestId}/problems`, { problemIds });
    },
    removeProblem(contestId, problemId) {
        return api.delete(`/contests/private/${contestId}/problems/${problemId}`);
    },
    generateAIProblem(contestId, params) {
        return api.post(`/contests/private/${contestId}/problems/generate`, params);
    },
    editProblem(contestId, problemId, updates) {
        return api.put(`/contests/private/${contestId}/problems/${problemId}/edit`, updates);
    },

    // ── Submissions ─────────────────────────────────────────────────────────────
    submitCode(contestId, submission) {
        return api.post(`/contests/private/${contestId}/submit`, submission);
    },
    getContestSubmissions(contestId, filters = {}) {
        const params = new URLSearchParams();
        if (filters.userId) params.append('userId', filters.userId);
        if (filters.problemId) params.append('problemId', filters.problemId);
        if (filters.status) params.append('status', filters.status);
        params.append('page', filters.page || 0);
        params.append('size', filters.size || 20);
        return api.get(`/contests/private/${contestId}/submissions?${params.toString()}`);
    },

    // ── Leaderboard ─────────────────────────────────────────────────────────────
    getLeaderboard(contestId) {
        return api.get(`/contests/private/${contestId}/leaderboard`);
    },

    // ── Analytics ────────────────────────────────────────────────────────────────
    getAnalytics(contestId) {
        return api.get(`/contests/private/${contestId}/analytics`);
    },
    exportAnalyticsCSV(contestId) {
        return api.get(`/contests/private/${contestId}/analytics/export`, { responseType: 'blob' });
    },

    // ── Proctoring (host access) ────────────────────────────────────────────────
    getProctoringSessions(contestId, flagged = null) {
        const params = new URLSearchParams();
        if (flagged !== null) params.append('flagged', flagged);
        return api.get(`/contests/private/${contestId}/proctoring/sessions?${params.toString()}`);
    },
    getProctoringSessionDetail(contestId, sessionId) {
        return api.get(`/contests/private/${contestId}/proctoring/sessions/${sessionId}`);
    },

    // ── Admin ────────────────────────────────────────────────────────────────────
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
    adminGetContestDetails(contestId) {
        return api.get(`/admin/private-contests/${contestId}`);
    },
    adminDeleteContest(contestId) {
        return api.delete(`/admin/private-contests/${contestId}`);
    },
    adminGetJudgeStats() {
        return api.get('/admin/private-contests/judge-stats');
    },
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
    },
};

export default PrivateContestService;
