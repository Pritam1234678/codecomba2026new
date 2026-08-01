package com.example.codecombat2026.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class CodeFormatService {

    private static final Logger log = LoggerFactory.getLogger(CodeFormatService.class);
    private static final int TIMEOUT_SECONDS = 5;
    private static final int MAX_CODE_LENGTH = 100_000;

    public String format(String code, String language) {
        if (code == null || code.isBlank()) return code;
        if (code.length() > MAX_CODE_LENGTH) return code;

        return switch (language.toUpperCase()) {
            case "JAVA" -> formatWithClang(code, "java");
            case "CPP" -> formatWithClang(code, "cpp");
            case "C" -> formatWithClang(code, "c");
            case "PYTHON", "PY" -> formatWithAutopep8(code);
            case "JAVASCRIPT", "JS" -> formatWithPrettier(code);
            default -> code;
        };
    }

    private String formatWithClang(String code, String language) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "/usr/bin/clang-format",
                "--style=LLVM",
                "--assume-filename=dummy." + language
            );
            pb.environment().remove("CLANG_FORMAT_STYLE");
            return runFormatter(pb, code);
        } catch (Exception e) {
            log.warn("clang-format failed for {}: {}", language, e.getMessage());
            return fallbackIndent(code);
        }
    }

    private String formatWithAutopep8(String code) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "/home/ubuntu/.local/bin/autopep8",
                "--max-line-length=120",
                "-"
            );
            return runFormatter(pb, code);
        } catch (Exception e) {
            log.warn("autopep8 failed: {}", e.getMessage());
            return fallbackIndent(code);
        }
    }

    private String formatWithPrettier(String code) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "/usr/bin/npx",
                "--yes",
                "prettier",
                "--parser", "babel",
                "--print-width", "120",
                "--tab-width", "4"
            );
            return runFormatter(pb, code);
        } catch (Exception e) {
            log.warn("prettier failed: {}", e.getMessage());
            return fallbackIndent(code);
        }
    }

    private String runFormatter(ProcessBuilder pb, String input) throws Exception {
        Process process = pb.start();

        try (OutputStream os = process.getOutputStream()) {
            os.write(input.getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!output.isEmpty()) output.append("\n");
                output.append(line);
            }
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("formatter timed out");
        }

        if (process.exitValue() != 0) {
            throw new IOException("formatter exit code " + process.exitValue() + ": " + error);
        }

        String result = output.toString();
        if (result.isBlank()) return input;
        return result;
    }

    private String fallbackIndent(String code) {
        String[] lines = code.split("\n", -1);
        StringBuilder result = new StringBuilder();
        int depth = 0;
        boolean inMultiLineComment = false;

        for (String rawLine : lines) {
            String trimmed = rawLine.trim();

            if (trimmed.isEmpty()) {
                result.append("\n");
                continue;
            }

            if (inMultiLineComment) {
                if (trimmed.contains("*/")) {
                    inMultiLineComment = false;
                }
            }
            if (trimmed.startsWith("/*")) {
                inMultiLineComment = true;
            }
            if (trimmed.startsWith("//") || trimmed.startsWith("#") || inMultiLineComment) {
                result.append("    ".repeat(Math.max(0, depth))).append(trimmed).append("\n");
                continue;
            }

            int closeBefore = countOccurrences(trimmed, "}") + countOccurrences(trimmed, ")");

            if (closeBefore > 0 && (trimmed.startsWith("}") || trimmed.startsWith(")") ||
                trimmed.startsWith("else") || trimmed.startsWith("elif") ||
                trimmed.startsWith("catch") || trimmed.startsWith("finally"))) {
                depth = Math.max(0, depth - 1);
            }

            result.append("    ".repeat(Math.max(0, depth))).append(trimmed).append("\n");

            int openCount = countOccurrences(trimmed, "{") + countOccurrences(trimmed, "(");
            int closeCount = countOccurrences(trimmed, "}") + countOccurrences(trimmed, ")");
            depth += openCount - closeCount;

            if (trimmed.endsWith(":")) {
                depth++;
            }

            depth = Math.max(0, depth);
        }

        return result.toString();
    }

    private int countOccurrences(String str, String find) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(find, idx)) != -1) {
            count++;
            idx += find.length();
        }
        return count;
    }
}
