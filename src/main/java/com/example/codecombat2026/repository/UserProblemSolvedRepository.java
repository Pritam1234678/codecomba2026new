package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.UserProblemSolved;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProblemSolvedRepository extends JpaRepository<UserProblemSolved, Long> {

    Optional<UserProblemSolved> findByUserIdAndProblemId(Long userId, Long problemId);

    List<UserProblemSolved> findByUserId(Long userId);

    boolean existsByUserIdAndProblemId(Long userId, Long problemId);

    long countByUserId(Long userId);
}
