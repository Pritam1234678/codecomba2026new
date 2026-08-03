package com.example.codecombat2026.sqljudge.service;

import com.example.codecombat2026.sqljudge.entity.SqlProblem;
import com.example.codecombat2026.sqljudge.entity.SqlSubmission;
import com.example.codecombat2026.sqljudge.repository.SqlProblemRepository;
import com.example.codecombat2026.sqljudge.repository.SqlSubmissionRepository;
import com.example.codecombat2026.sqljudge.worker.SqlJudgeWorkerPool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlSubmissionServiceTest {

    @Mock private SqlSubmissionRepository submissionRepository;
    @Mock private SqlProblemRepository problemRepository;
    @Mock private StringRedisTemplate redis;
    @Mock private ListOperations<String, String> listOps;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private SqlSubmissionService service;

    private static SqlProblem enabledProblem() {
        SqlProblem p = new SqlProblem();
        p.setId(1L);
        p.setEnabled(true);
        p.setSchemaName("q_1");
        return p;
    }

    @Test
    void missingProblemThrows() {
        when(problemRepository.findById(1L)).thenReturn(java.util.Optional.empty());
        assertThrows(IllegalArgumentException.class,
            () -> service.submit(7L, 1L, "SELECT 1", false));
    }

    @Test
    void disabledProblemThrows() {
        SqlProblem p = new SqlProblem();
        p.setId(1L);
        p.setEnabled(false);
        when(problemRepository.findById(1L)).thenReturn(java.util.Optional.of(p));
        assertThrows(IllegalArgumentException.class,
            () -> service.submit(7L, 1L, "SELECT 1", false));
    }

    @Test
    void redisFailureMarksInternalError() throws Exception {
        when(problemRepository.findById(1L)).thenReturn(java.util.Optional.of(enabledProblem()));
        SqlSubmission saved = new SqlSubmission();
        saved.setId(99L);
        saved.setStatus(SqlSubmission.Status.QUEUED);
        when(submissionRepository.save(any(SqlSubmission.class))).thenReturn(saved);
        when(redis.opsForList()).thenReturn(listOps);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"submissionId\":99}");
        doThrow(new RuntimeException("connection refused"))
            .when(listOps).leftPush(anyString(), anyString());

        SqlSubmission result = service.submit(7L, 1L, "SELECT 1", false);

        assertEquals(SqlSubmission.Status.INTERNAL_ERROR, result.getStatus());
        verify(submissionRepository, org.mockito.Mockito.times(2)).save(any(SqlSubmission.class));
    }

    @Test
    void successPushesJobToQueue() throws Exception {
        when(problemRepository.findById(1L)).thenReturn(java.util.Optional.of(enabledProblem()));
        SqlSubmission saved = new SqlSubmission();
        saved.setId(99L);
        saved.setStatus(SqlSubmission.Status.QUEUED);
        when(submissionRepository.save(any(SqlSubmission.class))).thenReturn(saved);
        when(redis.opsForList()).thenReturn(listOps);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"submissionId\":99}");

        SqlSubmission result = service.submit(7L, 1L, "SELECT 1", false);

        assertEquals(SqlSubmission.Status.QUEUED, result.getStatus());
        verify(listOps).leftPush(eq(SqlJudgeWorkerPool.QUEUE_KEY), anyString());
    }
}
