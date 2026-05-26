package com.example.codecombat2026.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * Lightweight math CAPTCHA. Issued challenges are stored in Valkey for 5
 * minutes and consumed on first verify (single-use). No third-party
 * services, no JS challenge — a 1-9 + 1-9 question is enough to reject
 * trivial scripted bots while staying readable for users.
 *
 * If Valkey is unreachable verify() returns false (fail closed) — that's
 * the safer default for a CAPTCHA: if we can't prove a token is valid, we
 * don't accept it.
 */
@Service
public class CaptchaService {

    @Autowired
    private StringRedisTemplate redis;

    private final SecureRandom rng = new SecureRandom();

    private static final Duration TTL = Duration.ofMinutes(5);

    public CaptchaChallenge issue() {
        int a = 1 + rng.nextInt(9);
        int b = 1 + rng.nextInt(9);
        int answer = a + b;
        String token = UUID.randomUUID().toString();
        try {
            redis.opsForValue().set("captcha:" + token, String.valueOf(answer), TTL);
        } catch (Exception ignored) {
            // Best-effort. verify() will fail closed if the key isn't there.
        }
        return new CaptchaChallenge(token, "What is " + a + " + " + b + "?");
    }

    public boolean verify(String token, String userAnswer) {
        if (token == null || token.isBlank() || userAnswer == null) return false;
        String key = "captcha:" + token;
        String expected;
        try {
            expected = redis.opsForValue().get(key);
        } catch (Exception e) {
            return false;
        }
        if (expected == null) return false;
        try {
            redis.delete(key); // single-use
        } catch (Exception ignored) {}
        try {
            return Integer.parseInt(userAnswer.trim()) == Integer.parseInt(expected);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public record CaptchaChallenge(String token, String question) {}
}
