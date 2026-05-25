import api from './api';

// ─── User-facing duel endpoints ──────────────────────────────────────────────

/**
 * Enqueue the current user into the matchmaking queue.
 * Idempotent on the backend (5s SET NX EX gate), so double-clicks are safe.
 *
 * @param difficulty one of 'EASY' | 'MEDIUM' | 'HARD' (V4)
 * @returns `{ queueToken, queuedAt, difficulty }`
 */
export const enqueue = (difficulty) =>
  api.post('/duels/queue', { difficulty }).then((r) => r.data);

/**
 * Remove the current user from the matchmaking queue. No-op if not queued.
 */
export const cancelQueue = () =>
  api.delete('/duels/queue').then((r) => r.data);

/**
 * Fetch a duel match record. Backend returns 403 if the requester is not a participant.
 */
export const getMatch = (matchId) =>
  api.get(`/duels/${matchId}`).then((r) => r.data);

/**
 * Submit code for a duel match. Resolves with `{ submissionId }`.
 */
export const submitDuelCode = (matchId, code, language) =>
  api
    .post(`/duels/${matchId}/submissions`, { code, language })
    .then((r) => r.data);

/**
 * Run user code synchronously against the problem's example test cases.
 * V4 — counts toward the 5-runs-per-match limit. Returns
 * `{ passed, compileError, cases, runsUsed, runsRemaining }`.
 */
export const runDuelCode = (matchId, body) =>
  api.post(`/duels/${matchId}/run`, body).then((r) => r.data);

/**
 * Forfeit a duel match. Opponent is awarded the win (outcome=ABANDONED).
 */
export const forfeit = (matchId) =>
  api.post(`/duels/${matchId}/forfeit`).then((r) => r.data);

/**
 * Send a presence heartbeat. Default kind is 'typing'; the backend debounces
 * and rebroadcasts to the opponent over the duel SSE channel.
 */
export const heartbeat = (matchId, kind = 'typing') =>
  api.post(`/duels/${matchId}/heartbeat`, { kind }).then((r) => r.data);

/**
 * Mint a single-use SSE ticket for `/api/duels/{matchId}/stream`.
 * Resolves with the raw ticket string (the response body shape is
 * `{ ticket }`, so we unwrap it here for callers).
 */
export const issueDuelTicket = (matchId) =>
  api.post(`/duels/${matchId}/sse-ticket`).then((r) => r.data.ticket);

/**
 * Recent duel history for the current user.
 * Resolves with an array of `DuelHistoryEntry`.
 */
export const getDuelHistory = (limit = 10) =>
  api.get(`/duels/history?limit=${limit}`).then((r) => r.data);

/**
 * Per-match submission list for the calling user. Used by the arena to
 * rebuild "Your submissions" on mount/refresh.
 */
export const getMatchSubmissions = (matchId) =>
  api.get(`/duels/${matchId}/submissions`).then((r) => r.data);

/**
 * Check if the current user has an active (IN_PROGRESS or WAITING) duel match.
 * Returns the match info or null if no active match (204 response).
 */
export const getActiveMatch = () =>
  api.get('/duels/active').then((r) => r.data).catch((err) => {
    if (err?.response?.status === 204) return null;
    throw err;
  });

// ─── Admin endpoints (require ROLE_ADMIN) ────────────────────────────────────

/**
 * Aggregate duel metrics: queue depth, active matches, finished-today counts, etc.
 */
export const getDuelMetrics = () =>
  api.get('/admin/duels/metrics').then((r) => r.data);

/**
 * Paginated list of duel matches for the admin console.
 */
export const listAdminMatches = ({
  status = 'IN_PROGRESS',
  limit = 50,
  offset = 0,
} = {}) =>
  api
    .get(`/admin/duels?status=${status}&limit=${limit}&offset=${offset}`)
    .then((r) => r.data);

/**
 * Force-cancel an in-progress duel match (outcome=ABANDONED, no winner).
 */
export const adminCancelMatch = (matchId) =>
  api.post(`/admin/duels/${matchId}/cancel`).then((r) => r.data);

/**
 * List the curated pool of problems eligible for duels.
 */
export const listEligibleProblems = () =>
  api.get('/admin/duels/eligible-problems').then((r) => r.data);

/**
 * Add a problem to the duel-eligible pool.
 */
export const addEligibleProblem = (problemId) =>
  api.post(`/admin/duels/eligible-problems/${problemId}`).then((r) => r.data);

/**
 * Remove a problem from the duel-eligible pool.
 */
export const removeEligibleProblem = (problemId) =>
  api
    .delete(`/admin/duels/eligible-problems/${problemId}`)
    .then((r) => r.data);

const DuelService = {
  enqueue,
  cancelQueue,
  getMatch,
  submitDuelCode,
  runDuelCode,
  forfeit,
  heartbeat,
  issueDuelTicket,
  getDuelHistory,
  getMatchSubmissions,
  getActiveMatch,
  getDuelMetrics,
  listAdminMatches,
  adminCancelMatch,
  listEligibleProblems,
  addEligibleProblem,
  removeEligibleProblem,
};

export default DuelService;
