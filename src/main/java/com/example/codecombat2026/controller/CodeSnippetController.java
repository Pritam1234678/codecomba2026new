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

    // ─── User-facing endpoints ────────────────────────────────────────────────
    // Returns only the starter code extracted from between USER_CODE_START/END markers.
    // solutionTemplate (full harness) is NEVER returned to users.

    @GetMapping
    public ResponseEntity<List<CodeSnippetDTO>> getAllSnippets(@PathVariable Long problemId) {
        return ResponseEntity.ok(snippetService.getSnippetsByProblemId(problemId));
    }

    @GetMapping("/{language}")
    public ResponseEntity<CodeSnippetDTO> getSnippet(
            @PathVariable Long problemId,
            @PathVariable String language) {
        return ResponseEntity.ok(snippetService.getSnippet(problemId, language));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────
    // Returns full harness (solutionTemplate) for editing.

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CodeSnippetDTO>> getAllSnippetsAdmin(@PathVariable Long problemId) {
        return ResponseEntity.ok(snippetService.getSnippetsByProblemIdAdmin(problemId));
    }

    @GetMapping("/admin/{language}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CodeSnippetDTO> getSnippetAdmin(
            @PathVariable Long problemId,
            @PathVariable String language) {
        return ResponseEntity.ok(snippetService.getSnippetAdmin(problemId, language));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CodeSnippet> saveSnippet(
            @PathVariable Long problemId,
            @RequestBody CodeSnippetDTO dto) {
        return ResponseEntity.ok(snippetService.saveSnippet(problemId, dto));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CodeSnippet>> saveAllSnippets(
            @PathVariable Long problemId,
            @RequestBody List<CodeSnippetDTO> dtos) {
        return ResponseEntity.ok(snippetService.saveAllSnippets(problemId, dtos));
    }

    @DeleteMapping("/{snippetId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSnippet(@PathVariable Long snippetId) {
        snippetService.deleteSnippet(snippetId);
        return ResponseEntity.ok(new MessageResponse("Snippet deleted successfully"));
    }

    @GetMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> validateSnippets(@PathVariable Long problemId) {
        boolean hasAll = snippetService.hasAllLanguages(problemId);
        if (hasAll) {
            return ResponseEntity.ok(new MessageResponse("All languages configured"));
        }
        List<String> missing = snippetService.getMissingLanguages(problemId);
        return ResponseEntity.badRequest()
                .body(new MessageResponse("Missing languages: " + String.join(", ", missing)));
    }
}
