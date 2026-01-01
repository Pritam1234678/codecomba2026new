package com.example.codecombat2026.service.judge;

import com.example.codecombat2026.dto.ExecutionResult;
import com.example.codecombat2026.entity.Submission;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class DockerJudgeService {

    @Value("${codecombat.docker.host}")
    private String dockerHost;

    private DockerClient dockerClient;

    @PostConstruct
    public void init() {
        try {
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .build();

            com.github.dockerjava.transport.DockerHttpClient httpClient = new com.github.dockerjava.zerodep.ZerodepDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(100)
                    .build();

            this.dockerClient = DockerClientBuilder.getInstance(config).withDockerHttpClient(httpClient).build();

            // Test connection
            this.dockerClient.pingCmd().exec();
            System.out.println("Docker connected successfully!");
        } catch (Throwable e) {
            System.err.println("Failed to initialize Docker Client: " + e.getMessage());
            this.dockerClient = null;
        }
    }

    public ExecutionResult execute(String code, Submission.ProgrammingLanguage language, String input,
            double timeLimit) {

        String fileName;
        String[] compileArgs, runArgs;

        // Determine language-specific settings
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");

        switch (language) {
            case JAVA:
                fileName = "Main.java";
                compileArgs = new String[] { "javac", fileName };
                runArgs = new String[] { "java", "Main" };
                break;
            case CPP:
                fileName = "main.cpp";
                compileArgs = new String[] { "g++", "-o", "main", fileName };
                runArgs = new String[] { isWindows ? "main.exe" : "./main" };
                break;
            case PYTHON:
                fileName = "main.py";
                compileArgs = null; // Python doesn't need compilation
                runArgs = new String[] { "python", fileName };
                break;
            case C:
                fileName = "main.c";
                compileArgs = new String[] { "gcc", "-o", "main", fileName };
                runArgs = new String[] { isWindows ? "main.exe" : "./main" };
                break;
            case JAVASCRIPT:
                fileName = "main.js";
                compileArgs = null; // JavaScript doesn't need compilation
                runArgs = new String[] { "node", fileName };
                break;
            default:
                return new ExecutionResult("", "Unsupported language: " + language, 0, 0, 1, false, false, true);
        }

        try {
            // Create temp directory for this execution
            java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("judge_");
            java.nio.file.Path sourceFile = tempDir.resolve(fileName);

            // Write code to file
            java.nio.file.Files.write(sourceFile, code.getBytes(StandardCharsets.UTF_8));

            // Compile (only for compiled languages)
            if (compileArgs != null) {
                ProcessBuilder compileBuilder = new ProcessBuilder(compileArgs);
                compileBuilder.directory(tempDir.toFile());
                compileBuilder.redirectErrorStream(true);

                Process compileProcess = compileBuilder.start();
                boolean compiledInTime = compileProcess.waitFor(10, TimeUnit.SECONDS);

                if (!compiledInTime) {
                    compileProcess.destroyForcibly();
                    cleanup(tempDir);
                    return new ExecutionResult("", "Compilation timeout", 0, 0, 1, false, false, true);
                }

                if (compileProcess.exitValue() != 0) {
                    String compileError = new String(compileProcess.getInputStream().readAllBytes(),
                            StandardCharsets.UTF_8);
                    cleanup(tempDir);
                    return new ExecutionResult("", compileError, 0, 0, 1, false, false, true);
                }

                // For C++ and C, verify the executable was created
                if (language == Submission.ProgrammingLanguage.CPP || language == Submission.ProgrammingLanguage.C) {
                    String exeName = isWindows ? "main.exe" : "main";
                    java.nio.file.Path exePath = tempDir.resolve(exeName);
                    if (!java.nio.file.Files.exists(exePath)) {
                        cleanup(tempDir);
                        return new ExecutionResult("", "Compilation succeeded but executable not found: " + exeName,
                                0, 0, 1, false, false, true);
                    }
                    // Update runArgs to use absolute path
                    runArgs = new String[] { exePath.toAbsolutePath().toString() };
                }
            }

            // Execute
            long execStart = System.currentTimeMillis();
            ProcessBuilder execBuilder = new ProcessBuilder(runArgs);
            execBuilder.directory(tempDir.toFile());

            Process execProcess = execBuilder.start();

            // Provide input
            if (input != null && !input.isEmpty()) {
                execProcess.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
                execProcess.getOutputStream().flush();
                execProcess.getOutputStream().close();
            }

            // Wait with timeout
            long timeLimitMs = (long) (timeLimit * 1000);
            boolean finishedInTime = execProcess.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);

            long execTime = System.currentTimeMillis() - execStart;

            if (!finishedInTime) {
                execProcess.destroyForcibly();
                cleanup(tempDir);
                return new ExecutionResult("", "Time Limit Exceeded", execTime, 0, 124, true, false, false);
            }

            // Capture output
            String stdout = new String(execProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(execProcess.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = execProcess.exitValue();

            // Estimate memory usage (approximate)
            Runtime runtime = Runtime.getRuntime();
            long memoryUsed = (runtime.totalMemory() - runtime.freeMemory()) / 1024; // Convert to KB

            cleanup(tempDir);

            return new ExecutionResult(stdout, stderr, execTime, memoryUsed, exitCode, false, false, false);

        } catch (Exception e) {
            return new ExecutionResult("", "Execution error: " + e.getMessage(), 0, 0, 1, false, false, false);
        }
    }

    private void cleanup(java.nio.file.Path dir) {
        try {
            java.nio.file.Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            java.nio.file.Files.delete(path);
                        } catch (Exception e) {
                            // Ignore cleanup errors
                        }
                    });
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}
