package com.example.codecombat2026.sqljudge.repository;

import com.example.codecombat2026.sqljudge.entity.SqlProblem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SqlProblemRepository extends JpaRepository<SqlProblem, Long> {

    List<SqlProblem> findByEnabledTrueOrderByCreatedAtDesc();

    List<SqlProblem> findByEnabledTrue();
    Page<SqlProblem> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying
    @Query("UPDATE SqlProblem p SET p.enabled = :enabled WHERE p.id = :id")
    void updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);
}
