package com.example.codecombat2026.controller;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/problems")
@PreAuthorize("hasRole('ADMIN')")
public class AiProblemGeneratorController {

    @Value("${NVIDIA_API_KEY:}")
    private String kimiApiKey;

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepseekApiKey;

    private static final String NVIDIA_API_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String MODEL_KIMI     = "moonshotai/kimi-k2.6";
    private static final String MODEL_DEEPSEEK = "deepseek-ai/deepseek-v4-pro";

    private final RestTemplate restTemplate = new RestTemplate();
    // Lenient parser: tolerates literal newlines and non-standard escapes inside AI-generated JSON strings
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
        return new ModelConfig(MODEL_KIMI, kimiApiKey, Map.of());
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

        // ── Pass 1: generate problem + harnesses ──────────────────────────────
        String pass1Content;
        try {
            pass1Content = callNim(cfg, List.of(
                Map.of("role", "system", "content", buildSystemPrompt()),
                Map.of("role", "user", "content",
                    "Generate a complete CodeCombat problem for: " + query + "\n\n" +
                    "OUTPUT ONLY the raw JSON object. No markdown fences, no ```json, " +
                    "no explanation, no preamble. Start your response with { and end with }.")
            ), 12288);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "AI call failed (pass 1): " + e.getMessage()));
        }

        Map<String, Object> result;
        try {
            result = objectMapper.readValue(extractJsonObject(pass1Content), Map.class);
        } catch (Exception e) {
            String raw = pass1Content.length() > 500 ? pass1Content.substring(0, 500) + "..." : pass1Content;
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "AI returned invalid JSON: " + e.getMessage(), "raw", raw));
        }

        // ── Pass 2: verify + fix expected values (non-fatal if it fails) ──────
        @SuppressWarnings("unchecked")
        Map<String, Object> snippets = (Map<String, Object>) result.get("snippets");
        if (snippets != null && !snippets.isEmpty()) {
            try {
                Map<String, Object> fixed = verifyTestCases(cfg, snippets);
                if (fixed != null) result.put("snippets", fixed);
            } catch (Exception ignored) {
                // pass 2 failure → return pass 1 result as-is
            }
        }

        return ResponseEntity.ok(result);
    }

    // ─── Pass 2: re-trace every test case and fix wrong expected values ───────

    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyTestCases(ModelConfig cfg,
                                                Map<String, Object> snippets) throws Exception {
        StringBuilder prompt = new StringBuilder(4096);
        prompt.append("These are code harnesses you generated for a competitive programming problem.\n");
        prompt.append("TASK: For EACH test case in main(), re-trace the algorithm step-by-step\n");
        prompt.append("from scratch and verify the expected value. Fix any that are wrong.\n");
        prompt.append("Keep harness structure, markers, and helper functions identical.\n\n");
        prompt.append("Return ONLY this JSON (no markdown, no preamble, start with {, end with }):\n");
        prompt.append("{\"JAVA\":\"...\",\"CPP\":\"...\",\"C\":\"...\",\"PYTHON\":\"...\",\"JAVASCRIPT\":\"...\"}\n\n");

        for (Map.Entry<String, Object> e : snippets.entrySet()) {
            prompt.append("=== ").append(e.getKey()).append(" ===\n");
            prompt.append(e.getValue()).append("\n\n");
        }

        String content = callNim(cfg,
            List.of(Map.of("role", "user", "content", prompt.toString())),
            8192);

        Map<String, Object> fixed = objectMapper.readValue(extractJsonObject(content), Map.class);

        // sanity-check: must contain at least one known language key
        if (fixed.containsKey("JAVA") || fixed.containsKey("CPP") || fixed.containsKey("PYTHON")) {
            return fixed;
        }
        return null;
    }

    // ─── NVIDIA NIM HTTP helper ───────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String callNim(ModelConfig cfg,
                           List<Map<String, Object>> messages,
                           int maxTokens) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model",       cfg.modelId());
        payload.put("messages",    messages);
        payload.put("max_tokens",  maxTokens);
        payload.put("temperature", 0.2);
        payload.put("top_p",       0.9);
        payload.put("stream",      false);
        payload.putAll(cfg.extra());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cfg.apiKey());

        ResponseEntity<Map> response = restTemplate.exchange(
            NVIDIA_API_URL, HttpMethod.POST,
            new HttpEntity<>(payload, headers), Map.class);

        Map body = response.getBody();
        if (body == null) throw new RuntimeException("Empty response from AI");

        List<Map> choices = (List<Map>) body.get("choices");
        if (choices == null || choices.isEmpty()) throw new RuntimeException("No choices in AI response");

        String content = (String) ((Map) choices.get(0).get("message")).get("content");
        if (content == null || content.isBlank()) throw new RuntimeException("Empty content from AI");
        return content;
    }

    // ─── Extract outermost {...} from arbitrary text ──────────────────────────

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        if (start < 0) return text.trim();

        boolean inString = false, escaped = false;
        int depth = 0;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped)          { escaped = false; continue; }
            if (c == '\\' && inString) { escaped = true;  continue; }
            if (c == '"')         { inString = !inString; continue; }
            if (!inString) {
                if      (c == '{') depth++;
                else if (c == '}') { if (--depth == 0) return text.substring(start, i + 1); }
            }
        }
        return text.substring(start);
    }

    // ─── System prompt ────────────────────────────────────────────────────────

    private String buildSystemPrompt() {
        return """
            You are an expert competitive programming problem generator for CodeCombat 2026.
            Given a LeetCode problem name or number, produce a COMPLETE, CORRECT problem
            definition plus 5 language harness files that compile and run as-is.

            ════════════════════════════════════════
            SECTION 1 — OUTPUT FORMAT (MANDATORY)
            ════════════════════════════════════════
            - Respond with ONLY a single raw JSON object.
            - NO markdown fences (no ```json or ```), NO preamble, NO trailing text.
            - First character MUST be {  Last character MUST be }
            - All string values: proper JSON escaping (\\n for newlines, \\" for quotes inside strings).

            ════════════════════════════════════════
            SECTION 2 — DATABASE SCHEMA
            ════════════════════════════════════════
            timeLimit : Double  (seconds)  EASY=3.0  MEDIUM=5.0  HARD=8.0
            memoryLimit: Integer (MB)      128 – 512
            level      : "EASY" | "MEDIUM" | "HARD"

            ════════════════════════════════════════
            SECTION 3 — HARNESS RULES (READ EVERY RULE)
            ════════════════════════════════════════

            RULE 1 — SELF-CONTAINED, NO STDIN
            Every harness is a single complete runnable file.
            ZERO stdin: no Scanner, no cin/scanf, no input(), no readline(), no readFileSync.
            All test inputs are hardcoded constants inside main().

            RULE 2 — MARKERS (exactly one pair, exact spelling)
            Java / C / C++ / JS : // USER_CODE_START   and   // USER_CODE_END
            Python               : # USER_CODE_START    and   # USER_CODE_END

            RULE 2B — solve() MUST BE AN EMPTY STUB — THIS IS NON-NEGOTIABLE
            The solve() function between USER_CODE_START and USER_CODE_END MUST contain
            ONLY a default return value. NEVER put any working algorithm inside solve().
            The user is supposed to write the algorithm themselves — if you put the answer
            there you have destroyed the purpose of the platform.
            Correct stub examples:
              Java:       public static int solve(int[] nums) { return 0; }
              Java:       public static String solve(String s) { return ""; }
              Java:       public static boolean solve(int[] nums) { return false; }
              C++:        int solve(vector<int>& nums) { return 0; }
              Python:     def solve(nums): return 0
              JavaScript: function solve(nums) { return 0; }
            WRONG — never do this:
              public static String solve(String s, int numRows) {
                  // ... 20 lines of zigzag algorithm ...   ← FORBIDDEN
              }

            RULE 3 — OUTPUT FORMAT (character-perfect, no extra spaces)
            Visible PASS : TC:N:PASS
            Hidden  PASS : TC:N:PASS:hidden
            Visible FAIL : TC:N:FAIL:input=<repr>:expected=<val>:got=<val>
            Hidden  FAIL : TC:N:FAIL:hidden
            N is 1-based. No spaces around colons. Newline after each line.

            RULE 4 — TEST COUNT
            Exactly 4 visible + 2 hidden = 6 total.
            Hidden tests NEVER print input, expected, or got.

            RULE 5 — TEST CASE CORRECTNESS (MOST CRITICAL RULE)
            Before writing EACH test case, mentally trace the correct algorithm step-by-step
            and compute the expected output by hand. Write it down, verify it, then encode it.
            ONE wrong expected value = candidate fails for correct code. This is unacceptable.
            Test cases must be DIVERSE — cover:
              • empty / null input (when allowed by constraints)
              • single-element input
              • all-same elements
              • already-sorted / reverse-sorted arrays
              • negative numbers and zero (when applicable)
              • large values near constraint boundaries
              • problems with multiple valid answers: pick ONE canonical answer and verify it

            RULE 6 — COMPARISON CORRECTNESS
            Match the comparison method EXACTLY to the return type:
              int / long / bool / char           → direct  ==
              float / double                     → Math.abs(result - expected) < 1e-9  (or language equivalent)
              String                             → .equals() in Java,  == in Python/JS,  strcmp==0 in C
              array / list (ORDER MATTERS)       → element-by-element equality
              array / list (ORDER IRRELEVANT)    → sort BOTH then element-by-element
              ListNode (linked-list return)      → traverse both node-by-node, compare .val
              TreeNode (tree return)             → recursive structural equality
            For Java arrays use Arrays.equals(). For Java Lists use List.equals().
            NEVER compare objects with == in Java/Python when value equality is needed.

            RULE 7 — CUSTOM DATA STRUCTURES (CRITICAL FOR USER EXPERIENCE)
            When the problem uses a custom type (ListNode, TreeNode, GraphNode, Interval, etc.):

            A. Define the type ONCE in the harness, BEFORE USER_CODE_START.
               This is the real definition used by the runner.

            B. IMMEDIATELY AFTER the // USER_CODE_START line, copy the SAME definition
               as a COMMENTED-OUT block. This shows the user what type they are working with
               inside their editor — exactly like LeetCode does.

            C. The solve() function signature must USE the custom type in its parameters
               and/or return type.

            Java example (LinkedList):
              class ListNode { int val; ListNode next; ListNode(int v){val=v;next=null;} }
              public class Main {
                  // USER_CODE_START
                  // class ListNode {
                  //     int val; ListNode next;
                  //     ListNode(int v) { val = v; next = null; }
                  // }
                  public static ListNode solve(ListNode head) { return null; }
                  // USER_CODE_END

            RULE 8 — HELPER FUNCTIONS (invisible to user)
            Place ALL helper functions (buildList, buildTree, listsEqual, treeEqual, etc.)
            AFTER USER_CODE_END. The user never sees or writes these.

            RULE 9 — FAIL MESSAGE REPRESENTATION
            Arrays  : [1,2,3]   Strings : "hello"   null : null
            Linked list in FAIL: print as [v1,v2,v3] by traversing nodes.

            RULE 10 — ARRAY COMPARISON FOR "ANY VALID ANSWER" PROBLEMS
            If the problem says "return any valid permutation / any valid path / order does not matter",
            sort both result and expected before comparing.

            ════════════════════════════════════════
            SECTION 4 — LANGUAGE TEMPLATES
            ════════════════════════════════════════

            ── JAVA ──
            import java.util.*;
            public class Main {
                // [Custom type definitions here]
                // USER_CODE_START
                // [Commented type definition for user reference]
                public static <ReturnType> solve(<Params>) { return <default>; }
                // USER_CODE_END
                // [Helpers: buildList, listsEqual, buildTree, treesEqual, etc.]
                static void test(<Params>, <ReturnType> expected, int tc, boolean hidden) {
                    <ReturnType> result = solve(<callArgs>);
                    boolean ok = <correctComparison>;
                    if (ok) System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
                    else if (hidden) System.out.println("TC:" + tc + ":FAIL:hidden");
                    else System.out.println("TC:" + tc + ":FAIL:input=" + <inputRepr> + ":expected=" + <fmtExpected> + ":got=" + <fmtResult>);
                }
                public static void main(String[] args) {
                    test(... , 1, false); // tc1 visible
                    test(... , 2, false);
                    test(... , 3, false);
                    test(... , 4, false);
                    test(... , 5, true);  // tc5 hidden
                    test(... , 6, true);  // tc6 hidden
                }
            }

            ── CPP ──
            #include <bits/stdc++.h>
            using namespace std;
            // USER_CODE_START
            <ReturnType> solve(<Params>) { return <default>; }
            // USER_CODE_END
            void test(<Params>, <ReturnType> expected, int tc, bool hidden) { ... }
            int main() { /* 6 test() calls */ return 0; }

            ── C ──
            #include <stdio.h> #include <stdlib.h> #include <string.h>
            // USER_CODE_START
            <ReturnType> solve(<Params>) { return <default>; }
            // USER_CODE_END
            void test(...) { ... }
            int main() { /* 6 test() calls */ return 0; }

            ── PYTHON ──
            # USER_CODE_START
            def solve(<params>): pass
            # USER_CODE_END
            def test(<params>, expected, tc, hidden): ...
            # 6 test() calls

            ── JAVASCRIPT ──
            // USER_CODE_START
            function solve(<params>) { return <default>; }
            // USER_CODE_END
            function test(<params>, expected, tc, hidden) { ... }
            // 6 test() calls

            ════════════════════════════════════════
            SECTION 5 — REQUIRED JSON STRUCTURE
            ════════════════════════════════════════
            {
              "problem": {
                "title": "string",
                "description": "string",
                "inputFormat": "string",
                "outputFormat": "string",
                "constraints": "string",
                "timeLimit": 5.0,
                "memoryLimit": 256,
                "level": "MEDIUM",
                "example1": "Input: ...\\nOutput: ...",
                "example2": "Input: ...\\nOutput: ...",
                "example3": "Input: ...\\nOutput: ..."
              },
              "snippets": {
                "JAVA": "complete harness as a single JSON string with \\\\n line separators",
                "CPP": "complete harness",
                "C": "complete harness",
                "PYTHON": "complete harness",
                "JAVASCRIPT": "complete harness"
              }
            }
            """;
    }
}
