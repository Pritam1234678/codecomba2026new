package com.example.codecombat2026.proctoring.repository;

import com.example.codecombat2026.proctoring.entity.ProctoringAdminAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ProctoringAdminAudit}.
 *
 * <p>The audit table is append-only (Req 21.5) — there is no delete
 * method here on purpose; admin actions persist forever via the
 * inherited {@code save} only. The single read pattern is
 * {@link #findBySessionIdOrderByActedAtDesc(Long)}, which feeds the
 * "admin actions" panel on the session detail view, newest first.
 */
@Repository
public interface ProctoringAdminAuditRepository extends JpaRepository<ProctoringAdminAudit, Long> {

    /**
     * All audit entries for a session, newest first. Drives the admin
     * session detail view's actions panel.
     *
     * @param sessionId owning {@code proctoring_sessions.id}
     * @return audit rows ordered descending by {@code acted_at}
     */
    List<ProctoringAdminAudit> findBySessionIdOrderByActedAtDesc(Long sessionId);
}
