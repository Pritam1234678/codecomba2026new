package com.example.codecombat2026.proctoring.repository;

import com.example.codecombat2026.proctoring.entity.ProctoringConsentAck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ProctoringConsentAck}.
 *
 * <p>The unique constraint
 * {@code (user_id, contest_id, consent_version)} on V7 makes the consent
 * flow idempotent. {@link #existsByUserIdAndContestIdAndConsentVersion(Long, Long, Integer)}
 * is the one query callers actually need: the preflight gate uses it to
 * skip re-prompting a candidate who already accepted the current
 * version (Req 2.6), and the consent endpoint uses it to short-circuit
 * before attempting an insert that would otherwise trip the unique
 * constraint.
 */
@Repository
public interface ProctoringConsentRepository extends JpaRepository<ProctoringConsentAck, Long> {

    /**
     * True iff the candidate has already acknowledged the given
     * {@code consentVersion} for this contest. Drives the
     * "skip-on-existing" branch of the consent flow (Req 2.6).
     *
     * @param userId          candidate {@code users.id}
     * @param contestId       parent {@code contests.id}
     * @param consentVersion  current version from {@code proctored_contests.consent_version}
     * @return {@code true} iff a matching ack row already exists
     */
    boolean existsByUserIdAndContestIdAndConsentVersion(Long userId, Long contestId, Integer consentVersion);

    /**
     * Look up an existing acknowledgment row. Used by
     * {@code ProctoringConsentService.recordAck} to return the existing ack
     * when a candidate replays the consent click — keeping the operation
     * idempotent without tripping the {@code (user_id, contest_id, consent_version)}
     * unique constraint.
     *
     * @param userId          candidate {@code users.id}
     * @param contestId       parent {@code contests.id}
     * @param consentVersion  current version from {@code proctored_contests.consent_version}
     * @return the ack row if one already exists, otherwise empty
     */
    Optional<ProctoringConsentAck> findByUserIdAndContestIdAndConsentVersion(
            Long userId, Long contestId, Integer consentVersion);
}
