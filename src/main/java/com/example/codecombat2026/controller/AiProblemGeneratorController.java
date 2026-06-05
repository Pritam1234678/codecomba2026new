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
            ), 32768);
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

        return ResponseEntity.ok(result);
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
            You generate complete CodeCombat 2026 problem definitions + 5-language harnesses
            following the exact format in PROBLEM_GUIDE.md.

            OUTPUT: ONE raw JSON object — first char {, last char }. No markdown, no preamble.
            Newlines in strings → \\n   Quotes in strings → \\"

            JSON:
            {
              "problem": {"title","description","inputFormat","outputFormat","constraints",
                          "timeLimit"(double seconds, EASY=3 MEDIUM=5 HARD=8),
                          "memoryLimit"(int MB, 128-512),
                          "level"("EASY"|"MEDIUM"|"HARD"),
                          "example1","example2","example3"},
              "snippets": {"JAVA","CPP","C","PYTHON","JAVASCRIPT"}
            }

            ════ HARNESS FORMAT (5 languages, follow templates exactly) ════

            Each harness = single runnable file. Zero stdin. Test cases hardcoded in main().

            MARKERS: Exactly one pair per file, each on its own line.
              Java/C++/C/JS:  // USER_CODE_START  and  // USER_CODE_END
              Python:         # USER_CODE_START   and  # USER_CODE_END
            Method between markers MUST be real compilable code (NEVER commented out).
            Method name is problem-specific (e.g. twoSum, trap, threeSum) — LeetCode style.
            Java class MUST be named `Main`. Java method MUST be `public static`.

            TC OUTPUT (one line per test, N is 1-based, no spaces):
              PASS visible : TC:N:PASS
              PASS hidden  : TC:N:PASS:hidden
              FAIL visible : TC:N:FAIL:input=<repr>:expected=<val>:got=<val>
              FAIL hidden  : TC:N:FAIL:hidden          ← NEVER expose input/expected/got

            TC COUNT: 4 visible + 2 hidden = 6 total.

            CORRECTNESS: For each test case, hand-trace and verify. Expected values can only
            contain elements that EXIST in the input array — never invent values.
            If problem says "1-indexed" (LC167 etc.), expected indices are 1-based.
            For "any-order valid" answers, sort both result and expected before comparing.

            COMPARISON BY TYPE:
              int/bool/char     → ==
              float/double      → abs(a-b) < 1e-9
              String            → .equals() Java | == Python/JS | strcmp C
              arrays            → Arrays.equals() Java | element loop C/C++/JS
              List<List>        → sort inner + outer, then compare
              ListNode/TreeNode → traverse / recurse on .val

            CUSTOM TYPES (ListNode, TreeNode): Define ONCE before USER_CODE_START.
            Repeat same definition as a comment INSIDE USER_CODE_START (LeetCode style).
            Put buildList/buildTree + equality helpers AFTER USER_CODE_END.

            C ARRAY RETURNS: int* solve(...,int* returnSize) {*returnSize=0; return NULL;}
            Pass &returnSize from test(), free() the pointer after checking.
            In-place: void solve(int* a, int n) {}

            COMPACT: helpers on as few lines as possible. No blank lines between methods.

            ════ JAVA TEMPLATE ════
            import java.util.*;
            public class Main {
                // USER_CODE_START
                public static <ReturnType> twoSum(<params>) { return <default>; }
                // USER_CODE_END
                static String fmt(int[] a){return Arrays.toString(a);}
                static void test(<params>, <ReturnType> expected, int tc, boolean hidden) {
                    <ReturnType> r = twoSum(<args>);
                    boolean ok = <correctCompare>;
                    if (ok) System.out.println("TC:"+tc+":PASS"+(hidden?":hidden":""));
                    else if (hidden) System.out.println("TC:"+tc+":FAIL:hidden");
                    else System.out.println("TC:"+tc+":FAIL:input="+fmt(<in>)+":expected="+expected+":got="+r);
                }
                public static void main(String[] args) {
                    test(...,1,false); test(...,2,false); test(...,3,false); test(...,4,false);
                    test(...,5,true);  test(...,6,true);
                }
            }

            ════ STUB DEFAULTS ════
            Java: {return 0;} / {return new int[0];} / {return "";} / {return null;} / {return new ArrayList<>();}
            C++:  {return 0;} / {return {};} / {return nullptr;}
            C:    {return 0;}  (arrays → returnSize pattern above)
            Python: return 0 / return [] / return "" / return None    (NEVER just `pass`)
            JS:   {return 0;} / {return [];}
            """;
    }
}
