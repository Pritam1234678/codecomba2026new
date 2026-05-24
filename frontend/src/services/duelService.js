import api from './api';

// ─── User-facing duel endpoints ──────────────────────────────────────────────

/**
 * Enqueue the current user into the matchmaking queue.
 * Idempotent on the backend (5s SET NX EX gate), so double-clicks are safe.
 * Resolves with `{ queueToken, position }` (shape per backend response).
 */
export const enqueue = () =>
  api.post('/duels/queue', {}).then((r) => r.data);

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
 *
 * TODO: backend endpoint `/api/user/duel-history` is pending implementation.
 * The wrapper is provided here so the lobby UI can be wired up; calls will
 * 404 until the controller lands.
 */
export const getDuelHistory = (limit = 10) =>
  api.get(`/user/duel-history?limit=${limit}`).then((r) => r.data);

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
  forfeit,
  heartbeat,
  issueDuelTicket,
  getDuelHistory,
  getDuelMetrics,
  listAdminMatches,
  adminCancelMatch,
  listEligibleProblems,
  addEligibleProblem,
  removeEligibleProblem,
};

export default DuelService;
