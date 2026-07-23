package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.ProblemSolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProblemSolutionRepository extends JpaRepository<ProblemSolution, Long> {
    List<ProblemSolution> findByProblemIdOrderByCreatedAtDesc(Long problemId);
    long countByProblemId(Long problemId);
}
