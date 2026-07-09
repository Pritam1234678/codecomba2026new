package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.ContestProblem;
import com.example.codecombat2026.entity.ContestProblemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContestProblemRepository extends JpaRepository<ContestProblem, ContestProblemId> {

    List<ContestProblem> findByContestIdOrderByDisplayOrderAscAddedAtAsc(Long contestId);

    List<ContestProblem> findByProblemId(Long problemId);

    boolean existsByContestIdAndProblemId(Long contestId, Long problemId);

    @Modifying
    @Query("DELETE FROM ContestProblem cp WHERE cp.contestId = :cid AND cp.problemId = :pid")
    int deleteByContestIdAndProblemId(@Param("cid") Long cid, @Param("pid") Long pid);

    long countByContestId(Long contestId);

    @Query("SELECT COALESCE(MAX(cp.displayOrder), 0) FROM ContestProblem cp WHERE cp.contestId = :contestId")
    Integer findMaxDisplayOrderByContestId(@Param("contestId") Long contestId);
}
