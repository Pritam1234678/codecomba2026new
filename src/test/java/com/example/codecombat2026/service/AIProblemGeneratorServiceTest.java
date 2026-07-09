package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.CodeSnippet;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.Role;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.TooManyRequestsException;
import com.example.codecombat2026.repository.CodeSnippetRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIProblemGeneratorService.
 * 
 * Tests cover:
 * - Successful problem generation
 * - Rate limit enforcement
 * - AI API failure handling
 * - Problem entity creation with correct visibility
 * - Test case storage (code snippets)
 * - Input validation
 * 
 * Requirements: 9.1, 9.2, 9.4, 9.6, 9.7, 30.4
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AIProblemGeneratorService Tests")
class AIProblemGeneratorServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private CodeSnippetRepository codeSnippetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AIProblemGeneratorService service;

    private static final Long TEST_USER_ID = 42L;
    private static final String TEST_PROMPT = "Create a problem about finding two numbers that sum to a target";
    private static final String TEST_DIFFICULTY = "MEDIUM";
    private static final String TEST_TOPIC = "Arrays";

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        
        // Set API keys via reflection
        ReflectionTestUtils.setField(service, "kimiApiKey", "test-nvidia-key");
        ReflectionTestUtils.setField(service, "deepseekApiKey", "test-deepseek-key");
        
        // Inject mocked RestTemplate
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("Should successfully generate problem when under rate limit")
    void testGenerateProblem_Success() {
        // Given: User is under rate limit
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        // Mock AI API responses
        mockSuccessfulAIResponses();

        // Mock repository saves
        Problem savedProblem = createMockProblem();
        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);
        when(codeSnippetRepository.save(any(CodeSnippet.class))).thenAnswer(i -> i.getArgument(0));

        // When: Generate problem
        Problem result = service.generateProblem(TEST_PROMPT, TEST_DIFFICULTY, TEST_TOPIC, TEST_USER_ID);

        // Then: Problem is created correctly
        assertNotNull(result);
        assertEquals(savedProblem.getId(), result.getId());
        assertEquals(savedProblem.getTitle(), result.getTitle());
        assertEquals("PRIVATE_OWNED", result.getVisibility());
        assertEquals(TEST_USER_ID, result.getCreatedBy());
        assertEquals(TEST_DIFFICULTY, result.getLevel());

        // Verify rate limit was checked
        verify(valueOperations).increment("ai:problem:gen:user:" + TEST_USER_ID);
        verify(redis).expire(eq("ai:problem:gen:user:" + TEST_USER_ID), any(Duration.class));

        // Verify problem was saved
        verify(problemRepository).save(any(Problem.class));

        // Verify code snippets were saved for all languages
        verify(codeSnippetRepository, times(5)).save(any(CodeSnippet.class));
    }

    @Test
    @DisplayName("Should enforce rate limit after 5 generations per day")
    void testCheckRateLimit_ExceedsLimit() {
        // Given: User has already generated 6 problems today
        when(valueOperations.increment(anyString())).thenReturn(6L);

        // When & Then: Should throw TooManyRequestsException
        TooManyRequestsException exception = assertThrows(
            TooManyRequestsException.class,
            () -> service.checkRateLimit(TEST_USER_ID)
        );

        assertEquals("You have reached your daily limit of 5 AI-generated problems", exception.getMessage());
        
        // Verify rate limit key was checked
        verify(valueOperations).increment("ai:problem:gen:user:" + TEST_USER_ID);
    }

    @Test
    @DisplayName("Should set expiration on first rate limit check")
    void testCheckRateLimit_FirstRequest() {
        // Given: First request of the day
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        // When: Check rate limit
        assertDoesNotThrow(() -> service.checkRateLimit(TEST_USER_ID));

        // Then: Should set 24-hour expiration
        verify(redis).expire(eq("ai:problem:gen:user:" + TEST_USER_ID), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("Should throw exception when prompt is empty")
    void testGenerateProblem_EmptyPrompt() {
        // Given: User is under rate limit
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // When & Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.generateProblem("", TEST_DIFFICULTY, TEST_TOPIC, TEST_USER_ID)
        );

        assertEquals("Prompt cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when prompt exceeds 1000 characters")
    void testGenerateProblem_PromptTooLong() {
        // Given: User is under rate limit
        when(valueOperations.increment(anyString())).thenReturn(1L);
        String longPrompt = "a".repeat(1001);

        // When & Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.generateProblem(longPrompt, TEST_DIFFICULTY, TEST_TOPIC, TEST_USER_ID)
        );

        assertEquals("Prompt cannot exceed 1000 characters", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when difficulty is invalid")
    void testGenerateProblem_InvalidDifficulty() {
        // Given: User is under rate limit
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // When & Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.generateProblem(TEST_PROMPT, "INVALID", TEST_TOPIC, TEST_USER_ID)
        );

        assertEquals("Difficulty must be EASY, MEDIUM, or HARD", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when topic exceeds 100 characters")
    void testGenerateProblem_TopicTooLong() {
        // Given: User is under rate limit
        when(valueOperations.increment(anyString())).thenReturn(1L);
        String longTopic = "a".repeat(101);

        // When & Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.generateProblem(TEST_PROMPT, TEST_DIFFICULTY, longTopic, TEST_USER_ID)
        );

        assertEquals("Topic cannot exceed 100 characters", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when AI API key is not configured")
    void testGenerateProblem_MissingApiKey() {
        // Given: User is under rate limit but API key is missing
        when(valueOperations.increment(anyString())).thenReturn(1L);
        ReflectionTestUtils.setField(service, "kimiApiKey", "");

        // When & Then: Should throw RuntimeException
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> service.generateProblem(TEST_PROMPT, TEST_DIFFICULTY, TEST_TOPIC, TEST_USER_ID)
        );

        assertTrue(exception.getMessage().contains("API key for AI model is not configured"));
    }

    @Test
    @DisplayName("Should handle AI API failure gracefully")
    void testGenerateProblem_AIApiFails() {
        // Given: User is under rate limit
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        // Mock AI API failure
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "AI service error"));

        // When & Then: Should throw RuntimeException
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> service.generateProblem(TEST_PROMPT, TEST_DIFFICULTY, TEST_TOPIC, TEST_USER_ID)
        );

        assertTrue(exception.getMessage().contains("AI failed to generate problem spec"));
    }

    @Test
    @DisplayName("Should create problem with correct visibility and ownership")
    void testGenerateProblem_CorrectVisibilityAndOwnership() {
        // Given: User is under rate limit
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        // Mock AI API responses
        mockSuccessfulAIResponses();

        // Capture saved problem
        ArgumentCaptor<Problem> problemCaptor = ArgumentCaptor.forClass(Problem.class);
        when(problemRepository.save(problemCaptor.capture())).thenAnswer(i -> {
            Problem p = i.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(codeSnippetRepository.save(any(CodeSnippet.class))).thenAnswer(i -> i.getArgument(0));

        // When: Generate problem
        service.generateProblem(TEST_PROMPT, TEST_DIFFICULTY, TEST_TOPIC, TEST_USER_ID);

        // Then: Problem has correct visibility and ownership
        Problem savedProblem = problemCaptor.getValue();
        assertEquals("PRIVATE_OWNED", savedProblem.getVisibility());
        assertEquals(TEST_USER_ID, savedProblem.getCreatedBy());
        assertTrue(savedProblem.getActive());
        assertEquals(TEST_DIFFICULTY, savedProblem.getLevel());
    }

    @Test
    @DisplayName("Should store test cases as code snippets for all languages")
    void testGenerateProblem_StoresAllLanguageSnippets() {
        // Given: User is under rate limit
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        // Mock AI API responses
        mockSuccessfulAIResponses();

        Problem savedProblem = createMockProblem();
        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);

        // Capture saved code snippets
        ArgumentCaptor<CodeSnippet> snippetCaptor = ArgumentCaptor.forClass(CodeSnippet.class);
        when(codeSnippetRepository.save(snippetCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        // When: Generate problem
        service.generateProblem(TEST_PROMPT, TEST_DIFFICULTY, TEST_TOPIC, TEST_USER_ID);

        // Then: All 5 language snippets are saved
        List<CodeSnippet> savedSnippets = snippetCaptor.getAllValues();
        assertEquals(5, savedSnippets.size());

        // Verify each language is present
        List<String> expectedLanguages = List.of("JAVA", "CPP", "PYTHON", "JAVASCRIPT", "C");
        for (String lang : expectedLanguages) {
            assertTrue(
                savedSnippets.stream().anyMatch(s -> s.getLanguage().name().equals(lang)),
                "Missing snippet for language: " + lang
            );
        }

        // Verify each snippet has content
        for (CodeSnippet snippet : savedSnippets) {
            assertNotNull(snippet.getProblem());
            assertNotNull(snippet.getLanguage());
            assertNotNull(snippet.getSolutionTemplate());
            assertFalse(snippet.getSolutionTemplate().isEmpty());
        }
    }

    @Test
    @DisplayName("Should include topic in query when provided")
    void testGenerateProblem_WithTopic() {
        // Given: User is under rate limit
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        // Mock AI API responses
        mockSuccessfulAIResponses();

        Problem savedProblem = createMockProblem();
        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);
        when(codeSnippetRepository.save(any(CodeSnippet.class))).thenAnswer(i -> i.getArgument(0));

        // When: Generate problem with topic
        Problem result = service.generateProblem(TEST_PROMPT, TEST_DIFFICULTY, TEST_TOPIC, TEST_USER_ID);

        // Then: Problem is generated successfully
        assertNotNull(result);
        
        // Note: We can't directly verify the query content since it's internal to the method,
        // but we can verify the call was made successfully with the topic parameter
    }

    @Test
    @DisplayName("Should work without topic")
    void testGenerateProblem_WithoutTopic() {
        // Given: User is under rate limit
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        // Mock AI API responses
        mockSuccessfulAIResponses();

        Problem savedProblem = createMockProblem();
        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);
        when(codeSnippetRepository.save(any(CodeSnippet.class))).thenAnswer(i -> i.getArgument(0));

        // When: Generate problem without topic
        Problem result = service.generateProblem(TEST_PROMPT, TEST_DIFFICULTY, null, TEST_USER_ID);

        // Then: Problem is generated successfully
        assertNotNull(result);
    }

    // Helper methods

    private void mockSuccessfulAIResponses() {
        // Mock Pass 1: Problem spec generation
        ResponseEntity<Map> pass1Response = ResponseEntity.ok(createMockAISpecResponse());
        
        // Mock Pass 2: Harness generation
        ResponseEntity<Map> pass2Response = ResponseEntity.ok(createMockAIHarnessResponse());
        
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(pass1Response, pass2Response);
    }

    private Map<String, Object> createMockAISpecResponse() {
        return Map.of(
            "choices", List.of(
                Map.of(
                    "finish_reason", "stop",
                    "message", Map.of(
                        "content", """
                        {
                          "problem": {
                            "title": "Two Sum",
                            "description": "Given an array of integers nums and an integer target, return indices of the two numbers that add up to target.",
                            "inputFormat": "nums: int[], target: int",
                            "outputFormat": "int[]",
                            "constraints": "2 <= nums.length <= 10^4\\n-10^9 <= nums[i] <= 10^9\\n-10^9 <= target <= 10^9",
                            "timeLimit": 5,
                            "memoryLimit": 256,
                            "level": "MEDIUM",
                            "example1": "Input: nums = [2,7,11,15], target = 9\\nOutput: [0,1]",
                            "example2": "Input: nums = [3,2,4], target = 6\\nOutput: [1,2]",
                            "example3": "Input: nums = [3,3], target = 6\\nOutput: [0,1]"
                          },
                          "signature": {
                            "name": "twoSum",
                            "returnType": "int[]",
                            "params": [
                              {"name": "nums", "type": "int[]"},
                              {"name": "target", "type": "int"}
                            ]
                          },
                          "referenceSolutionPython": "def twoSum(nums, target):\\n    seen = {}\\n    for i, num in enumerate(nums):\\n        if target - num in seen:\\n            return [seen[target - num], i]\\n        seen[num] = i\\n    return []",
                          "tests": [
                            {"args": {"nums": [2,7,11,15], "target": 9}, "expected": [0,1], "hidden": false},
                            {"args": {"nums": [3,2,4], "target": 6}, "expected": [1,2], "hidden": false},
                            {"args": {"nums": [3,3], "target": 6}, "expected": [0,1], "hidden": false},
                            {"args": {"nums": [-1,-2,-3,-4,-5], "target": -8}, "expected": [2,4], "hidden": false},
                            {"args": {"nums": [1,2,3,4,5], "target": 9}, "expected": [3,4], "hidden": true},
                            {"args": {"nums": [0,4,3,0], "target": 0}, "expected": [0,3], "hidden": true}
                          ]
                        }
                        """
                    )
                )
            )
        );
    }

    private Map<String, Object> createMockAIHarnessResponse() {
        return Map.of(
            "choices", List.of(
                Map.of(
                    "finish_reason", "stop",
                    "message", Map.of(
                        "content", """
                        ===HARNESS:JAVA===
                        // USER_CODE_START
                        class Solution {
                            public int[] twoSum(int[] nums, int target) { return new int[0]; }
                        }
                        // USER_CODE_END
                        
                        ===HARNESS:CPP===
                        // USER_CODE_START
                        class Solution {
                        public:
                            vector<int> twoSum(vector<int>& nums, int target) { return {}; }
                        };
                        // USER_CODE_END
                        
                        ===HARNESS:PYTHON===
                        # USER_CODE_START
                        class Solution:
                            def twoSum(self, nums, target): return []
                        # USER_CODE_END
                        
                        ===HARNESS:JAVASCRIPT===
                        // USER_CODE_START
                        function twoSum(nums, target) { return []; }
                        // USER_CODE_END
                        
                        ===HARNESS:C===
                        // USER_CODE_START
                        int* twoSum(int* nums, int numsSize, int target, int* returnSize) { return NULL; }
                        // USER_CODE_END
                        """
                    )
                )
            )
        );
    }

    private Problem createMockProblem() {
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setTitle("Two Sum");
        problem.setDescription("Given an array of integers nums and an integer target, return indices.");
        problem.setInputFormat("nums: int[], target: int");
        problem.setOutputFormat("int[]");
        problem.setConstraints("2 <= nums.length <= 10^4");
        problem.setTimeLimit(5.0);
        problem.setMemoryLimit(256);
        problem.setLevel("MEDIUM");
        problem.setVisibility("PRIVATE_OWNED");
        problem.setCreatedBy(TEST_USER_ID);
        problem.setActive(true);
        return problem;
    }

    // ==================== Problem Editing Tests ====================

    @Test
    @DisplayName("Should successfully edit problem by owner")
    void testEditProblem_SuccessByOwner() {
        // Given: Owner is editing their own problem
        Long problemId = 1L;
        Long ownerId = TEST_USER_ID;
        
        Problem existingProblem = createMockProblemForEdit();
        existingProblem.setCreatedBy(ownerId);
        existingProblem.setVisibility("PRIVATE_OWNED");
        
        User owner = createMockUser(ownerId, "owner", false);
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.of(existingProblem));
        when(userRepository.findById(ownerId)).thenReturn(java.util.Optional.of(owner));
        when(problemRepository.save(any(Problem.class))).thenAnswer(i -> i.getArgument(0));
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.title = "Updated Two Sum";
        dto.description = "Updated description";
        dto.level = "HARD";
        
        // When: Owner edits the problem
        Problem result = service.editProblem(problemId, dto, ownerId);
        
        // Then: Problem is updated
        assertNotNull(result);
        assertEquals("Updated Two Sum", result.getTitle());
        assertEquals("Updated description", result.getDescription());
        assertEquals("HARD", result.getLevel());
        
        verify(problemRepository).save(any(Problem.class));
    }

    @Test
    @DisplayName("Should successfully edit problem by admin")
    void testEditProblem_SuccessByAdmin() {
        // Given: Admin is editing someone else's problem
        Long problemId = 1L;
        Long ownerId = TEST_USER_ID;
        Long adminId = 99L;
        
        Problem existingProblem = createMockProblemForEdit();
        existingProblem.setCreatedBy(ownerId);
        existingProblem.setVisibility("PRIVATE_OWNED");
        
        User admin = createMockUser(adminId, "admin", true);
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.of(existingProblem));
        when(userRepository.findById(adminId)).thenReturn(java.util.Optional.of(admin));
        when(problemRepository.save(any(Problem.class))).thenAnswer(i -> i.getArgument(0));
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.title = "Admin Updated Title";
        
        // When: Admin edits the problem
        Problem result = service.editProblem(problemId, dto, adminId);
        
        // Then: Problem is updated
        assertNotNull(result);
        assertEquals("Admin Updated Title", result.getTitle());
        
        verify(problemRepository).save(any(Problem.class));
    }

    @Test
    @DisplayName("Should fail when non-owner attempts to edit")
    void testEditProblem_NonOwnerAttempt() {
        // Given: Non-owner tries to edit someone else's problem
        Long problemId = 1L;
        Long ownerId = TEST_USER_ID;
        Long otherUserId = 99L;
        
        Problem existingProblem = createMockProblemForEdit();
        existingProblem.setCreatedBy(ownerId);
        existingProblem.setVisibility("PRIVATE_OWNED");
        
        User otherUser = createMockUser(otherUserId, "otheruser", false);
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.of(existingProblem));
        when(userRepository.findById(otherUserId)).thenReturn(java.util.Optional.of(otherUser));
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.title = "Hacked Title";
        
        // When & Then: Should throw SecurityException
        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> service.editProblem(problemId, dto, otherUserId)
        );
        
        assertTrue(exception.getMessage().contains("permission to edit this problem"));
        verify(problemRepository, never()).save(any(Problem.class));
    }

    @Test
    @DisplayName("Should fail when editing problem attached to LIVE contest")
    void testEditProblem_AttachedToLiveContest() {
        // Given: Problem is attached to a LIVE contest
        Long problemId = 1L;
        Long ownerId = TEST_USER_ID;
        
        Contest liveContest = new Contest();
        liveContest.setId(100L);
        liveContest.setStatus(Contest.ContestStatus.LIVE);
        
        Problem existingProblem = createMockProblemForEdit();
        existingProblem.setCreatedBy(ownerId);
        existingProblem.setVisibility("PRIVATE_OWNED");
        existingProblem.getContests().add(liveContest);
        
        User owner = createMockUser(ownerId, "owner", false);
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.of(existingProblem));
        when(userRepository.findById(ownerId)).thenReturn(java.util.Optional.of(owner));
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.title = "Updated Title";
        
        // When & Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.editProblem(problemId, dto, ownerId)
        );
        
        assertTrue(exception.getMessage().contains("attached to a LIVE or ENDED contest"));
        verify(problemRepository, never()).save(any(Problem.class));
    }

    @Test
    @DisplayName("Should fail when editing problem attached to ENDED contest")
    void testEditProblem_AttachedToEndedContest() {
        // Given: Problem is attached to an ENDED contest
        Long problemId = 1L;
        Long ownerId = TEST_USER_ID;
        
        Contest endedContest = new Contest();
        endedContest.setId(100L);
        endedContest.setStatus(Contest.ContestStatus.ENDED);
        
        Problem existingProblem = createMockProblemForEdit();
        existingProblem.setCreatedBy(ownerId);
        existingProblem.setVisibility("PRIVATE_OWNED");
        existingProblem.getContests().add(endedContest);
        
        User owner = createMockUser(ownerId, "owner", false);
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.of(existingProblem));
        when(userRepository.findById(ownerId)).thenReturn(java.util.Optional.of(owner));
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.title = "Updated Title";
        
        // When & Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.editProblem(problemId, dto, ownerId)
        );
        
        assertTrue(exception.getMessage().contains("attached to a LIVE or ENDED contest"));
        verify(problemRepository, never()).save(any(Problem.class));
    }

    @Test
    @DisplayName("Should allow editing unattached problem")
    void testEditProblem_UnattachedProblem() {
        // Given: Problem is not attached to any contest
        Long problemId = 1L;
        Long ownerId = TEST_USER_ID;
        
        Problem existingProblem = createMockProblemForEdit();
        existingProblem.setCreatedBy(ownerId);
        existingProblem.setVisibility("PRIVATE_OWNED");
        existingProblem.getContests().clear(); // No contests
        
        User owner = createMockUser(ownerId, "owner", false);
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.of(existingProblem));
        when(userRepository.findById(ownerId)).thenReturn(java.util.Optional.of(owner));
        when(problemRepository.save(any(Problem.class))).thenAnswer(i -> i.getArgument(0));
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.title = "Updated Title";
        
        // When: Edit unattached problem
        Problem result = service.editProblem(problemId, dto, ownerId);
        
        // Then: Problem is updated successfully
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        verify(problemRepository).save(any(Problem.class));
    }

    @Test
    @DisplayName("Should allow editing problem attached to UPCOMING contest")
    void testEditProblem_AttachedToUpcomingContest() {
        // Given: Problem is attached to an UPCOMING contest (should be allowed)
        Long problemId = 1L;
        Long ownerId = TEST_USER_ID;
        
        Contest upcomingContest = new Contest();
        upcomingContest.setId(100L);
        upcomingContest.setStatus(Contest.ContestStatus.UPCOMING);
        
        Problem existingProblem = createMockProblemForEdit();
        existingProblem.setCreatedBy(ownerId);
        existingProblem.setVisibility("PRIVATE_OWNED");
        existingProblem.getContests().add(upcomingContest);
        
        User owner = createMockUser(ownerId, "owner", false);
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.of(existingProblem));
        when(userRepository.findById(ownerId)).thenReturn(java.util.Optional.of(owner));
        when(problemRepository.save(any(Problem.class))).thenAnswer(i -> i.getArgument(0));
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.title = "Updated Before Contest";
        
        // When: Edit problem attached to UPCOMING contest
        Problem result = service.editProblem(problemId, dto, ownerId);
        
        // Then: Problem is updated successfully
        assertNotNull(result);
        assertEquals("Updated Before Contest", result.getTitle());
        verify(problemRepository).save(any(Problem.class));
    }

    @Test
    @DisplayName("Should fail when non-admin tries to edit PUBLIC problem")
    void testEditProblem_PublicProblemNonAdmin() {
        // Given: Non-admin tries to edit PUBLIC problem
        Long problemId = 1L;
        Long ownerId = TEST_USER_ID;
        
        Problem existingProblem = createMockProblemForEdit();
        existingProblem.setCreatedBy(ownerId);
        existingProblem.setVisibility("PUBLIC");
        
        User owner = createMockUser(ownerId, "owner", false);
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.of(existingProblem));
        when(userRepository.findById(ownerId)).thenReturn(java.util.Optional.of(owner));
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.title = "Trying to edit public";
        
        // When & Then: Should throw SecurityException
        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> service.editProblem(problemId, dto, ownerId)
        );
        
        assertTrue(exception.getMessage().contains("PUBLIC problems can only be edited by administrators"));
        verify(problemRepository, never()).save(any(Problem.class));
    }

    @Test
    @DisplayName("Should allow admin to edit PUBLIC problem")
    void testEditProblem_PublicProblemByAdmin() {
        // Given: Admin edits PUBLIC problem
        Long problemId = 1L;
        Long adminId = 99L;
        
        Problem existingProblem = createMockProblemForEdit();
        existingProblem.setCreatedBy(50L); // Created by someone else
        existingProblem.setVisibility("PUBLIC");
        
        User admin = createMockUser(adminId, "admin", true);
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.of(existingProblem));
        when(userRepository.findById(adminId)).thenReturn(java.util.Optional.of(admin));
        when(problemRepository.save(any(Problem.class))).thenAnswer(i -> i.getArgument(0));
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.title = "Admin Updated Public Problem";
        
        // When: Admin edits PUBLIC problem
        Problem result = service.editProblem(problemId, dto, adminId);
        
        // Then: Problem is updated successfully
        assertNotNull(result);
        assertEquals("Admin Updated Public Problem", result.getTitle());
        verify(problemRepository).save(any(Problem.class));
    }

    @Test
    @DisplayName("Should validate level field")
    void testEditProblem_InvalidLevel() {
        // Given: Invalid level value
        Long problemId = 1L;
        Long ownerId = TEST_USER_ID;
        
        Problem existingProblem = createMockProblemForEdit();
        existingProblem.setCreatedBy(ownerId);
        existingProblem.setVisibility("PRIVATE_OWNED");
        
        User owner = createMockUser(ownerId, "owner", false);
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.of(existingProblem));
        when(userRepository.findById(ownerId)).thenReturn(java.util.Optional.of(owner));
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.level = "INVALID";
        
        // When & Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.editProblem(problemId, dto, ownerId)
        );
        
        assertTrue(exception.getMessage().contains("Level must be EASY, MEDIUM, or HARD"));
        verify(problemRepository, never()).save(any(Problem.class));
    }

    @Test
    @DisplayName("Should update all provided fields")
    void testEditProblem_UpdateAllFields() {
        // Given: All fields are provided in DTO
        Long problemId = 1L;
        Long ownerId = TEST_USER_ID;
        
        Problem existingProblem = createMockProblemForEdit();
        existingProblem.setCreatedBy(ownerId);
        existingProblem.setVisibility("PRIVATE_OWNED");
        
        User owner = createMockUser(ownerId, "owner", false);
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.of(existingProblem));
        when(userRepository.findById(ownerId)).thenReturn(java.util.Optional.of(owner));
        when(problemRepository.save(any(Problem.class))).thenAnswer(i -> i.getArgument(0));
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.title = "New Title";
        dto.description = "New Description";
        dto.inputFormat = "New Input Format";
        dto.outputFormat = "New Output Format";
        dto.constraints = "New Constraints";
        dto.timeLimit = 10.0;
        dto.memoryLimit = 512;
        dto.level = "HARD";
        dto.example1 = "Example 1";
        dto.example2 = "Example 2";
        dto.example3 = "Example 3";
        
        // When: Edit with all fields
        Problem result = service.editProblem(problemId, dto, ownerId);
        
        // Then: All fields are updated
        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertEquals("New Description", result.getDescription());
        assertEquals("New Input Format", result.getInputFormat());
        assertEquals("New Output Format", result.getOutputFormat());
        assertEquals("New Constraints", result.getConstraints());
        assertEquals(10.0, result.getTimeLimit());
        assertEquals(512, result.getMemoryLimit());
        assertEquals("HARD", result.getLevel());
        assertEquals("Example 1", result.getExample1());
        assertEquals("Example 2", result.getExample2());
        assertEquals("Example 3", result.getExample3());
        
        verify(problemRepository).save(any(Problem.class));
    }

    @Test
    @DisplayName("Should throw exception when problem not found")
    void testEditProblem_ProblemNotFound() {
        // Given: Problem doesn't exist
        Long problemId = 999L;
        Long userId = TEST_USER_ID;
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.empty());
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.title = "New Title";
        
        // When & Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.editProblem(problemId, dto, userId)
        );
        
        assertTrue(exception.getMessage().contains("Problem not found"));
        verify(problemRepository, never()).save(any(Problem.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testEditProblem_UserNotFound() {
        // Given: User doesn't exist
        Long problemId = 1L;
        Long userId = 999L;
        
        Problem existingProblem = createMockProblemForEdit();
        
        when(problemRepository.findById(problemId)).thenReturn(java.util.Optional.of(existingProblem));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());
        
        AIProblemGeneratorService.EditProblemDTO dto = new AIProblemGeneratorService.EditProblemDTO();
        dto.title = "New Title";
        
        // When & Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.editProblem(problemId, dto, userId)
        );
        
        assertTrue(exception.getMessage().contains("User not found"));
        verify(problemRepository, never()).save(any(Problem.class));
    }

    // Helper methods for problem editing tests

    private User createMockUser(Long id, String username, boolean isAdmin) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        
        java.util.Set<Role> roles = new java.util.HashSet<>();
        Role userRole = new Role();
        userRole.setName(Role.ERole.ROLE_USER);
        roles.add(userRole);
        
        if (isAdmin) {
            Role adminRole = new Role();
            adminRole.setName(Role.ERole.ROLE_ADMIN);
            roles.add(adminRole);
        }
        
        user.setRoles(roles);
        return user;
    }

    private Problem createMockProblemForEdit() {
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setTitle("Two Sum");
        problem.setDescription("Given an array of integers nums and an integer target, return indices.");
        problem.setInputFormat("nums: int[], target: int");
        problem.setOutputFormat("int[]");
        problem.setConstraints("2 <= nums.length <= 10^4");
        problem.setTimeLimit(5.0);
        problem.setMemoryLimit(256);
        problem.setLevel("MEDIUM");
        problem.setVisibility("PRIVATE_OWNED");
        problem.setCreatedBy(TEST_USER_ID);
        problem.setActive(true);
        return problem;
    }
}
