package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.ContestRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContestRegistrationRepository extends JpaRepository<ContestRegistration, Long> {

    boolean existsByContestIdAndUserId(Long contestId, Long userId);

    Optional<ContestRegistration> findByContestIdAndUserId(Long contestId, Long userId);

    List<ContestRegistration> findByContestId(Long contestId);

    long countByContestId(Long contestId);

    long countByUserId(Long userId);
}
