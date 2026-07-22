package com.example.codecombat2026.controller;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI problem generator — TWO-PASS design.
 *
 * Pass 1 — the model returns the problem spec: statement + function signature + a
 *          Python reference solution + 6 test INPUTS (small JSON output).
 * Verify — the reference solution is EXECUTED locally (python3) against each input to
 *          compute the true expected values. LLMs are good at writing a solution but
 *          unreliable at hand-computing outputs (especially negatives / large numbers),
 *          so we never trust the model's arithmetic — we run real code.
 * Pass 2 — a SINGLE call returns all five language harnesses as RAW delimited text
 *          (each block prefixed by ===HARNESS:<LANG>===), embedding the VERIFIED test
 *          values. Raw text → no JSON escaping; one call → friendly to NVIDIA NIM's
 *          rate limit (the old per-language design fired 6 calls and tripped 429s).
 *
 * Default model is NVIDIA Nemotron 3 Ultra (with reasoning enabled). DeepSeek is
 * available as an alternative via the "model=deepseek" parameter.
 *
 * Works for custom story-line problems too, not just LeetCode names/numbers.
 */
@RestController
@RequestMapping("/api/admin/problems")
@PreAuthorize("hasRole('ADMIN')")
public class AiProblemGeneratorController {

    @Value("${NVIDIA_API_KEY:}")
    private String nvidiaApiKey;

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepseekApiKey;

    private static final String NVIDIA_API_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String MODEL_NEMOTRON = "nvidia/nemotron-3-ultra-550b-a55b";
    private static final String MODEL_DEEPSEEK = "deepseek-ai/deepseek-v4-pro";

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

    private ModelConfig resolveModel(String modelParam) {
        if ("deepseek".equalsIgnoreCase(modelParam)) {
            return new ModelConfig(MODEL_DEEPSEEK, deepseekApiKey,
                Map.of("chat_template_kwargs", Map.of("thinking", false)));
        }
        return new ModelConfig(MODEL_NEMOTRON, nvidiaApiKey,
            Map.of(
                "chat_template_kwargs", Map.of("enable_thinking", true),
                "reasoning_budget", 16384
            ));
    }

    // ─── Main endpoint ────────────────────────────────────────────────────────

    @PostMapping("/ai-generate")
    public ResponseEntity<?> generate(@RequestBody Map<String, String> request) {
        String modelParam = request.getOrDefault("model", "kimi");
        ModelConfig cfg   = resolveModel(modelParam);

        if (cfg.apiKey() == null || cfg.apiKey().isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "API key for model '" + modelParam + "' is not configured"));
        }

        String query = request.getOrDefault("query", "").trim();
        if (query.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "query is required"));
        }

        // ── Pass 1: problem spec (statement + signature + tests) ──────────────
        // Escalating sampling per attempt: start conservative (normal problems succeed
        // immediately), then raise temperature / drop the penalty so hard, loop-prone
        // problems (interactive ones the model must reframe) get more diversity to escape
        // the repetition loop that truncates their JSON.
        double[][] pass1Sampling = { {0.5, 0.2}, {0.65, 0.1}, {0.8, 0.0}, {0.9, 0.0} };
        Map<String, Object> spec = null;
        Exception pass1Err = null;
        for (int attempt = 0; attempt < pass1Sampling.length && spec == null; attempt++) {
            try {
                String specRaw = callNim(cfg, List.of(
                    Map.of("role", "system", "content", PASS1_SYSTEM),
                    Map.of("role", "user", "content",
                        "Design the problem spec for: " + query + "\n\n" +
                        "Output ONLY the raw JSON spec object. Start with { and end with }.")
                ), 8192, pass1Sampling[attempt][0], pass1Sampling[attempt][1]);
                spec = parseJsonObject(specRaw);
            } catch (Exception e) {
                pass1Err = e;
            }
        }
        if (spec == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "AI failed (pass 1 — problem spec): "
                    + (pass1Err == null ? "unknown" : pass1Err.getMessage())));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> problem = (Map<String, Object>) spec.get("problem");
        Object signature = spec.get("signature");
        Object tests     = spec.get("tests");
        if (problem == null || signature == null || tests == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "AI pass 1 returned an incomplete spec (missing problem/signature/tests)."));
        }

        // ── Verify: run the reference solution to compute true expected values ──
        verifyExpectedValues(spec);

        // Compact spec string fed to the harness pass so all languages share data.
        String specForHarness;
        try {
            Map<String, Object> slim = new LinkedHashMap<>();
            slim.put("title",     problem.get("title"));
            slim.put("signature", signature);
            slim.put("tests",     tests);
            specForHarness = objectMapper.writeValueAsString(slim);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Could not serialize spec for harness generation: " + e.getMessage()));
        }

        // ── Pass 2: all five harnesses in one delimited response ──────────────
        // Harnesses repeat by nature, so NO frequency penalty (it truncates the later
        // languages). Nudge the temperature up on retry to break any rare loop.
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
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "AI failed (pass 2 — harnesses): "
                    + (pass2Err == null ? "unknown" : pass2Err.getMessage())));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("problem",  problem);
        result.put("snippets", snippets);
        return ResponseEntity.ok(result);
    }

    // ─── NVIDIA NIM HTTP helper (with 429 backoff) ────────────────────────────

    @SuppressWarnings("unchecked")
    private String callNim(ModelConfig cfg,
                           List<Map<String, Object>> messages,
                           int maxTokens,
                           double temperature,
                           double frequencyPenalty) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model",             cfg.modelId());
        payload.put("messages",          messages);
        payload.put("max_tokens",        maxTokens);
        payload.put("temperature",       temperature);
        payload.put("top_p",             0.95);
        payload.put("frequency_penalty", frequencyPenalty);
        payload.put("stream",            false);
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

        // "length" = hit token cap; "repetition" = NIM cut a degenerate loop.
        // Both leave the output incomplete, so treat them as retryable failures.
        if ("length".equals(finishReason) || "repetition".equals(finishReason)) {
            throw new RuntimeException("model output was incomplete (finish_reason=" + finishReason + ")");
        }
        return content;
    }

    // ─── Reference-solution execution (computes TRUE expected values) ─────────

    /**
     * Runs the Pass-1 Python reference solution against every test's args and
     * overwrites each test's "expected" with the executed result. The model is
     * reliable at WRITING a solution but not at hand-computing outputs, so we never
     * trust its arithmetic. On any failure we silently keep the model's expected
     * values as a fallback (generation still works, just less verified).
     */
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

            // Driver: define the reference fn, then print one OK:/ERR: line per test.
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

    // ─── Parsing helpers ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String content) throws Exception {
        return objectMapper.readValue(extractJsonObject(content), Map.class);
    }

    /** Extract the outermost {...} from arbitrary text. */
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

    /** Split a delimited multi-harness response into {LANG -> code}. */
    private Map<String, String> splitHarnesses(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        // Match a header line: ===HARNESS:JAVA=== (tolerate spaces / case).
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

    /** Remove ```lang fences / stray markdown around raw code. */
    private String stripCodeFences(String code) {
        String t = code.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl >= 0) t = t.substring(firstNl + 1);   // drop ```lang line
            int lastFence = t.lastIndexOf("```");
            if (lastFence >= 0) t = t.substring(0, lastFence);
        }
        return t.trim();
    }

    // ─── Prompts ──────────────────────────────────────────────────────────────

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

        TOPICS: Assign one or two comma-separated topics from this list that best
        describe the algorithmic category. Always choose from this exact list:
        Array, String, Two Pointers, Sliding Window, Binary Search, Hash Table,
        Linked List, Stack, Queue, Tree, Binary Tree, BST, Heap, Graph,
        Dynamic Programming, Greedy, Sorting, Bit Manipulation, Math, Recursion,
        Backtracking, DFS, BFS, Union Find, Trie, Divide and Conquer, Simulation

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
            "topics": "Two Pointers, Sliding Window",
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
         • CRITICAL — Each test(...) call in main()/entry MUST be wrapped in a try-catch
           (or equivalent) so a crash in one test case prints the FAIL line and continues to
           the next test case. Never let an unhandled exception abort the whole harness.
           Examples:
             Java:   try { test(arr, exp, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:input=ERR:expected=ERR:got=ERR"); }
             C++:    try { test(arr, exp, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
             Python: try: test(arr, exp, 1) except: print("TC:1:FAIL:hidden")
             JS:     try { test(arr, exp, 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
             C:      (no try-catch in C; rely on exit code — the parseOutput handler will mark it RE)

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

        Java's public class MUST be named Main. Match the structure of these worked examples
        (adapt names, types and values to the spec):

        ===HARNESS:JAVA===
        import java.util.*;
        // USER_CODE_START
        class Solution {
            public int solve(int[] arr) { return 0; }
        }
        // USER_CODE_END
        public class Main {
            static void test(int[] arr, int expected, int tc, boolean hidden) {
                int got = new Solution().solve(arr);
                if (got == expected) System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
                else if (hidden) System.out.println("TC:" + tc + ":FAIL:hidden");
                else System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(arr) + ":expected=" + expected + ":got=" + got);
            }
            public static void main(String[] a) {
                try { test(new int[]{1,2,3}, 6, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:input=" + Arrays.toString(new int[]{1,2,3}) + ":expected=6:got=ERR"); }
                try { test(new int[]{5}, 5, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:input=" + Arrays.toString(new int[]{5}) + ":expected=5:got=ERR"); }
            }
        }
        ===HARNESS:CPP===
        #include <bits/stdc++.h>
        using namespace std;
        // USER_CODE_START
        class Solution {
        public:
            int solve(vector<int>& arr) { return 0; }
        };
        // USER_CODE_END
        void test(vector<int> arr, int expected, int tc, bool hidden=false) {
            Solution sol; int got = sol.solve(arr);
            if (got == expected) cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\\n";
            else if (hidden) cout << "TC:" << tc << ":FAIL:hidden\\n";
            else { cout << "TC:" << tc << ":FAIL:input=["; for (size_t i=0;i<arr.size();i++){ if(i) cout<<","; cout<<arr[i]; } cout << "]:expected=" << expected << ":got=" << got << "\\n"; }
        }
        int main(){ try{test({1,2,3},6,1);}catch(...){cout<<"TC:1:FAIL:input=[1,2,3]:expected=6:got=ERR\\n";} try{test({5},5,2);}catch(...){cout<<"TC:2:FAIL:input=[5]:expected=5:got=ERR\\n";} return 0; }
        ===HARNESS:PYTHON===
        # USER_CODE_START
        class Solution:
            def solve(self, arr):
                return 0
        # USER_CODE_END
        def test(arr, expected, tc, hidden=False):
            got = Solution().solve(arr)
            if got == expected: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
            elif hidden: print(f"TC:{tc}:FAIL:hidden")
            else: print(f"TC:{tc}:FAIL:input={arr}:expected={expected}:got={got}")
        try: test([1,2,3], 6, 1)
        except: print("TC:1:FAIL:hidden")
        try: test([5], 5, 2)
        except: print("TC:2:FAIL:hidden")
        ===HARNESS:JAVASCRIPT===
        // USER_CODE_START
        function solve(arr) { return 0; }
        // USER_CODE_END
        function test(arr, expected, tc, hidden = false) {
            const got = solve(arr);
            if (got === expected) console.log(`TC:${tc}:PASS` + (hidden ? ':hidden' : ''));
            else if (hidden) console.log(`TC:${tc}:FAIL:hidden`);
            else console.log(`TC:${tc}:FAIL:input=[${arr}]:expected=${expected}:got=${got}`);
        }
        try { test([1,2,3], 6, 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
        try { test([5], 5, 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
        ===HARNESS:C===
        #include <stdio.h>
        // USER_CODE_START
        int solve(int* arr, int n) { return 0; }
        // USER_CODE_END
        void test(int* arr, int n, int expected, int tc, int hidden) {
            int got = solve(arr, n);
            if (got == expected) { if (hidden) printf("TC:%d:PASS:hidden\\n", tc); else printf("TC:%d:PASS\\n", tc); }
            else if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
            else { printf("TC:%d:FAIL:input=[", tc); for (int i=0;i<n;i++){ if(i) printf(","); printf("%d", arr[i]); } printf("]:expected=%d:got=%d\\n", expected, got); }
        }
        int main(){ int t1[]={1,2,3}; test(t1,3,6,1,0); int t2[]={5}; test(t2,1,5,2,0); return 0; }

        ─────────────────────────────────────────────────────────────────────────
        WORKED EXAMPLE for a ListNode problem (signature: ListNode reverseList(ListNode head)).
        Note the LeetCode-style Solution class / function, the commented banner, the real node
        type ABOVE the markers, the build/serialize helpers OUTSIDE the markers, and the
        array-encoded test data. Adapt the same pattern to TreeNode (level-order build/serialize)
        and to C++ / JS / C.

        ===HARNESS:JAVA===
        import java.util.*;
        class ListNode { int val; ListNode next; ListNode(int x){ val=x; } }
        // USER_CODE_START
        // Definition for singly-linked list.
        // class ListNode { int val; ListNode next; ListNode(int x){ val=x; } }
        class Solution {
            public ListNode reverseList(ListNode head) { return head; }
        }
        // USER_CODE_END
        public class Main {
            static ListNode build(int[] a){ ListNode d=new ListNode(0), c=d; for(int v:a){ c.next=new ListNode(v); c=c.next; } return d.next; }
            static int[] ser(ListNode h){ ArrayList<Integer> o=new ArrayList<>(); while(h!=null){ o.add(h.val); h=h.next; } int[] r=new int[o.size()]; for(int i=0;i<r.length;i++) r[i]=o.get(i); return r; }
            static void test(int[] in, int[] expected, int tc, boolean hidden){
                int[] got = ser(new Solution().reverseList(build(in)));
                if (Arrays.equals(got, expected)) System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
                else if (hidden) System.out.println("TC:" + tc + ":FAIL:hidden");
                else System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(in) + ":expected=" + Arrays.toString(expected) + ":got=" + Arrays.toString(got));
            }
            public static void main(String[] a){
                try { test(new int[]{1,2,3}, new int[]{3,2,1}, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:input=" + Arrays.toString(new int[]{1,2,3}) + ":expected=" + Arrays.toString(new int[]{3,2,1}) + ":got=ERR"); }
                try { test(new int[]{}, new int[]{}, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:input=" + Arrays.toString(new int[]{}) + ":expected=" + Arrays.toString(new int[]{}) + ":got=ERR"); }
            }
        }
        ===HARNESS:PYTHON===
        class ListNode:
            def __init__(self, val=0, next=None): self.val, self.next = val, next
        # USER_CODE_START
        # Definition for singly-linked list.
        # class ListNode:
        #     def __init__(self, val=0, next=None): self.val, self.next = val, next
        class Solution:
            def reverseList(self, head):
                return head
        # USER_CODE_END
        def _build(a):
            d = ListNode(0); c = d
            for v in a: c.next = ListNode(v); c = c.next
            return d.next
        def _ser(h):
            o = []
            while h: o.append(h.val); h = h.next
            return o
        def test(inp, expected, tc, hidden=False):
            got = _ser(Solution().reverseList(_build(inp)))
            if got == expected: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
            elif hidden: print(f"TC:{tc}:FAIL:hidden")
            else: print(f"TC:{tc}:FAIL:input={inp}:expected={expected}:got={got}")
        try: test([1,2,3], [3,2,1], 1)
        except: print("TC:1:FAIL:hidden")
        try: test([], [], 2)
        except: print("TC:2:FAIL:hidden")
        ─────────────────────────────────────────────────────────────────────────
        """;
}
