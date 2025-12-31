package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.CodeSnippetDTO;
import com.example.codecombat2026.entity.CodeSnippet;
import com.example.codecombat2026.entity.CodeSnippet.ProgrammingLanguage;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.repository.CodeSnippetRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodeSnippetService {

    @Autowired
    private CodeSnippetRepository snippetRepository;

    @Autowired
    private ProblemRepository problemRepository;

    /**
     * Get all snippets for a problem
     */
    public List<CodeSnippetDTO> getSnippetsByProblemId(Long problemId) {
        List<CodeSnippet> snippets = snippetRepository.findByProblemId(problemId);
        return snippets.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get snippet for specific language
     */
    public CodeSnippetDTO getSnippet(Long problemId, String languageStr) {
        ProgrammingLanguage language = ProgrammingLanguage.valueOf(languageStr.toUpperCase());
        CodeSnippet snippet = snippetRepository
                .findByProblemIdAndLanguage(problemId, language)
                .orElseThrow(() -> new RuntimeException("Snippet not found for language: " + languageStr));

        return convertToDTO(snippet);
    }

    /**
     * Save or update a snippet
     */
    @Transactional
    public CodeSnippet saveSnippet(Long problemId, CodeSnippetDTO dto) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        ProgrammingLanguage language = ProgrammingLanguage.valueOf(dto.getLanguage().toUpperCase());

        // Check if snippet already exists
        CodeSnippet snippet = snippetRepository
                .findByProblemIdAndLanguage(problemId, language)
                .orElse(new CodeSnippet());

        snippet.setProblem(problem);
        snippet.setLanguage(language);
        snippet.setStarterCode(dto.getStarterCode());
        snippet.setSolutionTemplate(dto.getSolutionTemplate());

        return snippetRepository.save(snippet);
    }

    /**
     * Bulk save snippets for a problem
     */
    @Transactional
    public List<CodeSnippet> saveAllSnippets(Long problemId, List<CodeSnippetDTO> dtos) {
        List<CodeSnippet> savedSnippets = new ArrayList<>();

        for (CodeSnippetDTO dto : dtos) {
            savedSnippets.add(saveSnippet(problemId, dto));
        }

        return savedSnippets;
    }

    /**
     * Delete snippet
     */
    @Transactional
    public void deleteSnippet(Long snippetId) {
        snippetRepository.deleteById(snippetId);
    }

    /**
     * Check if all 5 languages have snippets
     */
    public boolean hasAllLanguages(Long problemId) {
        long count = snippetRepository.countByProblemId(problemId);
        return count == 5; // Must have all 5 languages
    }

    /**
     * Get missing languages for a problem
     */
    public List<String> getMissingLanguages(Long problemId) {
        List<CodeSnippet> existing = snippetRepository.findByProblemId(problemId);
        List<ProgrammingLanguage> existingLangs = existing.stream()
                .map(CodeSnippet::getLanguage)
                .collect(Collectors.toList());

        List<String> missing = new ArrayList<>();
        for (ProgrammingLanguage lang : ProgrammingLanguage.values()) {
            if (!existingLangs.contains(lang)) {
                missing.add(lang.name());
            }
        }

        return missing;
    }

    /**
     * Convert entity to DTO
     */
    private CodeSnippetDTO convertToDTO(CodeSnippet snippet) {
        CodeSnippetDTO dto = new CodeSnippetDTO();
        dto.setId(snippet.getId());
        dto.setProblemId(snippet.getProblem().getId());
        dto.setLanguage(snippet.getLanguage().name());
        dto.setStarterCode(snippet.getStarterCode());
        dto.setSolutionTemplate(snippet.getSolutionTemplate());
        return dto;
    }
}
