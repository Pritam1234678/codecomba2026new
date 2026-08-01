package com.example.codecombat2026.controller;

import com.example.codecombat2026.service.CodeFormatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/code")
public class FormatController {

    @Autowired
    private CodeFormatService codeFormatService;

    private static final int MAX_CODE_LENGTH = 100_000;

    @PostMapping("/format")
    public ResponseEntity<?> formatCode(@RequestBody FormatRequest request) {
        if (request.code == null || request.code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Code cannot be empty"));
        }
        if (request.code.length() > MAX_CODE_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "Code too long"));
        }
        if (request.language == null || request.language.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Language is required"));
        }

        String formatted = codeFormatService.format(request.code, request.language);
        return ResponseEntity.ok(Map.of("code", formatted));
    }

    public static class FormatRequest {
        public String code;
        public String language;
    }
}
