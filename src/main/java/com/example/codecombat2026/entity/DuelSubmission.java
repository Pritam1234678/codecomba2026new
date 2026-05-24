package com.example.codecombat2026.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Links a {@link Submission} row to a {@link DuelMatch}. The primary key
 * mirrors {@code submissions.id} (no auto-generation) — one row in
 * {@code duel_submissions} per duel-tagged submission. The FK from
 * {@code submission_id} to {@code submissions(id)} is declared at the
 * database layer (V3 migration) with {@code ON DELETE CASCADE}.
 *
 * <p>Kept as flat columns rather than a {@code @OneToOne} association to
 * {@link Submission} to avoid bidirectional cycles and to keep the entity
 * lightweight on the duel verdict path.
 *
 * <p>{@code is_first_ac} is flipped to {@code TRUE} only on the winning
 * submission by {@code DuelService.onDuelVerdict} after the conditional
 * UPDATE on {@code duel_matches.winner_user_id IS NULL} succeeds — the
 * application is the sole writer of this column.
 */
@Entity
@Table(name = "duel_submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuelSubmission {

    /**
     * Mirrors {@code submissions.id}. Not {@code @GeneratedValue} — the
     * caller supplies the same id used to insert the {@code submissions} row.
     */
    @Id
    @Column(name = "submission_id")
    private Long submissionId;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "is_first_ac", nullable = false)
    private boolean firstAc;
}
