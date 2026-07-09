package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.CodeSnippet;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.TooManyRequestsException;
import com.example.codecombat2026.repository.CodeSnippetRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for generating coding problems using AI (NVIDIA NIM API).
 * 
 * This service:
 * - Enforces rate limiting (5 generations per user per day)
 * - Generates problems using a two-pass AI approach
 * - Creates Problem entities with PRIVATE_OWNED visibility
 * - Stores test cases in code snippets
 * - Returns ProblemDTO for preview
 * 
 * Requirements: 9.1, 9.2, 9.4, 9.6, 9.7, 30.4
 */
@Service
public class AIProblemGeneratorService {

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private CodeSnippetRepository codeSnippetRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${NVIDIA_API_KEY:}")
    private String kimiApiKey;

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepseekApiKey;

    private static final String NVIDIA_API_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String MODEL_KIMI = "moonshotai/kimi-k2.6";
    private static final String MODEL_DEEPSEEK = "deepseek-ai/deepseek-v4-pro";
    private static final int RATE_LIMIT_PER_DAY = 5;

    // Frontend consumes snippets keyed by these names.
    private static final List<String> LANGS = List.of("JAVA", "CPP", "PYTHON", "JAVASCRIPT", "C");

    // 20-minute read timeout — a couple of large AI passes plus retries.
    private final RestTemplate restTemplate = createRestTemplate();
    
    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10_000);
        f.setReadTimeout(1_200_000);
        return new RestTemplate(f);
    }

    // Lenient parser: tolerates literal newlines / non-standard escapes in AI JSON.
    private final ObjectMapper objectMapper = new ObjectMapper(
        JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .build()
    );

    private record ModelConfig(String modelId, String apiKey, Map<String, Object> extra) {}

    /**
     * Check rate limit for AI problem generation.
     * Throws TooManyRequestsException if user has exceeded 5 generations per day.
     * 
     * @param userId User ID to check rate limit for
     * @throws TooManyRequestsException if rate limit exceeded
     */
    public void checkRateLimit(Long userId) {
        String key = "ai:problem:gen:user:" + userId;
        Long count = redis.opsForValue().increment(key);
        
        if (count == null) {
            count = 1L;
        }
        
        if (count == 1) {
            redis.expire(key, Duration.ofDays(1));
        }
        
        if (count > RATE_LIMIT_PER_DAY) {
            throw new TooManyRequestsException("You have reached your daily limit of 5 AI-generated problems");
        }
    }

    /**
     * Generate a new problem using AI and store it in the database.
     * 
     * @param prompt Natural language description of the problem (max 1000 characters)
     * @param difficulty Problem difficulty: EASY, MEDIUM, or HARD
     * @param topic Optional topic/category (max 100 characters)
     * @param hostUserId User ID of the Contest_Host creating the problem
     * @return Generated Problem entity with PRIVATE_OWNED visibility
     * @throws TooManyRequestsException if rate limit exceeded
     * @throws RuntimeException if AI generation fails
     */
    @Transactional
    public Problem generateProblem(String prompt, String difficulty, String topic, Long hostUserId) {
        // Check rate limit
        checkRateLimit(hostUserId);

        // Validate inputs
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }
        if (prompt.length() > 1000) {
            throw new IllegalArgumentException("Prompt cannot exceed 1000 characters");
        }
        if (difficulty == null || !Arrays.asList("EASY", "MEDIUM", "HARD").contains(difficulty)) {
            throw new IllegalArgumentException("Difficulty must be EASY, MEDIUM, or HARD");
        }
        if (topic != null && topic.length() > 100) {
            throw new IllegalArgumentException("Topic cannot exceed 100 characters");
        }

        // Default to Kimi model
        ModelConfig cfg = resolveModel("kimi");

        if (cfg.apiKey() == null || cfg.apiKey().isBlank()) {
            throw new RuntimeException("API key for AI model is not configured");
        }

        // Build query with topic if provided
        String query = prompt.trim();
        if (topic != null && !topic.trim().isEmpty()) {
            query = "Topic: " + topic + "\n" + query;
        }

        // ── Pass 1: problem spec (statement + signature + tests) ──────────────
        double[][] pass1Sampling = { {0.5, 0.2}, {0.65, 0.1}, {0.8, 0.0}, {0.9, 0.0} };
        Map<String, Object> spec = null;
        Exception pass1Err = null;
        
        for (int attempt = 0; attempt < pass1Sampling.length && spec == null; attempt++) {
            try {
                String specRaw = callNim(cfg, List.of(
                    Map.of("role", "system", "content", PASS1_SYSTEM),
                    Map.of("role", "user", "content",
                        "Design the problem spec for: " + query + "\n\n" +
                        "Difficulty level should be: " + difficulty + "\n\n" +
                        "Output ONLY the raw JSON spec object. Start with { and end with }.")
                ), 8192, pass1Sampling[attempt][0], pass1Sampling[attempt][1]);
                spec = parseJsonObject(specRaw);
            } catch (Exception e) {
                pass1Err = e;
            }
        }
        
        if (spec == null) {
            throw new RuntimeException("AI failed to generate problem spec: " + 
                (pass1Err == null ? "unknown error" : pass1Err.getMessage()));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> problemData = (Map<String, Object>) spec.get("problem");
        Object signature = spec.get("signature");
        Object tests = spec.get("tests");
        
        if (problemData == null || signature == null || tests == null) {
            throw new RuntimeException("AI returned incomplete spec (missing problem/signature/tests)");
        }

        // ── Verify: run the reference solution to compute true expected values ──
        verifyExpectedValues(spec);

        // Compact spec string for harness generation
        String specForHarness;
        try {
            Map<String, Object> slim = new LinkedHashMap<>();
            slim.put("title", problemData.get("title"));
            slim.put("signature", signature);
            slim.put("tests", tests);
            specForHarness = objectMapper.writeValueAsString(slim);
        } catch (Exception e) {
            throw new RuntimeException("Could not serialize spec for harness generation: " + e.getMessage());
        }

        // ── Pass 2: all five harnesses in one delimited response ──────────────
        double[] pass2Temp = { 0.5, 0.65, 0.8 };
        Map<String, String> snippets = null;
        Exception pass2Err = null;
        
        for (int attempt = 0; attempt < pass2Temp.length && snippets == null; attempt++) {
            try {
                String harnessRaw = callNim(cfg, List.of(
                    Map.of("role", "system", "content", HARNESS_SYSTEM),
                    Map.of("role", "user", "content",
                        "Spec:\n" + specForHarness + "\n\n" +
                        "Output all five harnesses. Before each one put a line exactly like " +
                        "===HARNESS:JAVA=== (then CPP, PYTHON, JAVASCRIPT, C). Raw source only, " +
                        "no markdown fences, no commentary.")
                ), 8192, pass2Temp[attempt], 0.0);
                Map<String, String> parsed = splitHarnesses(harnessRaw);
                if (parsed.keySet().containsAll(LANGS)) {
                    snippets = parsed;
                } else {
                    pass2Err = new RuntimeException("missing languages: only got " + parsed.keySet());
                }
            } catch (Exception e) {
                pass2Err = e;
            }
        }
        
        if (snippets == null) {
            throw new RuntimeException("AI failed to generate harnesses: " + 
                (pass2Err == null ? "unknown error" : pass2Err.getMessage()));
        }

        // Create Problem entity
        Problem problem = new Problem();
        problem.setTitle((String) problemData.get("title"));
        problem.setDescription((String) problemData.get("description"));
        problem.setInputFormat((String) problemData.get("inputFormat"));
        problem.setOutputFormat((String) problemData.get("outputFormat"));
        problem.setConstraints((String) problemData.get("constraints"));
        problem.setTimeLimit(getDoubleValue(problemData.get("timeLimit")));
        problem.setMemoryLimit(getIntegerValue(problemData.get("memoryLimit")));
        problem.setLevel(difficulty);
        problem.setVisibility("PRIVATE_OWNED");
        problem.setCreatedBy(hostUserId);
        problem.setActive(true);
        problem.setExample1((String) problemData.get("example1"));
        problem.setExample2((String) problemData.get("example2"));
        problem.setExample3((String) problemData.get("example3"));

        // Save problem to database
        problem = problemRepository.save(problem);

        // Create CodeSnippet entities for each language
        for (Map.Entry<String, String> entry : snippets.entrySet()) {
            String langName = entry.getKey();
            String harnessCode = entry.getValue();
            
            CodeSnippet snippet = new CodeSnippet();
            snippet.setProblem(problem);
            snippet.setLanguage(CodeSnippet.ProgrammingLanguage.valueOf(langName));
            snippet.setSolutionTemplate(harnessCode);
            
            codeSnippetRepository.save(snippet);
        }

        return problem;
    }

    /**
     * DTO for problem editing request.
     * Contains all editable fields of a Problem entity.
     */
    public static class EditProblemDTO {
        public String title;
        public String description;
        public String inputFormat;
        public String outputFormat;
        public String constraints;
        public Double timeLimit;
        public Integer memoryLimit;
        public String level;
        public String example1;
        public String example2;
        public String example3;
    }

    /**
     * Edit an existing problem created by a Contest_Host or Admin.
     * 
     * Validation rules (Requirements 10.1, 10.2, 10.3, 10.4, 30.5):
     * - Problem must exist
     * - User must be the problem creator (created_by == userId) OR user must be an Admin
     * - Problem must not be attached to a LIVE or ENDED contest
     * - PUBLIC visibility problems can only be edited by Admins
     * 
     * @param problemId ID of the problem to edit
     * @param dto EditProblemDTO containing updated fields
     * @param userId ID of the user attempting to edit
     * @return Updated Problem entity
     * @throws IllegalArgumentException if validation fails
     * @throws SecurityException if user lacks permission
     */
    @Transactional
    public Problem editProblem(Long problemId, EditProblemDTO dto, Long userId) {
        // 1. Find the problem
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new IllegalArgumentException("Problem not found with id: " + problemId));

        // 2. Check if user exists and get their roles
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        boolean isAdmin = user.getRoles().stream()
            .anyMatch(role -> "ROLE_ADMIN".equals(role.getName().name()));

        // 3. Validate ownership (Requirement 10.1, 30.5)
        // User must be creator OR admin
        if (!isAdmin && !userId.equals(problem.getCreatedBy())) {
            throw new SecurityException("You do not have permission to edit this problem. Only the creator or an admin can edit it.");
        }

        // 4. Validate PUBLIC visibility (Requirement 10.3)
        // PUBLIC problems can only be edited by admins
        if ("PUBLIC".equals(problem.getVisibility()) && !isAdmin) {
            throw new SecurityException("PUBLIC problems can only be edited by administrators");
        }

        // 5. Check if problem is attached to LIVE or ENDED contests (Requirement 10.1, 10.4)
        // We check through the contests collection on the Problem entity
        boolean attachedToLiveOrEndedContest = problem.getContests().stream()
            .anyMatch(contest -> {
                Contest.ContestStatus status = contest.getStatus();
                return status == Contest.ContestStatus.LIVE || status == Contest.ContestStatus.ENDED;
            });

        if (attachedToLiveOrEndedContest) {
            throw new IllegalArgumentException("Cannot edit problem that is attached to a LIVE or ENDED contest");
        }

        // 6. Update fields (Requirement 10.2)
        if (dto.title != null) {
            problem.setTitle(dto.title);
        }
        if (dto.description != null) {
            problem.setDescription(dto.description);
        }
        if (dto.inputFormat != null) {
            problem.setInputFormat(dto.inputFormat);
        }
        if (dto.outputFormat != null) {
            problem.setOutputFormat(dto.outputFormat);
        }
        if (dto.constraints != null) {
            problem.setConstraints(dto.constraints);
        }
        if (dto.timeLimit != null) {
            problem.setTimeLimit(dto.timeLimit);
        }
        if (dto.memoryLimit != null) {
            problem.setMemoryLimit(dto.memoryLimit);
        }
        if (dto.level != null) {
            // Validate level
            if (!Arrays.asList("EASY", "MEDIUM", "HARD").contains(dto.level)) {
                throw new IllegalArgumentException("Level must be EASY, MEDIUM, or HARD");
            }
            problem.setLevel(dto.level);
        }
        if (dto.example1 != null) {
            problem.setExample1(dto.example1);
        }
        if (dto.example2 != null) {
            problem.setExample2(dto.example2);
        }
        if (dto.example3 != null) {
            problem.setExample3(dto.example3);
        }

        // Save with updated timestamp (JPA @PreUpdate would handle this if we had that annotation)
        Problem updated = problemRepository.save(problem);
        
        return updated;
    }

    private ModelConfig resolveModel(String modelParam) {
        if ("deepseek".equalsIgnoreCase(modelParam)) {
            return new ModelConfig(MODEL_DEEPSEEK, deepseekApiKey,
                Map.of("chat_template_kwargs", Map.of("thinking", false)));
        }
        return new ModelConfig(MODEL_KIMI, kimiApiKey, Map.of());
    }

    @SuppressWarnings("unchecked")
    private String callNim(ModelConfig cfg,
                           List<Map<String, Object>> messages,
                           int maxTokens,
                           double temperature,
                           double frequencyPenalty) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", cfg.modelId());
        payload.put("messages", messages);
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", temperature);
        payload.put("top_p", 0.95);
        payload.put("frequency_penalty", frequencyPenalty);
        payload.put("stream", false);
        payload.putAll(cfg.extra());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cfg.apiKey());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                response = restTemplate.exchange(NVIDIA_API_URL, HttpMethod.POST, entity, Map.class);
                break;
            } catch (HttpStatusCodeException ex) {
                if (ex.getStatusCode().value() == 429 && attempt < 3) {
                    Thread.sleep(8000L * (attempt + 1));   // 8s, 16s, 24s backoff
                    continue;
                }
                throw ex;
            }
        }
        if (response == null) throw new RuntimeException("rate-limited (429) after retries");

        Map body = response.getBody();
        if (body == null) throw new RuntimeException("Empty response from AI");

        List<Map> choices = (List<Map>) body.get("choices");
        if (choices == null || choices.isEmpty()) throw new RuntimeException("No choices in AI response");

        Map choice = choices.get(0);
        String finishReason = (String) choice.get("finish_reason");
        String content = (String) ((Map) choice.get("message")).get("content");
        if (content == null || content.isBlank()) throw new RuntimeException("Empty content from AI");

        if ("length".equals(finishReason) || "repetition".equals(finishReason)) {
            throw new RuntimeException("model output was incomplete (finish_reason=" + finishReason + ")");
        }
        return content;
    }

    @SuppressWarnings("unchecked")
    private void verifyExpectedValues(Map<String, Object> spec) {
        Path tmp = null;
        try {
            Object refObj = spec.get("referenceSolutionPython");
            if (!(refObj instanceof String ref) || ref.isBlank()) return;
            ref = stripCodeFences(ref);

            Map<String, Object> sig = (Map<String, Object>) spec.get("signature");
            String fname = sig == null ? null : (String) sig.get("name");
            if (fname == null || fname.isBlank()) return;

            List<Map<String, Object>> tests = (List<Map<String, Object>>) spec.get("tests");
            if (tests == null || tests.isEmpty()) return;

            List<Object> argsList = new ArrayList<>();
            for (Map<String, Object> t : tests) argsList.add(t.get("args"));
            String argsJson = objectMapper.writeValueAsString(argsList);

            String driver = ref
                + "\n\nimport json as _json\n"
                + "_tests = " + argsJson + "\n"
                + "for _t in _tests:\n"
                + "    try:\n"
                + "        _r = " + fname + "(**_t)\n"
                + "        print('OK:' + _json.dumps(_r))\n"
                + "    except Exception as _e:\n"
                + "        print('ERR:' + str(_e))\n";

            tmp = Files.createTempFile("ccref", ".py");
            Files.writeString(tmp, driver);

            Process p = new ProcessBuilder("python3", tmp.toString()).start();
            boolean done = p.waitFor(15, TimeUnit.SECONDS);
            if (!done) { p.destroyForcibly(); return; }
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            int idx = 0;
            for (String line : out.split("\\r?\\n")) {
                if (idx >= tests.size()) break;
                line = line.trim();
                if (line.startsWith("OK:")) {
                    try {
                        Object val = objectMapper.readValue(line.substring(3), Object.class);
                        tests.get(idx).put("expected", val);
                    } catch (Exception ignore) { /* keep model value */ }
                    idx++;
                } else if (line.startsWith("ERR:")) {
                    idx++;   // keep model's expected for this one
                }
            }
        } catch (Exception ignored) {
            // fall back entirely to model-provided expected values
        } finally {
            if (tmp != null) { try { Files.deleteIfExists(tmp); } catch (Exception ignore) {} }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String content) throws Exception {
        return objectMapper.readValue(extractJsonObject(content), Map.class);
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        if (start < 0) return text.trim();

        boolean inString = false, escaped = false;
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped)               { escaped = false; continue; }
            if (c == '\\' && inString) { escaped = true;  continue; }
            if (c == '"')              { inString = !inString; continue; }
            if (!inString) {
                if      (c == '{') depth++;
                else if (c == '}') { if (--depth == 0) return text.substring(start, i + 1); }
            }
        }
        return text.substring(start);
    }

    private Map<String, String> splitHarnesses(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        String[] parts = raw.split("(?im)^\\s*=+\\s*HARNESS\\s*:\\s*");
        for (String part : parts) {
            String t = part.stripLeading();
            int nl = t.indexOf('\n');
            if (nl < 0) continue;
            String header = t.substring(0, nl).replace("=", "").trim().toUpperCase();
            String lang = switch (header) {
                case "JAVA" -> "JAVA";
                case "CPP", "C++" -> "CPP";
                case "PYTHON", "PY" -> "PYTHON";
                case "JAVASCRIPT", "JS", "NODE" -> "JAVASCRIPT";
                case "C" -> "C";
                default -> null;
            };
            if (lang == null || out.containsKey(lang)) continue;
            String code = stripCodeFences(t.substring(nl + 1));
            if (!code.isBlank()) out.put(lang, code);
        }
        return out;
    }

    private String stripCodeFences(String code) {
        String t = code.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl >= 0) t = t.substring(firstNl + 1);
            int lastFence = t.lastIndexOf("```");
            if (lastFence >= 0) t = t.substring(0, lastFence);
        }
        return t.trim();
    }

    private Double getDoubleValue(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntegerValue(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // AI Prompts (same as in the controller)
    private static final String PASS1_SYSTEM = """
        You are a problem designer for CodeCombat 2026, a competitive-programming judge.
        The user gives EITHER a LeetCode problem (name or number) OR a custom problem
        described in their own words (possibly a story). Produce ONE raw JSON object —
        the problem spec — and NOTHING else.

        REFRAMING (critical — read first):
        The judge runs exactly ONE method that the candidate implements — exactly like a
        LeetCode `Solution` class with a single public method: inputs in, one concrete value out.
        If the requested problem is interactive or design/API-based (e.g. "Robot Room
        Cleaner", an iterator, "implement an LRU cache", a class to design), or otherwise
        has no direct input→output, you MUST REFRAME it into an equivalent single method:
        pass the hidden state as explicit inputs (e.g. the grid as int[][], start as int row
        and int col) and return a concrete value. `signature.name` is that method's name and
        MUST be a clean camelCase verb phrase (e.g. twoSum, reverseList, maxDepth) — it becomes
        the method inside the candidate's Solution class.

        DATA STRUCTURES (LeetCode-style — IMPORTANT):
        Two custom node types ARE allowed as parameter / return types, but ONLY when the
        problem is genuinely about that structure (otherwise prefer plain arrays):
          • ListNode  — singly-linked list. Serialized as the array of node values IN ORDER,
                        e.g. the list 1→2→3 is [1,2,3]; the empty list is [].
          • TreeNode  — binary tree. Serialized as a level-order (BFS) array with null for
                        missing children, e.g. [1,null,2,3]; the empty tree is [].
        In "tests", ALWAYS write a ListNode / TreeNode value as its serialized array exactly
        as above (never as an object). A ListNode[] (e.g. "merge k lists") is an array of such
        arrays. Choose ListNode / TreeNode types only for problems that are naturally about
        linked lists or binary trees; everything else stays on the plain allowed types.

        Keep "description" concise (about 60-120 words) and matching the pure-function version.
        Do NOT ramble or repeat sentences.

        Shape:
        {
          "problem": {
            "title": "short title",
            "description": "full, self-contained statement in clear prose; if the user gave a story, keep the narrative",
            "inputFormat": "how the input is described to the solver",
            "outputFormat": "what to return / print",
            "constraints": "newline-separated constraints",
            "timeLimit": 5,            // seconds: EASY 3, MEDIUM 5, HARD 8
            "memoryLimit": 256,        // MB, 128-512
            "level": "EASY|MEDIUM|HARD",
            "example1": "Input: ...\\nOutput: ...\\nExplanation: ...",
            "example2": "...",
            "example3": "..."
          },
          "signature": {
            "name": "camelCaseFunctionName",
            "returnType": "<type>",
            "params": [ { "name": "paramName", "type": "<type>" } ]
          },
          "referenceSolutionPython": "def camelCaseFunctionName(paramName, ...):\\n    ...correct solution...\\n    return answer",
          "tests": [
            { "args": { "paramName": <value> }, "expected": <value>, "hidden": false },
            ... EXACTLY 6 entries: the first 4 have hidden=false, the last 2 have hidden=true
          ]
        }

        Allowed <type> values ONLY: int, long, double, boolean, String, char,
        int[], long[], double[], boolean[], String[], int[][], ListNode, TreeNode, ListNode[].

        THE REFERENCE SOLUTION IS THE MOST IMPORTANT FIELD:
        • "referenceSolutionPython" must be a CORRECT, complete Python function named EXACTLY
          signature.name, taking parameters named EXACTLY the signature param names, and
          RETURNING the answer (do not print anything).
        • It will be EXECUTED to compute the real expected values, so it must actually work.
          Standard library imports (collections, heapq, math, bisect, itertools) are allowed —
          put any import lines at the top of the string.
        • IMPORTANT for ListNode / TreeNode: the reference receives each such argument as the
          PLAIN serialized Python list (e.g. [1,2,3] or [1,null→None,2,3]) and must RETURN the
          serialized list form of the answer. If it is easier to solve with real nodes, define
          tiny build/serialize helpers AT THE TOP of the reference string and convert internally,
          but the function's input and output must be the list encodings so the executed expected
          value is itself a list. (e.g. `def sortList(head): vals=sorted(head); return vals`)
        • Use JSON-friendly return types: int, float, bool, str, or lists of those. For index
          answers return a list like [i, j].

        TEST INPUT RULES:
        • "args" keys must EXACTLY equal the param names declared in "signature".
        • Every test input MUST be legal under the problem's rules and have a well-defined answer.
          If the statement guarantees "exactly one solution exists", every input must genuinely
          have one. Respect stated constraints (length/value ranges, uniqueness). Do not create
          inputs the constraints forbid (e.g. a single-element array when two distinct indices
          are required).
        • Choose 6 meaningful inputs incl. valid edge cases (smallest legal size, all-equal,
          negatives, maximum) — only edges the constraints actually permit.
        • Still fill each "expected" with your best hand-computed answer (used only as a fallback
          if execution fails), but the executed reference solution is the source of truth.

        OUTPUT: only the JSON object. First character {, last character }. No markdown, no comments.
        """;

    private static final String HARNESS_SYSTEM = """
        You generate the five CodeCombat 2026 judge harnesses (JAVA, CPP, PYTHON, JAVASCRIPT, C)
        for a given problem spec. INPUT: a JSON spec with the function `signature` and `tests`
        (each test has args, expected, hidden). OUTPUT: raw source code only.

        Emit the harnesses in this order, each preceded by its own header line EXACTLY:
        ===HARNESS:JAVA===
        ===HARNESS:CPP===
        ===HARNESS:PYTHON===
        ===HARNESS:JAVASCRIPT===
        ===HARNESS:C===
        No markdown fences, no prose — just the header lines and the code.

        Harness contract (identical scoring across languages):
        • One self-contained, runnable file. NO stdin (no Scanner/cin/scanf/input()/readFileSync).
          Hardcode the test data from the spec.
        • Wrap ONLY the user-facing solution with the markers (// USER_CODE_START / // USER_CODE_END;
          Python uses # ). This is the EXACT region shown to the candidate and replaced on submit.
        • USER-FACING SHAPE — make it look exactly like LeetCode:
            JAVA, C++, PYTHON  → a `Solution` class containing ONE public method (the signature).
                                 The class lives between the markers. Empty body returns a default.
            JAVASCRIPT, C      → a single top-level function (the signature). No class.
        • The test driver + main()/entry live OUTSIDE the markers and CALL into the user code:
            Java:    `new Solution().method(args)`
            C++:     `Solution sol; sol.method(args)`
            Python:  `Solution().method(args)`
            JS:      `method(args)`
            C:       `method(args)`
        • A test(...) helper is called once per spec test, numbered 1..6 in spec order, receiving
          the hidden flag. Each test prints EXACTLY one line (UNCHANGED — this is the score format):
             visible pass:  TC:<n>:PASS
             hidden  pass:  TC:<n>:PASS:hidden
             visible fail:  TC:<n>:FAIL:input=<args repr>:expected=<exp>:got=<got>
             hidden  fail:  TC:<n>:FAIL:hidden
        • Compare scalars with ==; compare arrays element-wise. Hidden tests must NEVER print
          input/expected/got. Keep the printed input/expected/got free of ':' characters.

        JAVA STRUCTURE RULE (important): declare `class Solution` as a TOP-LEVEL package-private
        class in the same file, ABOVE `public class Main`, and put it BETWEEN the markers. Java
        permits multiple top-level classes in one file as long as only one (Main) is public. This
        keeps the candidate's editable region a clean LeetCode-style `class Solution { ... }`.

        DATA STRUCTURES (ListNode / TreeNode) — LeetCode style:
        When the signature uses ListNode, ListNode[], or TreeNode:
          1. Place the REAL (uncommented) node class/struct ABOVE the marker region so BOTH the
             Solution code and the driver compile against it (Java/C++/C: class/struct; Py/JS: class).
          2. INSIDE the markers — the candidate-visible region — put the LeetCode-style COMMENTED
             definition banner just above the Solution method (Java/C++/Py) or function (JS/C),
             e.g. for Java:
               // USER_CODE_START
               // Definition for singly-linked list.
               // class ListNode { int val; ListNode next; ListNode(int x){ val=x; } }
               class Solution {
                   public ListNode reverseList(ListNode head) { return head; }
               }
               // USER_CODE_END
          3. Inside test(...), BUILD the real structure from the hardcoded serialized array
             (linked list: chain values in order; tree: level-order BFS with null children), call
             the solution, then SERIALIZE the result back to an array before comparing/printing.
             Provide tiny build/serialize helpers OUTSIDE the markers (candidate never sees them).
             Serialized forms MUST match the spec encodings (list 1→2→3 == [1,2,3]; tree
             level-order with nulls). Printed input/expected/got use the array form.
        For plain array/scalar problems use the same Solution-class / function shape (no node types).

        Java's public class MUST be named Main. Output the full harnesses without truncation.
        """;
}
