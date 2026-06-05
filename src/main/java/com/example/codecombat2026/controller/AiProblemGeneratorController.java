package com.example.codecombat2026.controller;

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
    private String kimiApiKey;          // Kimi K2.6

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepseekApiKey;      // DeepSeek V4 Pro

    private static final String NVIDIA_API_URL = "https://integrate.api.nvidia.com/v1/chat/completions";

    private static final String MODEL_KIMI      = "moonshotai/kimi-k2.6";
    private static final String MODEL_DEEPSEEK  = "deepseek-ai/deepseek-v4-pro";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Per-model configuration resolved from the request's {@code model} field. */
    private record ModelConfig(String modelId, String apiKey, Map<String, Object> extra) {}

    private ModelConfig resolveModel(String modelParam) {
        if ("deepseek".equalsIgnoreCase(modelParam)) {
            return new ModelConfig(
                MODEL_DEEPSEEK,
                deepseekApiKey,
                Map.of("chat_template_kwargs", Map.of("thinking", false))
            );
        }
        // default → Kimi K2.6
        return new ModelConfig(MODEL_KIMI, kimiApiKey, Map.of());
    }

    @PostMapping("/ai-generate")
    public ResponseEntity<?> generate(@RequestBody Map<String, String> request) {
        String modelParam = request.getOrDefault("model", "kimi");
        ModelConfig cfg = resolveModel(modelParam);

        if (cfg.apiKey() == null || cfg.apiKey().isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "API key for model '" + modelParam + "' is not configured"));
        }

        String query = request.getOrDefault("query", "").trim();
        if (query.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "query is required"));
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model",       cfg.modelId());
        payload.put("messages",    List.of(
            Map.of("role", "system", "content", buildSystemPrompt()),
            Map.of("role", "user",   "content",
                "Generate a complete CodeCombat problem for: " + query + "\n\n" +
                "OUTPUT ONLY the raw JSON object. No markdown fences, no ```json, " +
                "no explanation, no preamble. Start your response with { and end with }.")
        ));
        payload.put("max_tokens",  16384);
        payload.put("temperature", 0.2);
        payload.put("top_p",       0.9);
        payload.put("stream",      false);
        payload.putAll(cfg.extra());   // model-specific extras (e.g. chat_template_kwargs)

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cfg.apiKey());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                NVIDIA_API_URL, HttpMethod.POST,
                new HttpEntity<>(payload, headers), Map.class
            );

            Map body = response.getBody();
            if (body == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Empty response from AI"));
            }

            List<Map> choices = (List<Map>) body.get("choices");
            if (choices == null || choices.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "No choices in AI response"));
            }

            String content = (String) ((Map) choices.get(0).get("message")).get("content");
            if (content == null || content.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Empty content from AI"));
            }

            // Extract the outermost JSON object — handles any leading/trailing text or fences
            String json = extractJsonObject(content);

            try {
                Map<String, Object> result = objectMapper.readValue(json, Map.class);
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "AI returned invalid JSON: " + e.getMessage(),
                                 "raw", json.length() > 500 ? json.substring(0, 500) + "..." : json));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "AI call failed: " + e.getMessage()));
        }
    }

    /**
     * Extracts the outermost {...} block from arbitrary text.
     * Correctly handles { and } inside JSON string values by tracking quote state.
     */
    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        if (start < 0) return text.trim();

        boolean inString = false;
        boolean escaped  = false;
        int depth = 0;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) { escaped = false; continue; }
            if (c == '\\' && inString) { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }

            if (!inString) {
                if      (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) return text.substring(start, i + 1); }
            }
        }
        // Unbalanced — return everything from first { as best-effort
        return text.substring(start);
    }

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

            RULE 3 — OUTPUT FORMAT (character-perfect, no extra spaces)
            Visible PASS : TC:N:PASS
            Hidden  PASS : TC:N:PASS:hidden
            Visible FAIL : TC:N:FAIL:input=<repr>:expected=<val>:got=<val>
            Hidden  FAIL : TC:N:FAIL:hidden
            N is 1-based. No spaces around colons. Newline after each line.

            RULE 4 — TEST COUNT
            Exactly 8 visible + 2 hidden = 10 total.
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
               inside their editor — exactly like LeetCode does. Without this, the user cannot
               write correct code because they cannot see the struct/class definition.

            C. The solve() function signature must USE the custom type in its parameters
               and/or return type.

            Java example (LinkedList):
              class ListNode { int val; ListNode next; ListNode(int v){val=v;next=null;} }
              public class Main {
                  // USER_CODE_START
                  // Definition for singly-linked list:
                  // class ListNode {
                  //     int val;
                  //     ListNode next;
                  //     ListNode(int v) { val = v; next = null; }
                  // }
                  public static ListNode solve(ListNode head) { return null; }
                  // USER_CODE_END
                  ...
              }

            C example (struct):
              struct ListNode { int val; struct ListNode* next; };
              // USER_CODE_START
              /* Definition for singly-linked list:
                 struct ListNode {
                     int val;
                     struct ListNode* next;
                 }; */
              struct ListNode* solve(struct ListNode* head) { return NULL; }
              // USER_CODE_END

            Python example (class):
              class ListNode:
                  def __init__(self, val=0, next=None):
                      self.val = val
                      self.next = next
              # USER_CODE_START
              # Definition for singly-linked list:
              # class ListNode:
              #     def __init__(self, val=0, next=None):
              #         self.val = val
              #         self.next = next
              def solve(head):
                  pass
              # USER_CODE_END

            JavaScript example (class):
              class ListNode { constructor(val=0,next=null){this.val=val;this.next=next;} }
              // USER_CODE_START
              // Definition for singly-linked list:
              // class ListNode {
              //     constructor(val = 0, next = null) {
              //         this.val = val;
              //         this.next = next;
              //     }
              // }
              function solve(head) { return null; }
              // USER_CODE_END

            RULE 8 — HELPER FUNCTIONS (invisible to user)
            Place ALL helper functions (buildList, buildTree, listsEqual, treeEqual, etc.)
            AFTER USER_CODE_END. The user never sees or writes these.
            For linked lists: provide buildList(array) and listsEqual(a,b).
            For trees: provide buildTree(array) (level-order, null for missing) and treesEqual(a,b).

            RULE 9 — FAIL MESSAGE REPRESENTATION
            Arrays  : [1,2,3]      (comma-separated, bracket-wrapped, no spaces)
            Strings : "hello"      (double-quoted)
            null    : null
            Linked list in FAIL: print as [v1,v2,v3] by traversing nodes.

            RULE 10 — ARRAY COMPARISON FOR "ANY VALID ANSWER" PROBLEMS
            If the problem says "return any valid permutation / any valid path / order does not matter",
            sort both result and expected before comparing so the test does not fail on a correct
            but differently-ordered answer.

            ════════════════════════════════════════
            SECTION 4 — LANGUAGE TEMPLATES
            ════════════════════════════════════════

            ── JAVA ──
            import java.util.*;
            public class Main {
                // [Custom type definitions here, e.g.: class ListNode {...}]
                // USER_CODE_START
                // [Commented type definition here for user reference]
                public static <ReturnType> solve(<Params>) { return <default>; }
                // USER_CODE_END
                // [Helper: buildList, listsEqual, buildTree, treesEqual, etc.]
                static void test(<Params>, <ReturnType> expected, int tc, boolean hidden) {
                    <ReturnType> result = solve(<callArgs>);
                    boolean ok = <correctComparison>;
                    if (ok) System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
                    else if (hidden) System.out.println("TC:" + tc + ":FAIL:hidden");
                    else System.out.println("TC:" + tc + ":FAIL:input=" + <inputRepr> + ":expected=" + <fmtExpected> + ":got=" + <fmtResult>);
                }
                public static void main(String[] args) {
                    test(... , 1, false); // tc1 visible
                    // ... tc2–tc8 visible
                    test(... , 9, true);  // tc9 hidden
                    test(... , 10, true); // tc10 hidden
                }
            }

            ── CPP ──
            #include <bits/stdc++.h>
            using namespace std;
            // [struct/class definitions if needed]
            // USER_CODE_START
            // [Commented struct definition for user reference]
            <ReturnType> solve(<Params>) { return <default>; }
            // USER_CODE_END
            // [helper functions]
            void test(<Params>, <ReturnType> expected, int tc, bool hidden) {
                <ReturnType> result = solve(<callArgs>);
                bool ok = <correctComparison>;
                if (ok) cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\\n";
                else if (hidden) cout << "TC:" << tc << ":FAIL:hidden\\n";
                else cout << "TC:" << tc << ":FAIL:input=" << <inputRepr> << ":expected=" << <fmtExpected> << ":got=" << <fmtResult> << "\\n";
            }
            int main() { /* 10 test() calls */ return 0; }

            ── C ──
            #include <stdio.h>
            #include <stdlib.h>
            #include <string.h>
            // [struct definitions MUST be here before USER_CODE_START]
            // USER_CODE_START
            /* [Commented struct definition — user MUST see this to write correct C code]
               struct Foo { int x; struct Foo* next; }; */
            <ReturnType> solve(<Params>) { return <default>; }
            // USER_CODE_END
            // [helper functions: buildList, listsEqual, etc.]
            void test(<Params>, <expected>, int tc, int hidden) {
                <ReturnType> result = solve(<callArgs>);
                int ok = <correctComparison>;
                if (ok) printf(hidden ? "TC:%d:PASS:hidden\\n" : "TC:%d:PASS\\n", tc);
                else if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
                else printf("TC:%d:FAIL:input=<repr>:expected=<val>:got=<val>\\n", tc, ...);
            }
            int main() { /* 10 test() calls */ return 0; }

            ── PYTHON ──
            # [class definitions if needed]
            # USER_CODE_START
            # [Commented class definition for user reference]
            def solve(<params>):
                pass
            # USER_CODE_END
            # [helper functions]
            def test(<params>, expected, tc, hidden):
                result = solve(<callArgs>)
                ok = <correctComparison>
                if ok: print(f"TC:{tc}:PASS{':hidden' if hidden else ''}")
                elif hidden: print(f"TC:{tc}:FAIL:hidden")
                else: print(f"TC:{tc}:FAIL:input=<repr>:expected={expected}:got={result}")
            # 10 test() calls

            ── JAVASCRIPT ──
            // [class definitions if needed]
            // USER_CODE_START
            // [Commented class definition for user reference]
            function solve(<params>) { return <default>; }
            // USER_CODE_END
            // [helper functions]
            function test(<params>, expected, tc, hidden) {
                const result = solve(<callArgs>);
                const ok = <correctComparison>;
                if (ok) console.log(`TC:${tc}:PASS${hidden ? ':hidden' : ''}`);
                else if (hidden) console.log(`TC:${tc}:FAIL:hidden`);
                else console.log(`TC:${tc}:FAIL:input=<repr>:expected=${expected}:got=${result}`);
            }
            // 10 test() calls

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
