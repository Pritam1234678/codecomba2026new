package com.example.codecombat2026.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/problems")
@PreAuthorize("hasRole('ADMIN')")
public class AiProblemGeneratorController {

    @Value("${NVIDIA_API_KEY:}")
    private String nvidiaApiKey;

    private static final String NVIDIA_API_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String MODEL = "moonshotai/kimi-k2.6";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/ai-generate")
    public ResponseEntity<?> generate(@RequestBody Map<String, String> request) {
        if (nvidiaApiKey == null || nvidiaApiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "NVIDIA_API_KEY not configured"));
        }

        String query = request.getOrDefault("query", "").trim();
        if (query.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "query is required"));
        }

        Map<String, Object> payload = Map.of(
            "model", MODEL,
            "messages", List.of(
                Map.of("role", "system", "content", buildSystemPrompt()),
                Map.of("role", "user", "content",
                    "Generate a complete CodeCombat problem for: " + query + "\n\n" +
                    "OUTPUT ONLY the raw JSON object. No markdown fences, no ```json, " +
                    "no explanation, no preamble. Start your response with { and end with }.")
            ),
            "max_tokens", 16384,
            "temperature", 0.2,
            "top_p", 0.9,
            "stream", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(nvidiaApiKey);

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
            You are a competitive programming problem generator for CodeCombat 2026.
            Given a LeetCode problem name or number, generate a COMPLETE problem definition
            plus all 5 language harness code snippets.

            # STRICT OUTPUT RULES
            - Respond with ONLY a single raw JSON object
            - NO markdown code fences (no ```json or ```)
            - NO preamble, NO explanation, NO text outside the JSON
            - Start with { and end with }
            - All string values must use proper JSON escaping (\\n for newlines, \\" for quotes)

            # Database Schema
            - timeLimit: Double (seconds) — EASY=3.0, MEDIUM=5.0, HARD=8.0
            - memoryLimit: Integer (MB) — 128 to 512
            - level: "EASY" | "MEDIUM" | "HARD"

            # Harness Format Rules (CRITICAL)
            1. Single complete runnable file — NO stdin (no Scanner, cin, scanf, input(), readFileSync)
            2. MUST contain: // USER_CODE_START and // USER_CODE_END markers
               (Python: # USER_CODE_START / # USER_CODE_END)
            3. Test cases HARDCODED in harness. Output per test:
               PASS visible:  TC:N:PASS
               PASS hidden:   TC:N:PASS:hidden
               FAIL visible:  TC:N:FAIL:input=<repr>:expected=<val>:got=<val>
               FAIL hidden:   TC:N:FAIL:hidden
            4. Exactly 8 visible + 2 hidden test cases (10 total)
            5. Hidden tests NEVER expose input/expected/got

            # REQUIRED JSON STRUCTURE
            {
              "problem": {
                "title": "...",
                "description": "...",
                "inputFormat": "...",
                "outputFormat": "...",
                "constraints": "...",
                "timeLimit": 5.0,
                "memoryLimit": 256,
                "level": "MEDIUM",
                "example1": "Input: ...\\nOutput: ...",
                "example2": "Input: ...\\nOutput: ...",
                "example3": "Input: ...\\nOutput: ..."
              },
              "snippets": {
                "JAVA": "import java.util.*;\\npublic class Main {\\n    // USER_CODE_START\\n    ...\\n    // USER_CODE_END\\n    ...\\n}",
                "CPP": "#include <bits/stdc++.h>\\nusing namespace std;\\n// USER_CODE_START\\n...\\n// USER_CODE_END\\n...",
                "C": "#include <stdio.h>\\n// USER_CODE_START\\n...\\n// USER_CODE_END\\n...",
                "PYTHON": "# USER_CODE_START\\ndef solve(...):\\n    return ...\\n# USER_CODE_END\\n...",
                "JAVASCRIPT": "// USER_CODE_START\\nfunction solve(...) { return ...; }\\n// USER_CODE_END\\n..."
              }
            }

            JAVA harness template:
            import java.util.*;
            public class Main {
                // USER_CODE_START
                public static <returnType> solve(<params>) { return <default>; }
                // USER_CODE_END
                static void test(<params>, <expected>, int tc, boolean hidden) {
                    <returnType> result = solve(<args>);
                    if (<equals check>) {
                        System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
                    } else if (hidden) {
                        System.out.println("TC:" + tc + ":FAIL:hidden");
                    } else {
                        System.out.println("TC:" + tc + ":FAIL:input=<repr>:expected=" + expected + ":got=" + result);
                    }
                }
                public static void main(String[] args) {
                    test(<tc1args>, 1, false); ... test(<tc8args>, 8, false);
                    test(<tc9args>, 9, true);  test(<tc10args>, 10, true);
                }
            }

            CPP template: use #include <bits/stdc++.h>, USER_CODE_START/END outside class, cout for output.
            C template: use #include <stdio.h>, USER_CODE_START/END, printf for output.
            PYTHON template: # USER_CODE_START/END, print(f"TC:{tc}:...") for output.
            JAVASCRIPT template: // USER_CODE_START/END, console.log(`TC:${tc}:...`) for output.
            """;
    }
}
