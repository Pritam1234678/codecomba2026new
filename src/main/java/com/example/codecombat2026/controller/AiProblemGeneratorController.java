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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI problem generator — MULTI-PASS design.
 *
 * The old single-call approach asked the model to emit the whole problem PLUS all
 * five language harnesses inside one JSON object. On NVIDIA NIM that output blew
 * past the token budget and got truncated → "invalid JSON / unexpected end-of-input".
 * Embedding code inside JSON also made escaping fragile.
 *
 * New flow:
 *   Pass 1  — AI returns ONLY the problem spec: statement + function signature +
 *             6 canonical test cases as STRUCTURED data (small JSON, expected values
 *             computed once).
 *   Pass 2..6 — one call PER language, each returns RAW harness source (not wrapped
 *             in JSON, so no escaping issues), embedding the SAME test values.
 *
 * Every individual response is small → no truncation. Test values are computed a
 * single time in pass 1 → all five harnesses stay consistent. Works for custom
 * story-line problems too, not just LeetCode.
 */
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

    // Frontend expects snippets keyed in this exact order.
    private static final List<String> LANGS = List.of("JAVA", "CPP", "PYTHON", "JAVASCRIPT", "C");

    // 20-minute read timeout — up to 6 sequential AI calls.
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

        // ── Pass 1: problem spec (statement + signature + tests) ──────────────
        Map<String, Object> spec;
        try {
            String specRaw = callNim(cfg, List.of(
                Map.of("role", "system", "content", PASS1_SYSTEM),
                Map.of("role", "user", "content",
                    "Design the problem spec for: " + query + "\n\n" +
                    "Output ONLY the raw JSON spec object. Start with { and end with }.")
            ), 6144);
            spec = parseJsonObject(specRaw);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "AI failed (pass 1 — problem spec): " + e.getMessage()));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> problem = (Map<String, Object>) spec.get("problem");
        Object signature = spec.get("signature");
        Object tests     = spec.get("tests");
        if (problem == null || signature == null || tests == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "AI pass 1 returned an incomplete spec (missing problem/signature/tests)."));
        }

        // Compact spec string fed to every harness call so all languages share data.
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

        // ── Pass 2..6: one harness per language ───────────────────────────────
        Map<String, String> snippets = new LinkedHashMap<>();
        for (String lang : LANGS) {
            try {
                snippets.put(lang, generateHarness(cfg, lang, specForHarness));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "AI failed generating the " + lang + " harness: " + e.getMessage()));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("problem",  problem);
        result.put("snippets", snippets);
        return ResponseEntity.ok(result);
    }

    // ─── Per-language harness call (with one retry) ───────────────────────────

    private String generateHarness(ModelConfig cfg, String lang, String specJson) throws Exception {
        String system = harnessSystem(lang);
        String user   = "Spec:\n" + specJson + "\n\nOutput ONLY the raw " + lang +
                        " harness source code. No markdown fences, no explanation.";
        Exception last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String code = callNim(cfg, List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user",   "content", user)
                ), 4096);
                return stripCodeFences(code);
            } catch (Exception e) {
                last = e;
            }
        }
        throw last;
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

        Map choice = choices.get(0);
        String finishReason = (String) choice.get("finish_reason");
        String content = (String) ((Map) choice.get("message")).get("content");
        if (content == null || content.isBlank()) throw new RuntimeException("Empty content from AI");

        if ("length".equals(finishReason)) {
            throw new RuntimeException("output truncated (model hit its token limit). Try the DeepSeek model.");
        }
        return content;
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
          "tests": [
            { "args": { "paramName": <value> }, "expected": <value>, "hidden": false },
            ... EXACTLY 6 entries: the first 4 have hidden=false, the last 2 have hidden=true
          ]
        }

        Allowed <type> values ONLY: int, long, double, boolean, String, char,
        int[], long[], double[], boolean[], String[], int[][].

        CORRECTNESS RULES (most important):
        • Mentally SOLVE the problem and compute every "expected" by hand, then re-check each one.
        • Each "expected" must follow ONLY from that test's own "args". NEVER use a number or
          element that does not appear in / derive from that test's input.
        • "args" keys must EXACTLY equal the param names declared in "signature".
        • Pick 6 meaningful tests including edge cases (empty, single element, all equal,
          negatives, maximum) wherever the constraints allow them.
        • Values are JSON literals: arrays like [1,2,3], strings like "abc", booleans true/false.

        OUTPUT: only the JSON object. First character {, last character }. No markdown, no comments.
        """;

    /** Per-language harness system prompt = shared contract + a worked anchor example. */
    private String harnessSystem(String lang) {
        String contract = """
            You generate ONE complete %LANG% test-harness file for the CodeCombat 2026 judge.
            INPUT: a JSON spec containing the function `signature` and `tests` (each test has
            args, expected, hidden). OUTPUT: ONLY the raw %LANG% source code — no markdown
            fences, no JSON, no commentary.

            Harness contract:
            • One self-contained, runnable file. NO stdin (no Scanner/cin/scanf/input()/readFileSync).
              Hardcode the test data taken from the spec.
            • Wrap ONLY the solution stub with the markers (Python/C-style comments shown below). The
              stub is the `signature` function with an empty body returning a default value — this is
              the part the user will replace.
            • A test(...) helper is called once per spec test, numbered 1..6 in spec order, receiving
              the hidden flag. Each test prints EXACTLY one line:
                 visible pass:  TC:<n>:PASS
                 hidden  pass:  TC:<n>:PASS:hidden
                 visible fail:  TC:<n>:FAIL:input=<args repr>:expected=<exp>:got=<got>
                 hidden  fail:  TC:<n>:FAIL:hidden
            • Compare scalars with ==; compare arrays element-wise. Hidden tests must NEVER print
              input/expected/got. Compact code, minimal blank lines.

            Match the structure of this worked example exactly (adapt names, types and values to the spec):
            """.replace("%LANG%", lang);
        return contract + "\n" + harnessExample(lang);
    }

    /** Worked anchor example per language (int return, int[] param — adapt to real signature). */
    private String harnessExample(String lang) {
        return switch (lang) {
            case "JAVA" -> """
                import java.util.*;
                public class Main {
                    // USER_CODE_START
                    public static int solve(int[] arr) { return 0; }
                    // USER_CODE_END
                    static void test(int[] arr, int expected, int tc, boolean hidden) {
                        int got = solve(arr);
                        if (got == expected) System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
                        else if (hidden) System.out.println("TC:" + tc + ":FAIL:hidden");
                        else System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(arr) + ":expected=" + expected + ":got=" + got);
                    }
                    public static void main(String[] a) {
                        test(new int[]{1,2,3}, 6, 1, false);
                        test(new int[]{5}, 5, 2, false);
                        // ...4 visible then 2 hidden (hidden=true)
                    }
                }
                """;
            case "CPP" -> """
                #include <bits/stdc++.h>
                using namespace std;
                // USER_CODE_START
                int solve(vector<int>& arr) { return 0; }
                // USER_CODE_END
                void test(vector<int> arr, int expected, int tc, bool hidden=false) {
                    int got = solve(arr);
                    if (got == expected) cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\\n";
                    else if (hidden) cout << "TC:" << tc << ":FAIL:hidden\\n";
                    else { cout << "TC:" << tc << ":FAIL:input=["; for (size_t i=0;i<arr.size();i++){ if(i) cout<<","; cout<<arr[i]; } cout << "]:expected=" << expected << ":got=" << got << "\\n"; }
                }
                int main() {
                    test({1,2,3}, 6, 1);
                    test({5}, 5, 2);
                    return 0;
                }
                """;
            case "C" -> """
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
                int main() {
                    int t1[]={1,2,3}; test(t1, 3, 6, 1, 0);
                    int t2[]={5};     test(t2, 1, 5, 2, 0);
                    return 0;
                }
                """;
            case "PYTHON" -> """
                # USER_CODE_START
                def solve(arr):
                    return 0
                # USER_CODE_END
                def test(arr, expected, tc, hidden=False):
                    got = solve(arr)
                    if got == expected:
                        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
                    elif hidden:
                        print(f"TC:{tc}:FAIL:hidden")
                    else:
                        print(f"TC:{tc}:FAIL:input={arr}:expected={expected}:got={got}")
                test([1,2,3], 6, 1)
                test([5], 5, 2)
                """;
            case "JAVASCRIPT" -> """
                // USER_CODE_START
                function solve(arr) { return 0; }
                // USER_CODE_END
                function test(arr, expected, tc, hidden = false) {
                    const got = solve(arr);
                    if (got === expected) console.log(`TC:${tc}:PASS` + (hidden ? ':hidden' : ''));
                    else if (hidden) console.log(`TC:${tc}:FAIL:hidden`);
                    else console.log(`TC:${tc}:FAIL:input=[${arr}]:expected=${expected}:got=${got}`);
                }
                test([1,2,3], 6, 1);
                test([5], 5, 2);
                """;
            default -> "";
        };
    }
}
