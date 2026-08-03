package com.example.codecombat2026.sqljudge.repository;

import com.example.codecombat2026.sqljudge.entity.SqlSubmission;
import com.example.codecombat2026.sqljudge.entity.SqlSubmission.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SqlSubmissionRepository extends JpaRepository<SqlSubmission, Long> {

    Page<SqlSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId, Pageable pageable);

    List<SqlSubmission> findTop10ByUserIdOrderBySubmittedAtDesc(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("UPDATE SqlSubmission s SET s.status = :status WHERE s.id = :id AND s.status IN :inflight")
    int updateStatus(@Param("id") Long id,
                     @Param("inflight") List<Status> inflight,
                     @Param("status") Status status);

    @Modifying
    @Query("UPDATE SqlSubmission s SET s.status = :status, s.executionTimeMs = :timeMs, " +
           "s.selectedNode = :node, s.errorMessage = :err, " +
           "s.completedAt = CURRENT_TIMESTAMP WHERE s.id = :id AND s.status IN :inflight")
    int updateFinalized(@Param("id") Long id,
                        @Param("inflight") List<Status> inflight,
                        @Param("status") Status status,
                        @Param("timeMs") Long timeMs,
                        @Param("node") String node,
                        @Param("err") String err);

    @Modifying
    @Query("UPDATE SqlSubmission s SET s.resultPreview = :preview WHERE s.id = :id")
    int updateFinalizedPreview(@Param("id") Long id, @Param("preview") String preview);
}
