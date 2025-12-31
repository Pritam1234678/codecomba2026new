package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.ExecutionResult;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.TestCase;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.service.judge.DockerJudgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SubmissionService {
    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DockerJudgeService dockerJudgeService;

    @Autowired
    private com.example.codecombat2026.repository.CodeSnippetRepository snippetRepository;

    public Submission submitCode(Long userId, Long problemId, String code, Submission.ProgrammingLanguage language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        Submission submission = submissionRepository.findByUser_IdAndProblem_Id(userId, problemId);

        if (submission == null) {
            submission = new Submission();
            submission.setUser(user);
            submission.setProblem(problem);
            submission.setContest(problem.getContest()); // Set contest from problem
        }

        submission.setCode(code);
        submission.setLanguage(language);
        submission.setStatus(Submission.SubmissionStatus.JUDGING);

        // Save initial state
        submission = submissionRepository.save(submission);

        // Run judge (sync)
        judgeSubmission(submission);

        // Save result
        submission = submissionRepository.save(submission);

        // Populate transient fields for response
        submission.setUserName(user.getUsername());
        submission.setUserRoll(user.getRollNumber());
        submission.setProblemName(problem.getTitle());

        return submission;
    }

    public Submission getSubmission(Long userId, Long problemId) {
        // Method to fetch existing submission
        return submissionRepository.findByUser_IdAndProblem_Id(userId, problemId);
    }

    public List<Submission> getUserSubmissions(Long userId) {
        return submissionRepository.findByUser_Id(userId);
    }

    public Submission testCode(Long userId, Long problemId, String code, Submission.ProgrammingLanguage language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        // Create a temporary submission object (not saved to database)
        Submission testSubmission = new Submission();
        testSubmission.setUser(user);
        testSubmission.setProblem(problem);
        testSubmission.setCode(code);
        testSubmission.setLanguage(language);
        testSubmission.setStatus(Submission.SubmissionStatus.JUDGING);

        // Run the judge without saving
        judgeSubmission(testSubmission);

        // Populate transient fields for response
        testSubmission.setUserName(user.getUsername());
        testSubmission.setUserRoll(user.getRollNumber());
        testSubmission.setProblemName(problem.getTitle());

        // Return the result without saving to database
        return testSubmission;
    }

    private void judgeSubmission(Submission submission) {
        Problem problem = submission.getProblem();
        List<TestCase> testCases = problem.getTestCases();

        // Get merged code (user code + template)
        String executableCode = mergeCodeWithTemplate(
                submission.getCode(),
                submission.getLanguage(),
                problem);

        int passed = 0;
        double maxTime = 0.0;

        // If no test cases, auto-accept (or handle as error, but let's assume AC)
        if (testCases == null || testCases.isEmpty()) {
            submission.setStatus(Submission.SubmissionStatus.AC);
            submission.setTestCasesPassed(0);
            submission.setTotalTestCases(0);
            submission.setTimeConsumed(0.0);
            return;
        }

        // Track test case results
        StringBuilder testCaseDetailsBuilder = new StringBuilder("[");
        int testCaseNumber = 0;

        for (TestCase testCase : testCases) {
            testCaseNumber++;
            ExecutionResult result = dockerJudgeService.execute(
                    executableCode, // Use merged code instead of submission.getCode()
                    submission.getLanguage(),
                    testCase.getInput(),
                    problem.getTimeLimit());

            // Track time
            maxTime = Math.max(maxTime, result.getTimeTaken());

            // Check for compilation error
            if (result.isCompilationError()) {
                submission.setStatus(Submission.SubmissionStatus.CE);
                submission.setErrorMessage(result.getStderr());
                submission.setTestCasesPassed(0);
                submission.setTotalTestCases(testCases.size());
                submission.setTimeConsumed(maxTime);
                submission.setScore(0); // CE gets 0 score
                submission.setTestCaseDetails("[]"); // No test case details for CE
                return;
            }

            // Check for runtime error
            if (result.getExitCode() != 0) {
                // Add failed test case detail
                if (testCaseDetailsBuilder.length() > 1)
                    testCaseDetailsBuilder.append(",");
                testCaseDetailsBuilder.append(String.format(
                        "{\"testCase\":%d,\"status\":\"RE\",\"hidden\":%b}",
                        testCaseNumber, testCase.isHidden()));

                // Track error but continue to run remaining test cases
                if (submission.getStatus() == null || submission.getStatus() == Submission.SubmissionStatus.PENDING) {
                    submission.setStatus(Submission.SubmissionStatus.RE);
                    submission.setErrorMessage(result.getStderr());
                }
                continue; // Continue to next test case instead of returning
            }

            // Smart output comparison - remove only trailing newlines, preserve spaces
            String output = result.getStdout() != null ? result.getStdout().replaceAll("\\r?\\n+$", "") : "";
            String expected = testCase.getExpectedOutput() != null
                    ? testCase.getExpectedOutput().replaceAll("\\r?\\n+$", "")
                    : "";

            if (!output.equals(expected)) {
                // Wrong Answer - add failed test case detail
                if (testCaseDetailsBuilder.length() > 1)
                    testCaseDetailsBuilder.append(",");
                testCaseDetailsBuilder.append(String.format(
                        "{\"testCase\":%d,\"status\":\"FAIL\",\"hidden\":%b}",
                        testCaseNumber, testCase.isHidden()));

                // Track WA but continue to run remaining test cases
                if (submission.getStatus() == null || submission.getStatus() == Submission.SubmissionStatus.PENDING) {
                    submission.setStatus(Submission.SubmissionStatus.WA);
                }
                continue; // Continue to next test case instead of returning
            }

            // Test case passed
            if (testCaseDetailsBuilder.length() > 1)
                testCaseDetailsBuilder.append(",");
            testCaseDetailsBuilder.append(String.format(
                    "{\"testCase\":%d,\"status\":\"PASS\",\"hidden\":%b}",
                    testCaseNumber, testCase.isHidden()));
            passed++;
        }

        testCaseDetailsBuilder.append("]");

        if (passed == testCases.size()) {
            submission.setStatus(Submission.SubmissionStatus.AC);
        } else {
            // Should be caught by WA, but fallback
            submission.setStatus(Submission.SubmissionStatus.WA);
        }

        submission.setTestCasesPassed(passed);
        submission.setTotalTestCases(testCases.size());
        submission.setTimeConsumed(maxTime);
        submission.setTestCaseDetails(testCaseDetailsBuilder.toString());

        // Calculate final score
        int score = testCases.size() > 0 ? (int) Math.round((passed * 100.0) / testCases.size()) : 0;
        submission.setScore(score);
    }

    /**
     * Merge user code with solution template
     * Replaces USER_CODE_PLACEHOLDER with actual user code
     */
    private String mergeCodeWithTemplate(String userCode, Submission.ProgrammingLanguage language, Problem problem) {
        // Convert Submission.ProgrammingLanguage to CodeSnippet.ProgrammingLanguage
        com.example.codecombat2026.entity.CodeSnippet.ProgrammingLanguage snippetLang;

        switch (language) {
            case JAVA:
                snippetLang = com.example.codecombat2026.entity.CodeSnippet.ProgrammingLanguage.JAVA;
                break;
            case CPP:
                snippetLang = com.example.codecombat2026.entity.CodeSnippet.ProgrammingLanguage.CPP;
                break;
            case PYTHON:
                snippetLang = com.example.codecombat2026.entity.CodeSnippet.ProgrammingLanguage.PYTHON;
                break;
            case JAVASCRIPT:
                snippetLang = com.example.codecombat2026.entity.CodeSnippet.ProgrammingLanguage.JAVASCRIPT;
                break;
            case C:
                snippetLang = com.example.codecombat2026.entity.CodeSnippet.ProgrammingLanguage.C;
                break;
            default:
                // If no snippet found, return user code as-is (backward compatibility)
                return userCode;
        }

        // Try to get snippet
        return snippetRepository.findByProblemAndLanguage(problem, snippetLang)
                .map(snippet -> {
                    // Replace placeholder with user code
                    String template = snippet.getSolutionTemplate();
                    return template.replace("// USER_CODE_PLACEHOLDER", userCode)
                            .replace("# USER_CODE_PLACEHOLDER", userCode)
                            .replace("/* USER_CODE_PLACEHOLDER */", userCode);
                })
                .orElse(userCode); // Fallback to user code if no snippet
    }
}
