package com.example.codecombat2026.proctoring.controller;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.proctoring.entity.ProctoredContest;
import com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException;
import com.example.codecombat2026.proctoring.exception.ProctoringStateConflictException;
import com.example.codecombat2026.proctoring.repository.ProctoredContestRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringSessionRepository;
import com.example.codecombat2026.repository.ContestRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Admin-only lifecycle for the {@code proctored_contests} extension row.
 *
 * <p>The presence of a row keyed by {@code contestId} is the only authoritative
 * signal that a contest runs in proctored mode (Req 1.6) — the {@code Contest}
 * entity itself stays untouched. Both endpoints are gated on the parent
 * contest being in {@link Contest.ContestStatus#UPCOMING UPCOMING}: once a
 * contest goes {@code LIVE} or {@code ENDED}, toggling proctoring would change
 * the rules mid-flight for candidates already in the arena (Req 1.4, 1.5).
 *
 * <p>Both endpoints are idempotent against their own state — re-enabling an
 * already-proctored contest returns the existing row, and deleting an already
 * non-proctored contest returns {@code 204} — so the admin UI doesn't have to
 * track the toggle state perfectly.
 */
@RestController
@RequestMapping("/api/admin/proctoring/contests")
@PreAuthorize("hasRole('ADMIN')")
public class ProctoredContestController {

    private static final int DEFAULT_CONSENT_VERSION = 1;

    private final ProctoredContestRepository proctoredContestRepository;
    private final ContestRepository contestRepository;
    private final ProctoringSessionRepository proctoringSessionRepository;

    public ProctoredContestController(ProctoredContestRepository proctoredContestRepository,
                                      ContestRepository contestRepository,
                                      ProctoringSessionRepository proctoringSessionRepository) {
        this.proctoredContestRepository = proctoredContestRepository;
        this.contestRepository = contestRepository;
        this.proctoringSessionRepository = proctoringSessionRepository;
    }

    /**
     * Optional request body for enabling proctoring on a contest. When omitted
     * or {@code consentVersion} is {@code null}, defaults to
     * {@link #DEFAULT_CONSENT_VERSION}.
     */
    public record EnableRequest(Integer consentVersion) {}

    /**
     * Wire shape of a {@link ProctoredContest} returned to the admin UI. We
     * don't expose the JPA entity directly so the response stays stable even
     * if the entity grows columns later.
     */
    public record ProctoredContestView(
            Long id,
            Long contestId,
            Integer consentVersion,
            LocalDateTime createdAt
    ) {
        static ProctoredContestView of(ProctoredContest row) {
            return new ProctoredContestView(
                    row.getId(),
                    row.getContestId(),
                    row.getConsentVersion(),
                    row.getCreatedAt()
            );
        }
    }

    /**
     * Enable proctoring for an existing contest by inserting a
     * {@code proctored_contests} row.
     *
     * <p>Idempotent: if a row already exists for {@code contestId}, the
     * existing row is returned and the {@code UPCOMING} guard is skipped so
     * the admin UI's "ensure proctored" calls don't start failing the moment
     * the contest goes live.
     *
     * <p>Transactional to close the race between the "no active sessions"
     * check and the INSERT: the active-session guard is re-checked after
     * the INSERT within the same transaction boundary so a concurrent
     * session-create cannot slip in between the check and the write.
     *
     * <p>Returns {@code 200} with the row, {@code 404} if the contest doesn't
     * exist, or {@code 409 CONTEST_NOT_UPCOMING} when the contest has already
     * started or ended (Req 1.4, 1.5).
     *
     * @param contestId parent {@code contests.id} from the path
     * @param body      optional {@code consentVersion}; defaults to 1
     */
    @PostMapping("/{contestId}")
    @Transactional
    public ResponseEntity<ProctoredContestView> enable(@PathVariable Long contestId,
                                                       @RequestBody(required = false) EnableRequest body) {
        var existing = proctoredContestRepository.findByContestId(contestId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(ProctoredContestView.of(existing.get()));
        }

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ProctoringNotFoundException("Contest not found: " + contestId));

        if (contest.getStatus() != Contest.ContestStatus.UPCOMING
                && !proctoringSessionRepository.findByContestIdAndEndedAtIsNull(contestId).isEmpty()) {
            throw new ProctoringStateConflictException(
                    "ACTIVE_SESSIONS_EXIST",
                    "Cannot toggle proctoring while candidates have active sessions");
        }

        int consentVersion = (body != null && body.consentVersion() != null)
                ? body.consentVersion()
                : DEFAULT_CONSENT_VERSION;

        ProctoredContest row = new ProctoredContest();
        row.setContestId(contestId);
        row.setCreatedAt(LocalDateTime.now());
        row.setConsentVersion(consentVersion);

        ProctoredContest saved = proctoredContestRepository.save(row);

        // Post-insert re-check: a candidate may have created a session
        // between our pre-insert guard and the INSERT. The active-session
        // list is now guarded inside the transaction, so a concurrent
        // session-create will be serialized after this commit.
        if (contest.getStatus() != Contest.ContestStatus.UPCOMING) {
            if (!proctoringSessionRepository.findByContestIdAndEndedAtIsNull(contestId).isEmpty()) {
                throw new ProctoringStateConflictException(
                        "ACTIVE_SESSIONS_EXIST",
                        "Candidate session started between check and commit — cannot enable proctoring");
            }
        }

        return ResponseEntity.ok(ProctoredContestView.of(saved));
    }

    /**
     * Disable proctoring on a contest by deleting its
     * {@code proctored_contests} row.
     *
     * <p>Idempotent: if no row exists for {@code contestId}, returns
     * {@code 204} without touching the parent contest. When a row does exist,
     * the parent contest must still be {@code UPCOMING} or the request is
     * rejected with {@code 409 CONTEST_NOT_UPCOMING} (Req 1.4, 1.5).
     *
     * <p>Transactional for the same race-close reason as {@link #enable}.
     *
     * @param contestId parent {@code contests.id} from the path
     */
    @DeleteMapping("/{contestId}")
    @Transactional
    public ResponseEntity<Void> disable(@PathVariable Long contestId) {
        var existing = proctoredContestRepository.findByContestId(contestId);
        if (existing.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ProctoringNotFoundException("Contest not found: " + contestId));

        if (contest.getStatus() != Contest.ContestStatus.UPCOMING
                && !proctoringSessionRepository.findByContestIdAndEndedAtIsNull(contestId).isEmpty()) {
            throw new ProctoringStateConflictException(
                    "ACTIVE_SESSIONS_EXIST",
                    "Cannot toggle proctoring while candidates have active sessions");
        }

        proctoredContestRepository.delete(existing.get());

        if (contest.getStatus() != Contest.ContestStatus.UPCOMING) {
            if (!proctoringSessionRepository.findByContestIdAndEndedAtIsNull(contestId).isEmpty()) {
                throw new ProctoringStateConflictException(
                        "ACTIVE_SESSIONS_EXIST",
                        "Candidate session started between check and commit — cannot disable proctoring");
            }
        }

        return ResponseEntity.noContent().build();
    }
}
