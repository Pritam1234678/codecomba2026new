package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.WebContestJob;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.entity.WebContestTemplate;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.repository.WebContestTemplateRepository;
import com.example.codecombat2026.util.TimeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebContestService {

    private static final Logger log = LoggerFactory.getLogger(WebContestService.class);

    public static final String QUEUE_KEY = "web-contest:queue";

    @Autowired private WebContestTemplateRepository templateRepository;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    /**
     * Load template metadata + visible file contents for the editor.
     * NEVER returns hidden files.
     */
    public Map<String, Object> getTemplate(Long problemId, String language) {
        WebContestTemplate template = templateRepository
            .findByProblemIdAndLanguage(problemId, language.toUpperCase())
            .orElseThrow(() -> new ResourceNotFoundException(
                "No web contest template found for problem " + problemId + " language " + language));

        Map<String, Object> manifest = parseManifest(template.getManifestJson());
        String templatePath = template.getTemplatePath();

        // Load editable file contents
        @SuppressWarnings("unchecked")
        List<String> editableFileNames = (List<String>) manifest.get("editableFiles");
        @SuppressWarnings("unchecked")
        List<String> readonlyFileNames = (List<String>) manifest.get("readonlyFiles");

        Map<String, String> editableFiles = new HashMap<>();
        Map<String, String> readonlyFiles = new HashMap<>();

        if (editableFileNames != null) {
            for (String fileName : editableFileNames) {
                String content = readFileFromTemplate(templatePath, fileName);
                editableFiles.put(fileName, content);
            }
        }

        if (readonlyFileNames != null) {
            for (String fileName : readonlyFileNames) {
                String content = readFileFromTemplate(templatePath, fileName);
                readonlyFiles.put(fileName, content);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("editableFiles", editableFiles);
        response.put("readonlyFiles", readonlyFiles);
        response.put("manifest", Map.of(
            "testCount", template.getTestCount(),
            "language", template.getLanguage(),
            "description", manifest.getOrDefault("description", "")
        ));

        return response;
    }

    /**
     * Queue a test run (no leaderboard update).
     */
    @Transactional
    public Submission runCode(Long userId, Long problemId,
                              Map<String, String> editableFiles, String language) {
        return queueJob(userId, problemId, editableFiles, language, true);
    }

    /**
     * Queue a real submit (updates leaderboard, counts against submit cap).
     */
    @Transactional
    public Submission submitCode(Long userId, Long problemId,
                                 Map<String, String> editableFiles, String language) {
        return queueJob(userId, problemId, editableFiles, language, false);
    }

    private Submission queueJob(Long userId, Long problemId,
                                Map<String, String> editableFiles, String language,
                                boolean isTestRun) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));
        WebContestTemplate template = templateRepository
            .findByProblemIdAndLanguage(problemId, language.toUpperCase())
            .orElseThrow(() -> new ResourceNotFoundException(
                "No web contest template for problem " + problemId));

        // Create a submission row (reuse existing Submission entity)
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setContest(problem.getContest());
        submission.setCode(editableFiles.toString()); // store a summary of files
        submission.setLanguage(Submission.ProgrammingLanguage.valueOf(language.toUpperCase()));
        submission.setStatus(Submission.SubmissionStatus.PENDING);
        submission.setSubmittedAt(TimeUtil.now());
        submission.setUserName(user.getUsername());
        submission.setProblemName(problem.getTitle());
        submission.setTestRun(isTestRun);
        submission = submissionRepository.save(submission);

        Long contestId = problem.getContest() != null ? problem.getContest().getId() : null;

        WebContestJob job = new WebContestJob(
            submission.getId(),
            userId,
            problemId,
            contestId,
            editableFiles,
            language.toUpperCase(),
            template.getTemplatePath(),
            (double) template.getTimeoutSeconds(),
            template.getMemoryMb(),
            isTestRun
        );

        try {
            String jobJson = objectMapper.writeValueAsString(job);
            redis.opsForList().leftPush(QUEUE_KEY, jobJson);
            log.debug("Web contest {} queued: submission={} user={} problem={}",
                isTestRun ? "run" : "submit", submission.getId(), userId, problemId);
        } catch (Exception e) {
            log.error("Failed to queue web contest job: {}", e.getMessage());
            submission.setStatus(Submission.SubmissionStatus.RE);
            submission.setErrorMessage("Failed to queue. Please try again.");
            submissionRepository.save(submission);
        }

        return submission;
    }

    private String readFileFromTemplate(String templatePath, String relativePath) {
        try {
            Path filePath = Paths.get(templatePath, relativePath);
            if (Files.exists(filePath)) {
                return Files.readString(filePath);
            }
            return "// File not found: " + relativePath;
        } catch (IOException e) {
            log.warn("Failed to read template file {}/{}: {}", templatePath, relativePath, e.getMessage());
            return "// Error reading file: " + relativePath;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseManifest(String manifestJson) {
        try {
            return objectMapper.readValue(manifestJson, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse manifest JSON: {}", e.getMessage());
            return Map.of();
        }
    }
}
