package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.TestCase;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TestCaseService {
    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private ProblemRepository problemRepository;

    public TestCase createTestCase(Long problemId, TestCase testCase) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));
        testCase.setProblem(problem);
        return testCaseRepository.save(testCase);
    }

    public List<TestCase> getTestCasesByProblemId(Long problemId) {
        return testCaseRepository.findByProblemId(problemId);
    }

    public TestCase getTestCaseById(Long id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found"));
    }

    public TestCase updateTestCase(Long id, TestCase testCaseDetails) {
        TestCase testCase = getTestCaseById(id);
        testCase.setInput(testCaseDetails.getInput());
        testCase.setExpectedOutput(testCaseDetails.getExpectedOutput());
        testCase.setHidden(testCaseDetails.isHidden());
        return testCaseRepository.save(testCase);
    }

    public void deleteTestCase(Long id) {
        TestCase testCase = getTestCaseById(id);
        testCaseRepository.delete(testCase);
    }
}
