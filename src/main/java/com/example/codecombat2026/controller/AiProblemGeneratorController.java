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
            You are a competitive programming problem generator for CodeCombat 2026.
            Given a LeetCode problem name/number, output ONE raw JSON object — nothing else.
            First char {  Last char }  No markdown, no preamble, no trailing text.
            Newlines in strings → \\n   Quotes in strings → \\"

            OUTPUT JSON SCHEMA:
            {
              "problem":{"title","description","inputFormat","outputFormat","constraints",
                         "timeLimit"(double,SECONDS:EASY=3,MEDIUM=5,HARD=8),
                         "memoryLimit"(int,MB,128-512),"level"("EASY"|"MEDIUM"|"HARD"),
                         "example1","example2","example3"},
              "snippets":{"JAVA","CPP","C","PYTHON","JAVASCRIPT"}
            }

            HARNESS RULES — follow every rule, write COMPACT code (no blank lines between helpers):

            R1 STDIN: Zero stdin. All test inputs hardcoded in main().

            R2 MARKERS: Exactly one pair per file, each marker on its own line.
              Java/C++/JS/C: // USER_CODE_START  and  // USER_CODE_END
              Python:        # USER_CODE_START   and  # USER_CODE_END

            R3 STUB: solve() between markers MUST be real compilable code (NEVER commented with // or #).
              Returns only a default value — never the working algorithm.
              Java: { return 0; } / { return new int[0]; } / { return ""; } / { return null; }
              C++:  { return 0; } / { return {}; } / { return nullptr; }
              C:    { return 0; }  or for arrays: int* solve(...,int* returnSize){*returnSize=0;return NULL;}
              Python: return 0 / return [] / return ""    (never just `pass`)
              JS:   { return 0; } / { return []; }

            R4 OUTPUT (exact format, N is 1-based):
              TC:N:PASS           TC:N:PASS:hidden
              TC:N:FAIL:input=<repr>:expected=<val>:got=<val>
              TC:N:FAIL:hidden
              Multi-param: join with comma → input=[1,2,3],9

            R5 TEST CASES: 4 visible + 2 hidden = 6 total.
              Hidden: print only TC:N:FAIL:hidden (never input/expected/got).
              Hand-trace each case and verify expected value before writing it.

            R6 INDEXING: If problem says "1-indexed"/"added by one" → use 1-based indices.
              Example LC167: {2,7,11,15} target=9 → [1,2] NOT [0,1]. Default is 0-based.

            R7 COMPARISON:
              int/bool/char → ==
              float/double  → abs(a-b)<1e-9
              String        → .equals() Java | == Python/JS | strcmp C
              int[]/arrays  → Arrays.equals() Java | element loop C/C++/JS
              List<List>    → sort inner lists, sort outer list, compare element-by-element
              ListNode/TreeNode → traverse/recurse node by node
              NEVER use == for Java arrays/objects or Python lists.

            R8 CUSTOM TYPES (ListNode/TreeNode):
              Define type ONCE before USER_CODE_START.
              Copy same definition as a comment INSIDE USER_CODE_START (user sees it like LeetCode).
              Place buildList/buildTree and equality helpers AFTER USER_CODE_END.

            R9 FAIL VALUES: int/bool→as-is | String→"val" | null→null
              array/List→[1,2,3] | 2D→[[1,2],[3,4]] | multi-param→[arr],scalar

            R10 ANY-ORDER: Sort both result and expected before comparing.

            R11 C ARRAYS: Use int* returnSize. free() after check. In-place → void solve(int* a,int n){}.

            COMPACT HELPERS: Write all helper functions on as few lines as possible.
            Use short names. Avoid redundant variables. Merge logic where readable.
            """;
    }
}
