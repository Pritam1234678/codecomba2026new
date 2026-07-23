package com.example.codecombat2026.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private static final String GITHUB_API = "https://api.github.com";
    private static final String REPO_NAME = "CodeCoder";
    private final RestTemplate rest = new RestTemplate();
    {
        var f = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10_000);
        f.setReadTimeout(15_000);
        rest.setRequestFactory(f);
    }

    @Value("${GITHUB_CLIENT_ID:}")
    private String clientId;

    @Value("${GITHUB_CLIENT_SECRET:}")
    private String clientSecret;

    @Autowired
    private com.example.codecombat2026.repository.UserRepository userRepository;

    @Autowired
    private com.example.codecombat2026.repository.PracticeSubmissionRepository practiceSubmissionRepository;

    public Map<String, String> exchangeToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        try {
            HttpHeaders h = new HttpHeaders();
            h.setAccept(List.of(MediaType.APPLICATION_JSON));
            h.set("User-Agent", "CodeCoder");
            var resp = rest.postForEntity("https://github.com/login/oauth/access_token",
                new HttpEntity<>(body, h), Map.class);
            String token = (String) resp.getBody().get("access_token");
            if (token == null) throw new RuntimeException("No token in response: " + resp.getBody());
            var userResp = rest.exchange(GITHUB_API + "/user",
                HttpMethod.GET, authHeaders(token), Map.class);
            String username = (String) userResp.getBody().get("login");
            return Map.of("token", token, "username", username);
        } catch (Exception e) {
            log.error("GitHub token exchange failed: {}", e.getMessage());
            throw new RuntimeException("GitHub auth failed: " + e.getMessage());
        }
    }

    public String pushSolution(Long userId, Long submissionId, String problemTitle,
                                String language, String code, String userName) {
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return "User not found";
        var user = userOpt.get();
        String token = user.getGithubToken();
        if (token == null || token.isBlank()) return "GitHub not connected";
        String ghUser = user.getGithubUsername() != null ? user.getGithubUsername() : userName;

        try {
            ensureRepo(token);
            String slug = slugify(problemTitle);
            int count = countFiles(token, ghUser, slug, language);
            count++;
            String fileName = language.toLowerCase() + count + "Solution." + ext(language);
            String path = slug + "/" + fileName;

            String sha = getFileSha(token, ghUser, path);
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", "✅ " + problemTitle + " — " + language + " solution #" + count);
            payload.put("content", base64(code));
            if (sha != null) payload.put("sha", sha);

            rest.exchange(GITHUB_API + "/repos/" + ghUser + "/" + REPO_NAME + "/contents/" + path,
                HttpMethod.PUT, authJson(token, payload), Map.class);

            practiceSubmissionRepository.updateGithubPushed(submissionId);
            log.info("GitHub pushed: {} — {}/{}", problemTitle, slug, fileName);
            return "ok";
        } catch (Exception e) {
            log.error("GitHub push failed for sub {}: {}", submissionId, e.getMessage());
            return "push failed: " + e.getMessage();
        }
    }

    private void ensureRepo(String token) {
        try {
            var resp = rest.exchange(GITHUB_API + "/repos/" + getUser(token) + "/" + REPO_NAME,
                HttpMethod.GET, authHeaders(token), Map.class);
            if (resp.getStatusCode() == HttpStatus.OK) return;
        } catch (Exception ignored) {}

        Map<String, Object> body = new HashMap<>();
        body.put("name", REPO_NAME);
        body.put("private", false);
        body.put("auto_init", true);
        body.put("description", "CodeCoder practice solutions — auto-synced");
        rest.postForEntity(GITHUB_API + "/user/repos", authJson(token, body), Map.class);
    }

    private String getUser(String token) {
        try {
            var resp = rest.exchange(GITHUB_API + "/user", HttpMethod.GET, authHeaders(token), Map.class);
            return (String) resp.getBody().get("login");
        } catch (Exception e) { return null; }
    }

    private int countFiles(String token, String owner, String slug, String lang) {
        try {
            String prefix = slug + "/" + lang.toLowerCase();
            var resp = rest.exchange(GITHUB_API + "/repos/" + owner + "/" + REPO_NAME + "/contents/" + slug,
                HttpMethod.GET, authHeaders(token), List.class);
            List<Map<String, Object>> files = (List<Map<String, Object>>) resp.getBody();
            if (files == null) return 0;
            return (int) files.stream().filter(f -> {
                String name = (String) f.get("name");
                return name != null && name.startsWith(lang.toLowerCase());
            }).count();
        } catch (Exception e) { return 0; }
    }

    private String getFileSha(String token, String owner, String path) {
        try {
            var resp = rest.exchange(GITHUB_API + "/repos/" + owner + "/" + REPO_NAME + "/contents/" + path,
                HttpMethod.GET, authHeaders(token), Map.class);
            return (String) resp.getBody().get("sha");
        } catch (Exception e) { return null; }
    }

    public boolean isConnected(Long userId) {
        return userRepository.findById(userId)
            .map(u -> u.getGithubToken() != null && !u.getGithubToken().isBlank())
            .orElse(false);
    }

    private HttpEntity<?> authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        h.set("User-Agent", "CodeCoder");
        return new HttpEntity<>(h);
    }

    private HttpEntity<Map<String, Object>> authJson(String token, Map<String, Object> body) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("User-Agent", "CodeCoder");
        return new HttpEntity<>(body, h);
    }

    private String slugify(String title) {
        return title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private String ext(String lang) {
        return switch (lang.toUpperCase()) {
            case "JAVA" -> ".java";
            case "PYTHON" -> ".py";
            case "CPP" -> ".cpp";
            case "C" -> ".c";
            case "JAVASCRIPT" -> ".js";
            default -> ".txt";
        };
    }

    private String base64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes());
    }
}
