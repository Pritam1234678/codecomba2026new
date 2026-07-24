package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.SheetProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface SheetProblemRepository extends JpaRepository<SheetProblem, Long> {
    List<SheetProblem> findBySheetIdOrderBySortOrder(Long sheetId);
    List<SheetProblem> findBySheetIdAndProblemId(Long sheetId, Long problemId);
    @Transactional
    void deleteBySheetIdAndProblemId(Long sheetId, Long problemId);
    @Transactional
    void deleteBySheetId(Long sheetId);
}
