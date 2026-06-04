package com.example.codecombat2026.proctoring.service;

import com.example.codecombat2026.proctoring.entity.ProctoredContest;
import com.example.codecombat2026.proctoring.entity.ProctoringConsentAck;
import com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException;
import com.example.codecombat2026.proctoring.repository.ProctoredContestRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringConsentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Records and queries candidate consent acknowledgments for proctored
 * contests (Req 2.3, 2.5, 2.6).
 *
 * <p>The unique constraint
 * {@code (user_id, contest_id, consent_version)} on
 * {@code proctoring_consent_acks} (V7) is the source of idempotency. This
 * service layers two extra rules on top of that:
 *
 * <ol>
 *   <li>The contest must actually be proctored — a missing
 *       {@link ProctoredContest} row yields 404 via
 *       {@link ProctoringNotFoundException}.</li>
 *   <li>The {@code consentVersion} sent by the client must match the
 *       version currently published on the proctored-contest row. A
 *       mismatch produces 400 so the client knows to refetch the latest
 *       consent text and prompt the candidate again.</li>
 * </ol>
 *
 * <p>Re-clicking "Accept" with a matching version returns the existing
 * ack row instead of attempting a duplicate insert, keeping the entry
 * flow safe against double-submit and offline replay.
 */
@Service
public class ProctoringConsentService {

    private final ProctoringConsentRepository consentRepo;
    private final ProctoredContestRepository proctoredContestRepo;

    public ProctoringConsentService(ProctoringConsentRepository consentRepo,
                                    ProctoredContestRepository proctoredContestRepo) {
        this.consentRepo = consentRepo;
        this.proctoredContestRepo = proctoredContestRepo;
    }

    /**
     * Record (or return the existing) consent acknowledgment for a
     * candidate.
     *
     * @param userId         candidate {@code users.id}
     * @param contestId      parent {@code contests.id}
     * @param consentVersion version the candidate clicked "Accept" against;
     *                       must match {@code proctored_contests.consent_version}
     * @param clientIp       resolved client IP (typically {@code X-Forwarded-For[0]});
     *                       may be {@code null}
     * @param userAgent      raw {@code User-Agent} header; may be {@code null}
     * @return the persisted (or pre-existing) ack row
     * @throws ProctoringNotFoundException if the contest is not proctored
     * @throws ResponseStatusException     400 if {@code consentVersion}
     *                                     does not match the current version
     */
    @Transactional
    public ProctoringConsentAck recordAck(Long userId,
                                          Long contestId,
                                          Integer consentVersion,
                                          String clientIp,
                                          String userAgent) {
        ProctoredContest proctored = proctoredContestRepo.findByContestId(contestId)
                .orElseThrow(() -> new ProctoringNotFoundException(
                        "Proctored contest not found: " + contestId));

        if (!Objects.equals(proctored.getConsentVersion(), consentVersion)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "consent version mismatch");
        }

        Optional<ProctoringConsentAck> existing =
                consentRepo.findByUserIdAndContestIdAndConsentVersion(userId, contestId, consentVersion);
        if (existing.isPresent()) {
            return existing.get();
        }

        ProctoringConsentAck ack = new ProctoringConsentAck();
        ack.setUserId(userId);
        ack.setContestId(contestId);
        ack.setConsentVersion(consentVersion);
        ack.setAcceptedAt(LocalDateTime.now());
        ack.setClientIp(clientIp);
        ack.setUserAgent(userAgent);
        return consentRepo.save(ack);
    }

    /**
     * Cheap existence check for the eligibility endpoint.
     *
     * @param userId         candidate {@code users.id}
     * @param contestId      parent {@code contests.id}
     * @param consentVersion version to check against
     * @return {@code true} iff the candidate has acknowledged this exact version
     */
    @Transactional(readOnly = true)
    public boolean hasAck(Long userId, Long contestId, Integer consentVersion) {
        return consentRepo.existsByUserIdAndContestIdAndConsentVersion(userId, contestId, consentVersion);
    }
}
