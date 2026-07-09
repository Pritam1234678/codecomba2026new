package com.example.codecombat2026.controller;

import com.example.codecombat2026.annotation.RateLimited;
import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.dto.ProblemDTO;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.exception.BadRequestException;
import com.example.codecombat2026.exception.ConflictException;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.AIProblemGeneratorService;
import com.example.codecombat2026.service.ContestProblemService;
import com.example.codecombat2026.service.PrivateContestAccessValidator;
import com.example.codecombat2026.service.RateLimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for problem management in private contests.
 * 
 * Provides endpoints for:
 * - Browsing available problems to add to contests
 * - Attaching problems to private contests
 * - Removing problems from private contests
 * - AI-generated problem creation
 * - Editing problems (owner or admin only)
 * 
 * Security:
 * - All endpoints require authentication
 * - Contest host verification for contest-specific operations
 * - Owner or admin verification for problem editing
 * - Rate limiting on AI generation endpoint
 * 
 * Requirements: 8.1, 8.4, 9.1, 10.1
 */
@RestController
@RequestMapping("/api/contests/private")
public class PrivateContestProblemController {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestProblemController.class);

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private ContestProblemService contestProblemService;

    @Autowired
    private AIProblemGeneratorService aiProblemGeneratorService;

    @Autowired
    private PrivateContestAccessValidator accessValidator;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private com.example.codecombat2026.service.PrivateContestCacheService cacheService;

    /**
     * Browse available problems for attachment to private contest.
     * 
     * GET /api/contests/private/{id}/problems/available
     * 
     * Returns problems that:
     * - Have visibility PUBLIC or PRIVATE_AVAILABLE
     * - Are not currently attached to this contest
     * - Match optional filters (difficulty, search)
     * 
     * Access: Contest host only
     * 
     * @param contestId The private contest ID
     * @param difficulty Optional filter: EASY, MEDIUM, HARD
     * @param search Optional title search string
     * @param authentication Current user authentication
     * @return List of available problems
     * @throws ForbiddenException if user is not the contest host
     * @throws ResourceNotFoundException if contest doesn't exist
     * 
     * Requirements: 8.1
     */
    @GetMapping("/{contestId}/problems/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProblemDTO>> browseAvailableProblems(
            @PathVariable Long contestId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String search,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        log.info("User {} browsing available problems for contest {}", userId, contestId);

        // Verify user is the contest host
        if (!accessValidator.isHost(contestId, userId)) {
            throw new ForbiddenException("Only the contest host can browse problems");
        }

        // Validate difficulty if provided
        if (difficulty != null && !difficulty.isBlank()) {
            if (!List.of("EASY", "MEDIUM", "HARD").contains(difficulty.toUpperCase())) {
                throw new BadRequestException("Invalid difficulty. Must be EASY, MEDIUM, or HARD");
            }
            difficulty = difficulty.toUpperCase();
        }

        // Get available problems (not already in contest, with PUBLIC or PRIVATE_AVAILABLE visibility)
        List<Problem> problems = problemRepository.findAvailableForContest(
                contestId,
                search,
                difficulty
        );

        // Filter by visibility (only PUBLIC and PRIVATE_AVAILABLE)
        List<Problem> filteredProblems = problems.stream()
                .filter(p -> "PUBLIC".equals(p.getVisibility()) || 
                            "PRIVATE_AVAILABLE".equals(p.getVisibility()))
                .collect(Collectors.toList());

        // Convert to DTOs
        List<ProblemDTO> problemDTOs = filteredProblems.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        log.info("Found {} available problems for contest {}", problemDTOs.size(), contestId);

        return ResponseEntity.ok(problemDTOs);
    }

    /**
     * Attach problems to private contest.
     * 
     * POST /api/contests/private/{id}/problems
     * 
     * Attaches one or more problems to the contest.
     * Problems are added with sequential display order.
     * 
     * Access: Contest host only
     * Timing: Only allowed before contest starts (status UPCOMING)
     * 
     * @param contestId The private contest ID
     * @param request Request body with problemIds array
     * @param authentication Current user authentication
     * @return Response with count of attached problems
     * @throws ForbiddenException if user is not the contest host
     * @throws ConflictException if contest has already started
     * @throws BadRequestException if problem IDs are invalid
     * 
     * Requirements: 8.4
     */
    @PostMapping("/{contestId}/problems")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> attachProblems(
            @PathVariable Long contestId,
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        List<Long> problemIds = request.get("problemIds");
        if (problemIds == null || problemIds.isEmpty()) {
            throw new BadRequestException("problemIds array is required and cannot be empty");
        }

        log.info("User {} attaching {} problems to contest {}", userId, problemIds.size(), contestId);

        // Verify user is the contest host
        if (!accessValidator.isHost(contestId, userId)) {
            throw new ForbiddenException("Only the contest host can attach problems");
        }

        // Attach problems using the ContestProblemService
        List<com.example.codecombat2026.entity.ContestProblem> attachedProblems = 
                contestProblemService.attachMany(contestId, problemIds);

        // Invalidate cache since contest problems changed
        cacheService.invalidateContestCache(contestId);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("attachedCount", attachedProblems.size());
        response.put("message", "Successfully attached " + attachedProblems.size() + " problem(s) to contest");

        log.info("Attached {} problems to contest {}", attachedProblems.size(), contestId);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove a problem from private contest.
     * 
     * DELETE /api/contests/private/{id}/problems/{problemId}
     * 
     * Removes the specified problem from the contest.
     * 
     * Access: Contest host only
     * Timing: Only allowed before contest starts (status UPCOMING)
     * 
     * @param contestId The private contest ID
     * @param problemId The problem ID to remove
     * @param authentication Current user authentication
     * @return Success message
     * @throws ForbiddenException if user is not the contest host
     * @throws ConflictException if contest has already started
     * @throws ResourceNotFoundException if problem not attached to contest
     * 
     * Requirements: 8.4
     */
    @DeleteMapping("/{contestId}/problems/{problemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> removeProblem(
            @PathVariable Long contestId,
            @PathVariable Long problemId,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        log.info("User {} removing problem {} from contest {}", userId, problemId, contestId);

        // Verify user is the contest host
        if (!accessValidator.isHost(contestId, userId)) {
            throw new ForbiddenException("Only the contest host can remove problems");
        }

        // Remove problem using ContestProblemService
        contestProblemService.detach(contestId, problemId);

        // Invalidate cache since contest problems changed
        cacheService.invalidateContestCache(contestId);

        log.info("Removed problem {} from contest {}", problemId, contestId);

        return ResponseEntity.ok(new MessageResponse("Problem removed from contest successfully"));
    }

    /**
     * Generate a new problem using AI.
     * 
     * POST /api/contests/private/{id}/problems/generate
     * 
     * Generates a new problem using AI based on natural language prompt.
     * 
     * Rate Limiting: 5 generations per user per day (enforced in service)
     * 
     * The generated problem:
     * - Is created with visibility PRIVATE_OWNED
     * - Is owned by the requesting user (createdBy field)
     * - Can be edited by the owner
     * - Is NOT automatically attached to the contest (must be done separately)
     * 
     * Access: Contest host only
     * 
     * @param contestId The private contest ID (for access validation)
     * @param request Request body with prompt, difficulty, and topic
     * @param authentication Current user authentication
     * @return Generated problem DTO
     * @throws ForbiddenException if user is not the contest host
     * @throws com.example.codecombat2026.exception.TooManyRequestsException if rate limit exceeded
     * @throws BadRequestException if input validation fails
     * 
     * Requirements: 9.1, 24.2, 24.5, 24.6
     */
    @RateLimited(type = RateLimited.RateLimitType.AI_PROBLEM_GENERATION)
    @PostMapping("/{contestId}/problems/generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProblemDTO> generateProblem(
            @PathVariable Long contestId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        String prompt = request.get("prompt");
        String difficulty = request.get("difficulty");
        String topic = request.get("topic");

        log.info("User {} generating AI problem for contest {}", userId, contestId);

        // Verify user is the contest host
        if (!accessValidator.isHost(contestId, userId)) {
            throw new ForbiddenException("Only the contest host can generate problems");
        }

        // Validate inputs
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new BadRequestException("Prompt is required");
        }
        if (prompt.length() > 1000) {
            throw new BadRequestException("Prompt cannot exceed 1000 characters");
        }
        if (difficulty == null || difficulty.trim().isEmpty()) {
            throw new BadRequestException("Difficulty is required");
        }
        if (topic != null && topic.length() > 100) {
            throw new BadRequestException("Topic cannot exceed 100 characters");
        }

        // Generate problem using AI service (rate limiting is enforced inside)
        Problem generatedProblem = aiProblemGeneratorService.generateProblem(
                prompt.trim(),
                difficulty.toUpperCase(),
                topic != null ? topic.trim() : null,
                userId
        );

        log.info("Generated problem {} for user {}", generatedProblem.getId(), userId);

        // Convert to DTO and return
        ProblemDTO problemDTO = convertToDTO(generatedProblem);

        return ResponseEntity.status(HttpStatus.CREATED).body(problemDTO);
    }

    /**
     * Edit a problem.
     * 
     * PUT /api/problems/{id}
     * 
     * Allows editing of problem details.
     * 
     * Access Control:
     * - Owner (createdBy == userId) can edit their own PRIVATE_OWNED problems
     * - Admins can edit any problem
     * - Cannot edit problems attached to LIVE or ENDED contests
     * 
     * Editable Fields:
     * - title, description
     * - inputFormat, outputFormat, constraints
     * - timeLimit, memoryLimit
     * - example1, example2, example3
     * - level (difficulty)
     * 
     * Non-Editable:
     * - visibility (set at creation)
     * - createdBy (set at creation)
     * - Code snippets (managed separately)
     * 
     * @param problemId The problem ID to edit
     * @param updates Map of fields to update
     * @param authentication Current user authentication
     * @return Updated problem DTO
     * @throws ForbiddenException if user doesn't have permission
     * @throws ConflictException if problem is attached to a live contest
     * @throws ResourceNotFoundException if problem doesn't exist
     * 
     * Requirements: 10.1
     */
    @PutMapping("/{contestId}/problems/{problemId}/edit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProblemDTO> editProblem(
            @PathVariable Long contestId,
            @PathVariable Long problemId,
            @RequestBody Map<String, Object> updates,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        log.info("User {} editing problem {}", userId, problemId);

        // Verify user is the contest host (for context validation)
        if (!accessValidator.isHost(contestId, userId) && !isAdmin) {
            throw new ForbiddenException("Only the contest host or admin can edit problems");
        }

        log.info("User {} editing problem {}", userId, problemId);

        // Find the problem
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        // Verify user has permission to edit
        boolean isOwner = problem.getCreatedBy() != null && problem.getCreatedBy().equals(userId);
        boolean canEditPublic = isAdmin && "PUBLIC".equals(problem.getVisibility());
        boolean canEditPrivate = isOwner && "PRIVATE_OWNED".equals(problem.getVisibility());

        if (!canEditPublic && !canEditPrivate && !isAdmin) {
            throw new ForbiddenException("You don't have permission to edit this problem");
        }

        // Check if problem is attached to any LIVE or ENDED contests
        // This would require a query through ContestProblem repository
        // For now, we'll do a simplified check using the contests collection
        boolean attachedToActiveContest = problem.getContests().stream()
                .anyMatch(c -> c.getStatus() == com.example.codecombat2026.entity.Contest.ContestStatus.LIVE ||
                              c.getStatus() == com.example.codecombat2026.entity.Contest.ContestStatus.ENDED);

        if (attachedToActiveContest) {
            throw new ConflictException("Cannot edit problem that is attached to a LIVE or ENDED contest");
        }

        // Apply updates
        if (updates.containsKey("title")) {
            problem.setTitle((String) updates.get("title"));
        }
        if (updates.containsKey("description")) {
            problem.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("inputFormat")) {
            problem.setInputFormat((String) updates.get("inputFormat"));
        }
        if (updates.containsKey("outputFormat")) {
            problem.setOutputFormat((String) updates.get("outputFormat"));
        }
        if (updates.containsKey("constraints")) {
            problem.setConstraints((String) updates.get("constraints"));
        }
        if (updates.containsKey("timeLimit")) {
            Object timeLimit = updates.get("timeLimit");
            if (timeLimit instanceof Number) {
                problem.setTimeLimit(((Number) timeLimit).doubleValue());
            }
        }
        if (updates.containsKey("memoryLimit")) {
            Object memoryLimit = updates.get("memoryLimit");
            if (memoryLimit instanceof Number) {
                problem.setMemoryLimit(((Number) memoryLimit).intValue());
            }
        }
        if (updates.containsKey("example1")) {
            problem.setExample1((String) updates.get("example1"));
        }
        if (updates.containsKey("example2")) {
            problem.setExample2((String) updates.get("example2"));
        }
        if (updates.containsKey("example3")) {
            problem.setExample3((String) updates.get("example3"));
        }
        if (updates.containsKey("level")) {
            String level = (String) updates.get("level");
            if (List.of("EASY", "MEDIUM", "HARD").contains(level.toUpperCase())) {
                problem.setLevel(level.toUpperCase());
            } else {
                throw new BadRequestException("Invalid difficulty level");
            }
        }

        // Save updated problem
        problem = problemRepository.save(problem);

        log.info("Updated problem {}", problemId);

        return ResponseEntity.ok(convertToDTO(problem));
    }

    /**
     * Convert Problem entity to DTO.
     * 
     * @param problem The problem entity
     * @return ProblemDTO with basic fields
     */
    private ProblemDTO convertToDTO(Problem problem) {
        ProblemDTO dto = new ProblemDTO();
        dto.setId(problem.getId());
        dto.setTitle(problem.getTitle());
        dto.setDescription(problem.getDescription());
        dto.setInputFormat(problem.getInputFormat());
        dto.setOutputFormat(problem.getOutputFormat());
        dto.setConstraints(problem.getConstraints());
        dto.setTimeLimit(problem.getTimeLimit());
        dto.setMemoryLimit(problem.getMemoryLimit());
        dto.setLevel(problem.getLevel());
        dto.setVisibility(problem.getVisibility());
        dto.setCreatedBy(problem.getCreatedBy());
        dto.setActive(problem.getActive());
        dto.setExample1(problem.getExample1());
        dto.setExample2(problem.getExample2());
        dto.setExample3(problem.getExample3());
        return dto;
    }
}
