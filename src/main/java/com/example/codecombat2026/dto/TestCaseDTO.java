package com.example.codecombat2026.dto;

public class TestCaseDTO {
    private Long id;
    private String input;
    private String expectedOutput;
    private boolean isHidden;
    private Long problemId;

    public TestCaseDTO() {
    }

    public TestCaseDTO(Long id, String input, String expectedOutput, boolean isHidden, Long problemId) {
        this.id = id;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.isHidden = isHidden;
        this.problemId = problemId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }
}
