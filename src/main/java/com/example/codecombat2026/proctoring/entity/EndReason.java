package com.example.codecombat2026.proctoring.entity;

/**
 * Terminal reason persisted on {@code proctoring_sessions.end_reason}.
 *
 * <p>Mirrors the V7 CHECK constraint
 * {@code end_reason IN ('CONTEST_ENDED','SELF_FINISHED','SELF_QUIT',
 * 'ADMIN_FORCED','HEARTBEAT_TIMEOUT')}. Mapped via
 * {@link jakarta.persistence.EnumType#STRING} so the database CHECK
 * constraint stays authoritative.
 *
 * <p>Semantics:
 * <ul>
 *   <li>{@link #CONTEST_ENDED} — closed by the per-contest sweep when the
 *       parent contest's {@code end_time} elapses (Req 13.5).</li>
 *   <li>{@link #SELF_FINISHED} — candidate clicked "Finish" while at least
 *       one accepted submission existed (Req 13.6).</li>
 *   <li>{@link #SELF_QUIT} — candidate clicked "Quit" — triggers permanent
 *       lockout for that {@code (user_id, contest_id)} (Req 13.6).</li>
 *   <li>{@link #ADMIN_FORCED} — admin force-ended the session (Req 15.5).</li>
 *   <li>{@link #HEARTBEAT_TIMEOUT} — server lost the candidate's heartbeat
 *       past {@code proctoring.heartbeat-timeout-seconds} (Req 13.7).</li>
 * </ul>
 */
public enum EndReason {
    CONTEST_ENDED,
    SELF_FINISHED,
    SELF_QUIT,
    ADMIN_FORCED,
    HEARTBEAT_TIMEOUT
}
