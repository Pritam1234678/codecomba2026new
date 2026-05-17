package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.Contest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContestRepository extends JpaRepository<Contest, Long> {
    List<Contest> findByStatus(Contest.ContestStatus status);

    List<Contest> findByActiveTrue();

    Long countByActive(Boolean active);
}
