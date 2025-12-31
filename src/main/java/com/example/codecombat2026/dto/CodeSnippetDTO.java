package com.example.codecombat2026.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeSnippetDTO {
    private Long id;
    private Long problemId;
    private String language;
    private String starterCode;
    private String solutionTemplate;
}
