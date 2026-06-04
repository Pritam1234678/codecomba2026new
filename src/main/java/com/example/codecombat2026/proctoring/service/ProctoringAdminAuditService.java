package com.example.codecombat2026.proctoring.service;

import com.example.codecombat2026.proctoring.entity.ProctoringAdminAudit;
import com.example.codecombat2026.proctoring.repository.ProctoringAdminAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Append-only audit logger for admin actions on proctoring sessions
 * (Req 15.7, 21.5).
 *
 * <p>Every admin-initiated force-end or warning persists exactly one row
 * to {@code proctoring_admin_audit} via this service. The table itself
 * has no DELETE endpoint exposed anywhere — admin actions are kept
 * forever for post-contest review and dispute handling, and the V7
 * CHECK constraint {@code action IN ('FORCE_END','WARNING')} is the
 * authoritative whitelist (the {@link #ACTION_FORCE_END} /
 * {@link #ACTION_WARNING} constants here mirror that whitelist so
 * callers don't have to hard-code string literals).
 *
 * <p>This service is the only audit-write path; the admin controller
 * (task 10.2) is expected to call {@link #logForceEnd(Long, Long, String)}
 * before/after invoking {@code sessionService.forceEnd} and
 * {@link #logWarning(Long, Long, String)} alongside the WARNING frame
 * push, so the audit row exists even if the WebSocket close-frame
 * delivery is best-effort.
 */
@Service
public class ProctoringAdminAuditService {

    /** Audit action value for an admin-initiated force-end. */
    public static final String ACTION_FORCE_END = "FORCE_END";

    /** Audit action value for an admin-initiated warning. */
    public static final String ACTION_WARNING = "WARNING";

    private final ProctoringAdminAuditRepository auditRepo;

    public ProctoringAdminAuditService(ProctoringAdminAuditRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    /**
     * Persist a {@code FORCE_END} audit row.
     *
     * @param adminId   acting admin's {@code users.id}
     * @param sessionId target {@code proctoring_sessions.id}
     * @param reason    free-text reason supplied by the admin
     * @return the saved audit row (with generated id and {@code actedAt})
     */
    @Transactional
    public ProctoringAdminAudit logForceEnd(Long adminId, Long sessionId, String reason) {
        return write(adminId, sessionId, ACTION_FORCE_END, reason);
    }

    /**
     * Persist a {@code WARNING} audit row.
     *
     * @param adminId   acting admin's {@code users.id}
     * @param sessionId target {@code proctoring_sessions.id}
     * @param message   warning text shown to the candidate; stored verbatim in {@code reason}
     * @return the saved audit row (with generated id and {@code actedAt})
     */
    @Transactional
    public ProctoringAdminAudit logWarning(Long adminId, Long sessionId, String message) {
        return write(adminId, sessionId, ACTION_WARNING, message);
    }

    private ProctoringAdminAudit write(Long adminId, Long sessionId, String action, String reason) {
        ProctoringAdminAudit row = new ProctoringAdminAudit();
        row.setAdminId(adminId);
        row.setSessionId(sessionId);
        row.setAction(action);
        row.setActedAt(LocalDateTime.now());
        row.setReason(reason);
        return auditRepo.save(row);
    }
}
