package com.example.codecombat2026.dto;

import java.time.LocalDateTime;

public class ContestStatusDTO {
    private Boolean active;
    private Boolean exists;
    private String contestName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public ContestStatusDTO() {
    }

    public ContestStatusDTO(Boolean active, Boolean exists, String contestName, LocalDateTime startTime,
            LocalDateTime endTime) {
        this.active = active;
        this.exists = exists;
        this.contestName = contestName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getExists() {
        return exists;
    }

    public void setExists(Boolean exists) {
        this.exists = exists;
    }

    public String getContestName() {
        return contestName;
    }

    public void setContestName(String contestName) {
        this.contestName = contestName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
