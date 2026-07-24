package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.PracticeSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PracticeSheetRepository extends JpaRepository<PracticeSheet, Long> {
    List<PracticeSheet> findByActiveTrueOrderByCreatedAtDesc();
    List<PracticeSheet> findAllByOrderByCreatedAtDesc();
}
