package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.DuelSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link DuelSubmission} link rows. The primary key
 * mirrors {@code submissions.id}; one row in {@code duel_submissions} per
 * duel-tagged submission.
 *
 * <p>{@link #markFirstAc(Long)} is the only writer of {@code is_first_ac}
 * and is invoked from {@code DuelService.onDuelVerdict} after the
 * conditional UPDATE on {@code duel_matches.winner_user_id IS NULL}
 * succeeds — keeping the application as the sole flipper of that flag.
 */
@Repository
public interface DuelSubmissionRepository extends JpaRepository<DuelSubmission, Long> {

    /**
     * Returns every duel-submission link for the given match, in no
     * particular order. Used by admin views and verdict replay.
     */
    List<DuelSubmission> findByMatchId(UUID matchId);

    /**
     * Returns the link row marked as the first-AC submission for the
     * match, if one has been recorded. Backed by the application-level
     * invariant that at most one row per match has {@code is_first_ac=TRUE}.
     */
    Optional<DuelSubmission> findFirstByMatchIdAndFirstAcTrue(UUID matchId);

    /**
     * Flip {@code is_first_ac} to {@code TRUE} for the given submission.
     * Returns the number of rows updated; callers should treat
     * {@code 0} as "row not found" (e.g. submission was rolled back).
     *
     * <p>Transaction management is the caller's responsibility — this
     * method intentionally omits {@code @Transactional} so it joins the
     * surrounding {@code DuelService} transaction that already holds the
     * winner-claim UPDATE.
     */
    @Modifying
    @Query("UPDATE DuelSubmission d SET d.firstAc = true WHERE d.submissionId = :submissionId")
    int markFirstAc(@Param("submissionId") Long submissionId);
}
