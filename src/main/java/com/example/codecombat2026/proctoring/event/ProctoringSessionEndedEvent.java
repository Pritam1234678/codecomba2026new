package com.example.codecombat2026.proctoring.event;

import com.example.codecombat2026.proctoring.entity.EndReason;

/**
 * Fired by {@code ProctoringSessionService} immediately after a
 * conditional-UPDATE close successfully returned {@code 1 row} (Req
 * 13.4 - 13.8). Because the close is single-writer, this event is
 * also single-writer — duplicate close attempts (force-end racing
 * heartbeat-timeout, double-click on Quit, etc.) collapse to no
 * event publication, so admin subscribers see exactly one
 * {@code SESSION_ENDED} per session.
 *
 * <p>{@code endReason} is the value persisted into
 * {@code proctoring_sessions.end_reason}; it is never {@code null} on
 * a real close, so the listener can rely on it for terminal-screen
 * routing on the admin side.
 */
public record ProctoringSessionEndedEvent(
        Long sessionId,
        Long contestId,
        EndReason endReason
) {}
