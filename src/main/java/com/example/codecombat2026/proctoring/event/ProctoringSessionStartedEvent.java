package com.example.codecombat2026.proctoring.event;

/**
 * Fired by {@code ProctoringSessionService.createSession} after a fresh
 * {@code proctoring_sessions} row has been persisted (Req 15.1, 15.2).
 *
 * <p>The admin SSE bridge (
 * {@link com.example.codecombat2026.proctoring.event.ProctoringSseListener})
 * uses this event to push a {@code SESSION_STARTED} frame to every
 * admin subscriber on the matching {@code contestId}, so newly-arrived
 * candidates appear in the live grid without a manual refresh.
 *
 * <p>This is a thin announcement payload — the admin UI is expected to
 * call {@code GET /api/admin/proctoring/contests/{cid}/sessions} for the
 * authoritative row state on receipt.
 */
public record ProctoringSessionStartedEvent(
        Long sessionId,
        Long contestId,
        Long userId
) {}
