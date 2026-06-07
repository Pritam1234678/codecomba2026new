package com.example.codecombat2026.controller;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
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

        Map choice = choices.get(0);
        String finishReason = (String) choice.get("finish_reason");
        String content = (String) ((Map) choice.get("message")).get("content");
        if (content == null || content.isBlank()) throw new RuntimeException("Empty content from AI");

        if ("length".equals(finishReason)) {
            throw new RuntimeException(
                "Output truncated: model hit its token limit on NVIDIA NIM. " +
                "The JSON is incomplete. Try DeepSeek model or simplify the problem name.");
        }
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

    // ─── System prompt: instruction + the entire PROBLEM_GUIDE.md ─────────────

    private final String systemPrompt = loadSystemPrompt();

    private static String loadSystemPrompt() {
        String guide;
        try {
            ClassPathResource guideRes = new ClassPathResource("PROBLEM_GUIDE.md");
            String raw = new String(guideRes.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // Strip blank lines to reduce token count (Kimi K2.6 has tight output budget on NVIDIA NIM)
            guide = String.join("\n", raw.lines().filter(l -> !l.isBlank()).toArray(String[]::new));
        } catch (Exception e) {
            guide = "(guide missing — follow CodeCombat harness conventions)";
        }
        return """
            You are a problem generator for CodeCombat 2026. The user gives you a LeetCode
            problem name/number OR a problem description. You output ONE raw JSON object
            containing the problem + 5-language harnesses (JAVA, CPP, C, PYTHON, JAVASCRIPT).

            FOLLOW THE GUIDE BELOW EXACTLY — it has the templates and rules for every language.

            OUTPUT RULES:
            • First char {, last char }. No markdown fences, no preamble.
            • Newlines inside string values → \\n     Quotes inside strings → \\"
            • Write COMPACT helpers (no blank lines, short helper code).
            • 4 visible + 2 hidden = 6 total test cases (override the guide's "8+2" — keep it 6).
            • Hand-verify every expected value. Values must only reference elements in the input.
            • timeLimit (double, seconds): EASY=3 MEDIUM=5 HARD=8.  memoryLimit (int, MB) 128-512.

            JSON SHAPE:
            {
              "problem": {
                "title","description","inputFormat","outputFormat","constraints",
                "timeLimit","memoryLimit","level"("EASY"|"MEDIUM"|"HARD"),
                "example1","example2","example3"
              },
              "snippets": { "JAVA":"...", "CPP":"...", "C":"...", "PYTHON":"...", "JAVASCRIPT":"..." }
            }

            ════════════════ PROBLEM_GUIDE.md (authoritative) ════════════════
            """ + guide;
    }

    private String buildSystemPrompt() {
        return systemPrompt;
    }
}
