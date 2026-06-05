package com.example.codecombat2026.controller;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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

    private static final String NVIDIA_API_URL  = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String MODEL_KIMI      = "moonshotai/kimi-k2.6";
    private static final String MODEL_QWEN      = "qwen/qwen3-coder-480b-a35b-instruct";
    private static final String MODEL_DEEPSEEK  = "deepseek-ai/deepseek-v4-pro";

    // 20-minute read timeout — two AI passes can take up to ~16 min total
    private final RestTemplate restTemplate = createRestTemplate();
    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10_000);
        f.setReadTimeout(1_200_000);
        return new RestTemplate(f);
    }

    // Lenient parser: tolerates literal newlines / non-standard escapes in AI JSON
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
        if ("qwen".equalsIgnoreCase(modelParam)) {
            return new ModelConfig(MODEL_QWEN, deepseekApiKey, Map.of());
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
                Map<String, Object> fixed = verifyTestCases(cfg, snippets, query);
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
                                                Map<String, Object> snippets,
                                                String originalQuery) throws Exception {
        StringBuilder prompt = new StringBuilder(4096);
        prompt.append("You generated harnesses for this problem: \"").append(originalQuery).append("\"\n\n");
        prompt.append("CRITICAL TASK: For EACH test case in main(), independently compute the\n");
        prompt.append("CORRECT expected value by hand-tracing the algorithm on the given input.\n");
        prompt.append("Do NOT trust existing expected values — recompute every single one from scratch.\n\n");
        prompt.append("INDEXING RULE: If the problem says \"1-indexed\", \"added by one\", or \"indices start at 1\",\n");
        prompt.append("then array indices in the answer must be 1-based (first element = 1, not 0).\n");
        prompt.append("If the problem says \"0-indexed\" or nothing, use 0-based.\n\n");
        prompt.append("ARRAY ELEMENT RULE: The expected output can ONLY contain values that\n");
        prompt.append("actually exist in the input array (for index/element problems).\n");
        prompt.append("NEVER invent values not present in the input. If input is [-4,-2,0,2],\n");
        prompt.append("the output cannot reference -8, -6, or any value outside that array.\n\n");
        prompt.append("VERIFICATION STEPS for each test case:\n");
        prompt.append("1. Write out the input values explicitly — list every element.\n");
        prompt.append("2. Hand-simulate the algorithm step-by-step using ONLY those elements.\n");
        prompt.append("3. Verify each output element EXISTS in the input array.\n");
        prompt.append("4. Apply the correct indexing convention.\n");
        prompt.append("5. Replace the expected value in the test() call if it was wrong.\n\n");
        prompt.append("Keep harness structure, markers, and helper functions IDENTICAL.\n");
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
            - Newlines inside harness strings → \\n (backslash + n, two chars).
            - Quotes inside strings → \\" (backslash + quote).

            ════════════════════════════════════════
            SECTION 2 — PROBLEM METADATA
            ════════════════════════════════════════
            timeLimit  : Double (SECONDS)  EASY=3.0  MEDIUM=5.0  HARD=8.0
            memoryLimit: Integer (MB)      128 – 512
            level      : "EASY" | "MEDIUM" | "HARD"

            ════════════════════════════════════════
            SECTION 3 — HARNESS RULES (FOLLOW EVERY RULE)
            ════════════════════════════════════════

            ── RULE 1 · SELF-CONTAINED, NO STDIN ─────────────────────────────
            Every harness is a single complete runnable file.
            ZERO stdin: no Scanner, no cin/scanf, no input(), no readline(), no readFileSync.
            All test inputs are hardcoded constants inside main().

            ── RULE 2 · MARKERS ──────────────────────────────────────────────
            Exactly ONE USER_CODE_START / USER_CODE_END pair per file.
            Java / C / C++ / JS : // USER_CODE_START   and   // USER_CODE_END
            Python               : # USER_CODE_START    and   # USER_CODE_END
            Each marker MUST be on its OWN line — no code on the same line as a marker.

            ── RULE 2B · solve() STUB ONLY — NON-NEGOTIABLE ──────────────────
            solve() inside the markers MUST return a default value only.
            NEVER put the working algorithm inside solve(). User writes the solution.
            NEVER prefix solve() with // or # — it must be REAL, COMPILABLE code.
            Only custom type comments (e.g. // class ListNode{...}) use // inside markers.
            The solve() declaration itself is ALWAYS uncommented real code.
            Correct stubs:
              Java:   public static int solve(...)     { return 0; }
              Java:   public static int[] solve(...)   { return new int[0]; }
              Java:   public static String solve(...)  { return ""; }
              Java:   public static boolean solve(...) { return false; }
              Java:   public static ListNode solve(...){ return null; }
              C++:    int solve(...)          { return 0; }
              C++:    vector<int> solve(...)  { return {}; }
              C:      int solve(...)          { return 0; }   (arrays → see RULE 11)
              Python: def solve(...): return 0   |  return []  |  return ""
              JS:     function solve(...) { return 0; }   |   { return []; }
            FORBIDDEN — never write the algorithm inside solve().
            FORBIDDEN — never put // or # in front of the solve() declaration.

            ── RULE 3 · OUTPUT FORMAT (character-perfect) ────────────────────
            Visible PASS : TC:N:PASS
            Hidden  PASS : TC:N:PASS:hidden
            Visible FAIL : TC:N:FAIL:input=<repr>:expected=<val>:got=<val>
            Hidden  FAIL : TC:N:FAIL:hidden
            N is 1-based. No spaces around colons. Newline after each line.
            Multi-param input: join all params with comma inside input=
              e.g. solve(int[] nums, int target) → input=[2,7,11,15],9

            ── RULE 4 · TEST COUNT ───────────────────────────────────────────
            Exactly 4 visible + 2 hidden = 6 total.
            Hidden tests NEVER print input, expected, or got — only TC:N:FAIL:hidden.

            ── RULE 5 · TEST CASE CORRECTNESS (MOST CRITICAL) ───────────────
            For EACH test case: trace the algorithm step-by-step from scratch,
            compute the expected output by hand, verify it, then encode it.
            ONE wrong expected value = correct code fails. Unacceptable.
            Cover diverse inputs:
              • empty / null input (when constraints allow)
              • single-element input
              • all-same elements
              • already-sorted / reverse-sorted arrays
              • negatives and zero (when applicable)
              • large values near constraint boundaries
              • problems with multiple valid answers: pick ONE canonical output

            ── RULE 5B · INDEXING CONVENTION ────────────────────────────────
            Read the problem statement before writing ANY test case.
            "1-indexed" / "added by one" (e.g. LC167 Two Sum II):
              expected MUST be 1-based → [2,7,11,15] target=9 → [1,2]  NOT [0,1]
            Standard array problems: 0-based unless the problem says otherwise.
            NEVER assume indexing — always derive it from the problem statement.

            ── RULE 6 · COMPARISON METHOD (match type exactly) ──────────────
            int / long / char / boolean  →  ==
            float / double               →  abs(result-expected) < 1e-9
            String                       →  .equals() Java | == Python/JS | strcmp==0 C
            int[] / long[]               →  Arrays.equals() Java | element loop C/C++/JS
            List<Integer>                →  .equals() Java | == Python | array compare JS
            int[][]  (2D)                →  row-by-row Arrays.equals() in a loop
            ListNode (linked list)       →  traverse node-by-node, compare .val
            TreeNode (binary tree)       →  recursive: val == && left== && right==
            NEVER use == for arrays or objects in Java.
            NEVER use == for lists/objects in Python.

            ── RULE 7 · CUSTOM DATA STRUCTURES ──────────────────────────────
            When the problem needs ListNode, TreeNode, or any custom class:
            A. Define the type ONCE before USER_CODE_START (real runner definition).
            B. Copy the SAME definition as comments immediately INSIDE USER_CODE_START
               (user sees this in their editor — exactly like LeetCode).
            C. solve() signature MUST use the custom type.

            Java — ListNode:
              class ListNode { int val; ListNode next; ListNode(int v){val=v;next=null;} }
              public class Main {
                  // USER_CODE_START
                  // class ListNode { int val; ListNode next; ListNode(int v){val=v;next=null;} }
                  public static ListNode solve(ListNode head) { return null; }
                  // USER_CODE_END

            Java — TreeNode:
              class TreeNode { int val; TreeNode left, right; TreeNode(int v){val=v;} }
              public class Main {
                  // USER_CODE_START
                  // class TreeNode { int val; TreeNode left, right; TreeNode(int v){val=v;} }
                  public static TreeNode solve(TreeNode root) { return null; }
                  // USER_CODE_END

            C++ — struct:
              struct ListNode { int val; ListNode* next; ListNode(int v):val(v),next(nullptr){} };
              // USER_CODE_START
              // struct ListNode { int val; ListNode* next; ListNode(int v):val(v),next(nullptr){} };
              ListNode* solve(ListNode* head) { return nullptr; }
              // USER_CODE_END

            C — struct:
              struct ListNode { int val; struct ListNode* next; };
              // USER_CODE_START
              /* struct ListNode { int val; struct ListNode* next; }; */
              struct ListNode* solve(struct ListNode* head) { return NULL; }
              // USER_CODE_END

            Python — class:
              class ListNode:
                  def __init__(self, val=0, next=None): self.val=val; self.next=next
              # USER_CODE_START
              # class ListNode:
              #     def __init__(self, val=0, next=None): self.val=val; self.next=next
              def solve(head): return None
              # USER_CODE_END

            JavaScript — class:
              class ListNode { constructor(val=0,next=null){this.val=val;this.next=next;} }
              // USER_CODE_START
              // class ListNode { constructor(val=0,next=null){this.val=val;this.next=next;} }
              function solve(head) { return null; }
              // USER_CODE_END

            ── RULE 8 · HELPER FUNCTIONS (AFTER USER_CODE_END, hidden from user) ──
            For linked lists : buildList(int[]) → ListNode  and  listsEqual(a,b) → bool
            For trees        : buildTree(int[]) → TreeNode (level-order, use null/INT_MIN for missing)
                               treesEqual(a,b)  → bool (recursive: val + left + right)
            For 2D arrays    : arrays2DEqual(a,b) or row-by-row loop
            All helpers go AFTER USER_CODE_END. User never sees or writes these.

            ── RULE 9 · FAIL MESSAGE FORMAT ─────────────────────────────────
            int / long / bool → the value as-is
            float / double    → formatted to reasonable precision
            String            → "hello"   (with double quotes)
            null              → null
            int[] / List      → [1,2,3]   (brackets, comma-separated, no spaces)
            ListNode chain    → [v1,v2,v3]  (traverse and format)
            2D array          → [[1,2],[3,4]]
            Multi-param input → join all args with comma: [2,7,11,15],9

            ── RULE 10 · "ANY VALID ANSWER" PROBLEMS ────────────────────────
            If "any valid permutation / any order is acceptable":
            Sort BOTH result and expected before comparing. Never fail a correct reordering.

            ── RULE 11 · C ARRAY RETURNS ────────────────────────────────────
            C cannot return bare arrays. Use a returnSize pointer parameter:
              int* solve(int* nums, int numsSize, int target, int* returnSize) {
                  *returnSize = 0;
                  return NULL;   // stub
              }
            test() must pass &returnSize, use the returned pointer to check values,
            and free() the pointer after checking.
            For in-place problems (rotate, reverse): use void solve(int* nums, int n) { }
            and check the modified array in test().

            ════════════════════════════════════════
            SECTION 4 — LANGUAGE TEMPLATES
            ════════════════════════════════════════

            ── JAVA ──
            import java.util.*;
            public class Main {
                // [Custom type definitions — before USER_CODE_START]
                // USER_CODE_START
                // [Commented copy of custom type — shown to user]
                public static <ReturnType> solve(<Params>) { return <default>; }
                // USER_CODE_END
                // [Helpers: buildList, listsEqual, buildTree, treesEqual, fmt helpers]
                static <fmtHelper if needed>
                static void test(<Params>, <ReturnType> expected, int tc, boolean hidden) {
                    <ReturnType> result = solve(<callArgs>);
                    boolean ok = <correctComparison>;
                    if (ok) System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
                    else if (hidden) System.out.println("TC:" + tc + ":FAIL:hidden");
                    else System.out.println("TC:" + tc + ":FAIL:input=" + <inputRepr>
                                            + ":expected=" + <fmtExpected> + ":got=" + <fmtResult>);
                }
                public static void main(String[] args) {
                    test(<args>, <expected>, 1, false);
                    test(<args>, <expected>, 2, false);
                    test(<args>, <expected>, 3, false);
                    test(<args>, <expected>, 4, false);
                    test(<args>, <expected>, 5, true);
                    test(<args>, <expected>, 6, true);
                }
            }

            ── C++ ──
            #include <bits/stdc++.h>
            using namespace std;
            // [struct definitions if needed]
            // USER_CODE_START
            // [Commented struct if needed]
            <ReturnType> solve(<Params>) { return <default>; }
            // USER_CODE_END
            // [helpers + fmt lambdas]
            void test(<Params>, <ReturnType> expected, int tc, bool hidden) {
                auto result = solve(<callArgs>);
                bool ok = <correctComparison>;
                if (ok) cout << "TC:" << tc << ":PASS" << (hidden?":hidden":"") << "\\n";
                else if (hidden) cout << "TC:" << tc << ":FAIL:hidden\\n";
                else cout << "TC:" << tc << ":FAIL:input=" << <inputRepr>
                          << ":expected=" << <fmtExpected> << ":got=" << <fmtResult> << "\\n";
            }
            int main() { /* 6 test() calls */ return 0; }

            ── C ──
            #include <stdio.h>
            #include <stdlib.h>
            #include <string.h>
            // [struct definitions — BEFORE USER_CODE_START]
            // USER_CODE_START
            /* [Commented struct if needed] */
            <ReturnType> solve(<Params>) { return <default>; }  // arrays: see RULE 11
            // USER_CODE_END
            // [helpers: buildList, listsEqual, printArr, etc.]
            void test(<testParams>) {
                <ReturnType> result = solve(<callArgs>);
                int ok = <correctComparison>;
                if (ok) printf(hidden ? "TC:%d:PASS:hidden\\n" : "TC:%d:PASS\\n", tc);
                else if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
                else printf("TC:%d:FAIL:input=...:expected=...:got=...\\n", tc);
            }
            int main() { /* 6 test() calls */ return 0; }

            ── PYTHON ──
            # [class definitions if needed]
            # USER_CODE_START
            # [Commented class if needed]
            def solve(<params>): return <default>    # NOT pass — must return a value
            # USER_CODE_END
            # [helpers: build_list, lists_equal, build_tree, trees_equal]
            def test(<params>, expected, tc, hidden):
                result = solve(<callArgs>)
                ok = <correctComparison>
                if ok: print(f"TC:{tc}:PASS{':hidden' if hidden else ''}")
                elif hidden: print(f"TC:{tc}:FAIL:hidden")
                else: print(f"TC:{tc}:FAIL:input=<repr>:expected={expected}:got={result}")
            # 6 test() calls

            ── JAVASCRIPT ──
            // [class definitions if needed]
            // USER_CODE_START
            // [Commented class if needed]
            function solve(<params>) { return <default>; }
            // USER_CODE_END
            // [helpers: buildList, listsEqual, buildTree, treesEqual]
            function test(<params>, expected, tc, hidden) {
                const result = solve(<callArgs>);
                const ok = <correctComparison>;
                if (ok) console.log(`TC:${tc}:PASS${hidden?':hidden':''}`);
                else if (hidden) console.log(`TC:${tc}:FAIL:hidden`);
                else console.log(`TC:${tc}:FAIL:input=<repr>:expected=${JSON.stringify(expected)}:got=${JSON.stringify(result)}`);
            }
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
                "JAVA":       "complete harness — newlines as \\n",
                "CPP":        "complete harness",
                "C":          "complete harness",
                "PYTHON":     "complete harness",
                "JAVASCRIPT": "complete harness"
              }
            }
            """;
    }
}
