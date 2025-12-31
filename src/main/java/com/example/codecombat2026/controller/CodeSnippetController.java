package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.CodeSnippetDTO;
import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.entity.CodeSnippet;
import com.example.codecombat2026.service.CodeSnippetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems/{problemId}/snippets")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CodeSnippetController {

    @Autowired
    private CodeSnippetService snippetService;

    /**
     * Get all snippets for a problem
     * Public endpoint - users need this to load code editor
     */
    @GetMapping
    public ResponseEntity<List<CodeSnippetDTO>> getAllSnippets(@PathVariable Long problemId) {
        List<CodeSnippetDTO> snippets = snippetService.getSnippetsByProblemId(problemId);
        return ResponseEntity.ok(snippets);
    }

    /**
     * Get snippet for specific language
     * Public endpoint
     */
    @GetMapping("/{language}")
    public ResponseEntity<CodeSnippetDTO> getSnippet(
            @PathVariable Long problemId,
            @PathVariable String language) {
        CodeSnippetDTO snippet = snippetService.getSnippet(problemId, language);
        return ResponseEntity.ok(snippet);
    }

    /**
     * Create or update a snippet
     * Admin only
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CodeSnippet> saveSnippet(
            @PathVariable Long problemId,
            @RequestBody CodeSnippetDTO dto) {
        CodeSnippet snippet = snippetService.saveSnippet(problemId, dto);
        return ResponseEntity.ok(snippet);
    }

    /**
     * Bulk create/update snippets
     * Admin only
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CodeSnippet>> saveAllSnippets(
            @PathVariable Long problemId,
            @RequestBody List<CodeSnippetDTO> dtos) {
        List<CodeSnippet> snippets = snippetService.saveAllSnippets(problemId, dtos);
        return ResponseEntity.ok(snippets);
    }

    /**
     * Delete a snippet
     * Admin only
     */
    @DeleteMapping("/{snippetId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSnippet(@PathVariable Long snippetId) {
        snippetService.deleteSnippet(snippetId);
        return ResponseEntity.ok(new MessageResponse("Snippet deleted successfully"));
    }

    /**
     * Check if problem has all required snippets
     * Admin only
     */
    @GetMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> validateSnippets(@PathVariable Long problemId) {
        boolean hasAll = snippetService.hasAllLanguages(problemId);

        if (hasAll) {
            return ResponseEntity.ok(new MessageResponse("All languages configured"));
        } else {
            List<String> missing = snippetService.getMissingLanguages(problemId);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Missing languages: " + String.join(", ", missing)));
        }
    }
}
