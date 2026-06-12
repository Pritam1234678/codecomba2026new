package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.CodeSnippetDTO;
import com.example.codecombat2026.entity.CodeSnippet;
import com.example.codecombat2026.entity.CodeSnippet.ProgrammingLanguage;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.repository.CodeSnippetRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodeSnippetService {

    public static final String START_MARKER    = "// USER_CODE_START";
    public static final String END_MARKER      = "// USER_CODE_END";
    public static final String START_MARKER_PY = "# USER_CODE_START";
    public static final String END_MARKER_PY   = "# USER_CODE_END";

    // Cache key: snippets:user:{problemId}  — list of all user-facing snippets
    private static final String SNIPPETS_KEY_PREFIX = "snippets:user:";
    private static final Duration SNIPPETS_TTL = Duration.ofMinutes(10);

    @Autowired private CodeSnippetRepository snippetRepository;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    // ─── User-facing (cached) ─────────────────────────────────────────────────

    /**
     * Returns all snippets for a problem with only starter code (no harness).
     * Cached in Valkey for 10 minutes — called on every ProblemSolve page load.
     */
    public List<CodeSnippetDTO> getSnippetsByProblemId(Long problemId) {
        String key = SNIPPETS_KEY_PREFIX + problemId;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<CodeSnippetDTO>>() {});
            }
        } catch (Exception ignored) {}

        List<CodeSnippetDTO> result = snippetRepository.findByProblemId(problemId).stream()
                .map(this::toUserDTO)
                .collect(Collectors.toList());

        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(result), SNIPPETS_TTL);
        } catch (Exception ignored) {}

        return result;
    }

    /**
     * Returns a single snippet for a specific language.
     * Uses the cached list to avoid extra DB call.
     */
    public CodeSnippetDTO getSnippet(Long problemId, String languageStr) {
        ProgrammingLanguage language = ProgrammingLanguage.valueOf(languageStr.toUpperCase());

        // Try to get from cached list first
        List<CodeSnippetDTO> all = getSnippetsByProblemId(problemId);
        return all.stream()
                .filter(s -> s.getLanguage().equalsIgnoreCase(languageStr))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Snippet not found for language: " + languageStr));
    }

    // ─── Admin (not cached — always fresh) ───────────────────────────────────

    public List<CodeSnippetDTO> getSnippetsByProblemIdAdmin(Long problemId) {
        return snippetRepository.findByProblemId(problemId).stream()
                .map(this::toAdminDTO)
                .collect(Collectors.toList());
    }

    public CodeSnippetDTO getSnippetAdmin(Long problemId, String languageStr) {
        ProgrammingLanguage language = ProgrammingLanguage.valueOf(languageStr.toUpperCase());
        CodeSnippet snippet = snippetRepository
                .findByProblemIdAndLanguage(problemId, language)
                .orElseThrow(() -> new RuntimeException("Snippet not found for language: " + languageStr));
        return toAdminDTO(snippet);
    }

    @Transactional
    public CodeSnippet saveSnippet(Long problemId, CodeSnippetDTO dto) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        ProgrammingLanguage language = ProgrammingLanguage.valueOf(dto.getLanguage().toUpperCase());

        CodeSnippet snippet = snippetRepository
                .findByProblemIdAndLanguage(problemId, language)
                .orElse(new CodeSnippet());

        snippet.setProblem(problem);
        snippet.setLanguage(language);
        snippet.setSolutionTemplate(dto.getSolutionTemplate());

        CodeSnippet saved = snippetRepository.save(snippet);

        // Evict user-facing snippet cache and harness cache
        evictSnippetCache(problemId, language.name());

        return saved;
    }

    @Transactional
    public List<CodeSnippet> saveAllSnippets(Long problemId, List<CodeSnippetDTO> dtos) {
        List<CodeSnippet> saved = new ArrayList<>();
        for (CodeSnippetDTO dto : dtos) {
            saved.add(saveSnippet(problemId, dto));
        }
        // Evict the combined list cache after bulk save
        evictAllSnippetCaches(problemId);
        return saved;
    }

    @Transactional
    public void deleteSnippet(Long snippetId) {
        snippetRepository.findById(snippetId).ifPresent(s -> {
            Long problemId = s.getProblem().getId();
            snippetRepository.deleteById(snippetId);
            evictAllSnippetCaches(problemId);
        });
    }

    // ─── Cache eviction ───────────────────────────────────────────────────────

    public void evictSnippetCache(Long problemId, String language) {
        try {
            // Evict user-facing list cache
            redis.delete(SNIPPETS_KEY_PREFIX + problemId);
            // Evict harness cache (used by judge workers)
            redis.delete("snippet:" + problemId + ":" + language);
        } catch (Exception ignored) {}
    }

    public void evictAllSnippetCaches(Long problemId) {
        try {
            redis.delete(SNIPPETS_KEY_PREFIX + problemId);
            for (String lang : new String[]{"JAVA", "CPP", "PYTHON", "JAVASCRIPT", "C"}) {
                redis.delete("snippet:" + problemId + ":" + lang);
            }
        } catch (Exception ignored) {}
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    public boolean hasAllLanguages(Long problemId) {
        return snippetRepository.countByProblemId(problemId) == 5;
    }

    public List<String> getMissingLanguages(Long problemId) {
        List<ProgrammingLanguage> existing = snippetRepository.findByProblemId(problemId)
                .stream().map(CodeSnippet::getLanguage).collect(Collectors.toList());
        List<String> missing = new ArrayList<>();
        for (ProgrammingLanguage lang : ProgrammingLanguage.values()) {
            if (!existing.contains(lang)) missing.add(lang.name());
        }
        return missing;
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    private CodeSnippetDTO toUserDTO(CodeSnippet snippet) {
        CodeSnippetDTO dto = new CodeSnippetDTO();
        dto.setId(snippet.getId());
        dto.setProblemId(snippet.getProblem().getId());
        dto.setLanguage(snippet.getLanguage().name());
        dto.setStarterCode(extractStarterCode(snippet.getSolutionTemplate(), snippet.getLanguage()));
        dto.setSolutionTemplate(null);
        return dto;
    }

    private CodeSnippetDTO toAdminDTO(CodeSnippet snippet) {
        CodeSnippetDTO dto = new CodeSnippetDTO();
        dto.setId(snippet.getId());
        dto.setProblemId(snippet.getProblem().getId());
        dto.setLanguage(snippet.getLanguage().name());
        dto.setStarterCode(null);
        dto.setSolutionTemplate(snippet.getSolutionTemplate());
        return dto;
    }

    public static String extractStarterCode(String harness, ProgrammingLanguage language) {
        if (harness == null || harness.isEmpty()) return "";

        String startMarker = (language == ProgrammingLanguage.PYTHON) ? START_MARKER_PY : START_MARKER;
        String endMarker   = (language == ProgrammingLanguage.PYTHON) ? END_MARKER_PY   : END_MARKER;

        int startIdx = harness.indexOf(startMarker);
        int endIdx   = harness.indexOf(endMarker);

        if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
            // No markers found — return a language-appropriate blank starter so the
            // user always sees a compilable LeetCode-style skeleton instead of empty.
            return switch (language) {
                case JAVA -> "class Solution {\n    public void solve() {\n        // Write your code here\n    }\n}";
                case CPP  -> "class Solution {\npublic:\n    void solve() {\n        // Write your code here\n    }\n};";
                case PYTHON -> "class Solution:\n    def solve(self):\n        # Write your code here\n        pass";
                case JAVASCRIPT -> "function solve() {\n    // Write your code here\n}";
                case C    -> "void solve() {\n    // Write your code here\n}";
            };
        }

        String between = harness.substring(startIdx + startMarker.length(), endIdx);
        return between.replaceAll("^\\r?\\n", "").replaceAll("\\r?\\n$", "");
    }
}
