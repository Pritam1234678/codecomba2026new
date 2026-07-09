package com.example.codecombat2026.controller;

import com.example.codecombat2026.service.SubmissionWorkerPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class WelcomeController {

    @Autowired
    private SubmissionWorkerPool workerPool;

    @GetMapping("/")
    public Map<String, Object> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Code Combat API");
        response.put("version", "1.0.0");
        response.put("status", "running");
        response.put("endpoints", Map.of(
                "auth", "/api/auth",
                "contests", "/api/contests",
                "problems", "/api/problems",
                "submissions", "/api/submissions",
                "test", "/api/test"));
        return response;
    }

    @GetMapping("/api")
    public Map<String, String> apiInfo() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Code Combat API v1.0");
        response.put("documentation", "Visit / for endpoint information");
        return response;
    }

    /**
     * Queue status — useful during a live contest to monitor load.
     * Returns stats for both public and private submission queues.
     * GET /api/queue-status
     */
    @GetMapping("/api/queue-status")
    public Map<String, Object> queueStatus() {
        Map<String, Object> status = new HashMap<>();
        Long publicDepth = workerPool.getQueueDepth();
        Long privateDepth = workerPool.getPrivateQueueDepth();
        Long totalDepth = (publicDepth != null ? publicDepth : 0L) + (privateDepth != null ? privateDepth : 0L);
        
        status.put("publicQueueDepth", publicDepth);
        status.put("privateQueueDepth", privateDepth);
        status.put("totalQueueDepth", totalDepth);
        status.put("activeJobs", workerPool.getActiveJobs());
        status.put("totalProcessed", workerPool.getTotalProcessed());
        status.put("estimatedWaitSeconds", totalDepth * 5); // ~5s avg per job
        return status;
    }
}
