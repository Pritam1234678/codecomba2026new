package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.dto.TestCaseDTO;
import com.example.codecombat2026.entity.TestCase;
import com.example.codecombat2026.service.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTestCaseController {

    @Autowired
    private TestCaseService testCaseService;

    @PostMapping("/problems/{problemId}/testcases")
    public ResponseEntity<TestCaseDTO> createTestCase(@PathVariable Long problemId, @RequestBody TestCase testCase) {
        TestCase created = testCaseService.createTestCase(problemId, testCase);
        return ResponseEntity.ok(mapToDTO(created));
    }

    @GetMapping("/problems/{problemId}/testcases")
    public ResponseEntity<List<TestCaseDTO>> getTestCasesByProblem(@PathVariable Long problemId) {
        List<TestCase> testCases = testCaseService.getTestCasesByProblemId(problemId);
        List<TestCaseDTO> dtos = testCases.stream().map(this::mapToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/testcases/{id}")
    public ResponseEntity<TestCaseDTO> updateTestCase(@PathVariable Long id, @RequestBody TestCase testCase) {
        TestCase updated = testCaseService.updateTestCase(id, testCase);
        return ResponseEntity.ok(mapToDTO(updated));
    }

    @DeleteMapping("/testcases/{id}")
    public ResponseEntity<?> deleteTestCase(@PathVariable Long id) {
        testCaseService.deleteTestCase(id);
        return ResponseEntity.ok(new MessageResponse("Test case deleted successfully"));
    }

    private TestCaseDTO mapToDTO(TestCase tc) {
        return new TestCaseDTO(tc.getId(), tc.getInput(), tc.getExpectedOutput(),
                tc.isHidden(), tc.getProblem() != null ? tc.getProblem().getId() : null);
    }
}
