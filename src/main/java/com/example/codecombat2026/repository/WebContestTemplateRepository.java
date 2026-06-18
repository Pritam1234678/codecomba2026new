package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.WebContestTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebContestTemplateRepository extends JpaRepository<WebContestTemplate, Long> {

    Optional<WebContestTemplate> findByProblemIdAndLanguage(Long problemId, String language);
}
