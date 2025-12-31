package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.CodeSnippet;
import com.example.codecombat2026.entity.CodeSnippet.ProgrammingLanguage;
import com.example.codecombat2026.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeSnippetRepository extends JpaRepository<CodeSnippet, Long> {

    /**
     * Find all snippets for a specific problem
     */
    List<CodeSnippet> findByProblem(Problem problem);

    /**
     * Find all snippets for a problem by ID
     */
    List<CodeSnippet> findByProblemId(Long problemId);

    /**
     * Find snippet for specific problem and language
     */
    Optional<CodeSnippet> findByProblemAndLanguage(Problem problem, ProgrammingLanguage language);

    /**
     * Find snippet by problem ID and language
     */
    Optional<CodeSnippet> findByProblemIdAndLanguage(Long problemId, ProgrammingLanguage language);

    /**
     * Check if snippet exists for problem and language
     */
    boolean existsByProblemIdAndLanguage(Long problemId, ProgrammingLanguage language);

    /**
     * Delete all snippets for a problem
     */
    void deleteByProblemId(Long problemId);

    /**
     * Count snippets for a problem
     */
    long countByProblemId(Long problemId);
}
