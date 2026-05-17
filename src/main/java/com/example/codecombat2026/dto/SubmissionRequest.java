package com.example.codecombat2026.dto;

import com.example.codecombat2026.entity.Submission;
import lombok.Data;

@Data
public class SubmissionRequest {
    private Long problemId;
    private String code;
    private Submission.ProgrammingLanguage language;
}
