package com.example.codecombat2026.dto;

import java.time.LocalDateTime;

public class ContestStatusDTO {
    private Boolean active;       // contest active
    private Boolean exists;       // contest exists
    private String contestName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean problemActive; // problem active (false = disabled by admin)

    public ContestStatusDTO() {
    }

    public ContestStatusDTO(Boolean active, Boolean exists, String contestName, LocalDateTime startTime,
            LocalDateTime endTime) {
        this.active = active;
        this.exists = exists;
        this.contestName = contestName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.problemActive = true; // default
    }

    public ContestStatusDTO(Boolean active, Boolean exists, String contestName, LocalDateTime startTime,
            LocalDateTime endTime, Boolean problemActive) {
        this.active = active;
        this.exists = exists;
        this.contestName = contestName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.problemActive = problemActive;
    }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getExists() { return exists; }
    public void setExists(Boolean exists) { this.exists = exists; }
    public String getContestName() { return contestName; }
    public void setContestName(String contestName) { this.contestName = contestName; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Boolean getProblemActive() { return problemActive; }
    public void setProblemActive(Boolean problemActive) { this.problemActive = problemActive; }
}
